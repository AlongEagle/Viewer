/*
 * Copyright (C) 2015 iChano incorporation's Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhongyun.viewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.zxing.client.android.Intents;
import com.ichano.rvs.viewer.Viewer;
import com.ichano.rvs.viewer.bean.StreamerInfo;
import com.ichano.rvs.viewer.callback.RecvJpegListener;
import com.ichano.rvs.viewer.constant.RvsJpegType;
import com.ichano.rvs.viewer.constant.StreamerPresenceState;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.FeedbackAgent;
import com.umeng.update.UmengUpdateAgent;
import com.zhongyun.viewer.db.CameraInfo;
import com.zhongyun.viewer.db.CameraInfoManager;
import com.zhongyun.viewer.utils.AppUtils;
import com.zhongyun.viewer.utils.Constants;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CameraListActivity extends Activity
	implements MyViewerHelper.CameraStateListener, View.OnClickListener,
	PopupMenu.OnMenuItemClickListener, AdapterView.OnItemClickListener{

	private static final String TAG = CameraListActivity.class.getSimpleName();
	private final static int SCANNIN_GREQUEST_CODE = 1;
	private final static long GET_THUMB_PERIOD = 600000;
	private final static String DEFAULT_USER = "admin";

	private static final String AD_UNIT_ID = "ca-app-pub-4895175297664182/1269168158";
	private static final String DISCLAIMER_URL_CN = "file:///android_asset/iChanoPrivacyPolicyCN.html";
	private static final String DISCLAIMER_URL_EN = "file:///android_asset/iChanoPrivacyPolicyEN.html";
	private boolean mShowChinese;
	
	private Viewer mViewer;
	private MyViewerHelper mMyViewerHelper;
	private Bitmap mCameraDefaulThumb;
	private List<CameraInfo> mCameraInfos;
	private CameraInfoManager mCameraInfoManager;
	private CameraListAdapter mCameraListAdapter;
	private ListView mCameraListView;
	private ImageView mMenuView;
	private PopupMenu mMenu;
	private Handler mHandler = new Handler();
	private LayoutInflater mLayoutInflater;
	@SuppressLint("UseSparseArrays")
	private HashMap<Long, Long> mThumbsGetTime = new HashMap<Long, Long>();
	private HashMap<Long, Long> mThumbRequestMap = new HashMap<Long, Long>();
	private Dialog mAboutDialog;
	private Dialog mDisclaimerDialog;
	private Dialog mAddCameraDlg;
	private Dialog mExitDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.camera_list);
		AppUtils.setStatusBarTransparent(this, getResources().getColor(R.color.title_red));
		mLayoutInflater = LayoutInflater.from(this);
		mCameraListView = (ListView) findViewById(R.id.cameraList);
		TextView titleView = (TextView) findViewById(R.id.title);
		titleView.setText(R.string.app_name);
		mMenuView = (ImageView) findViewById(R.id.menu);
		mMenuView.setVisibility(View.VISIBLE);
		mMenuView.setOnClickListener(this);
		mMenu = new PopupMenu(this, mMenuView);
		MenuInflater inflater = mMenu.getMenuInflater();
	    inflater.inflate(R.menu.popup_menu, mMenu.getMenu());
	    mMenu.setOnMenuItemClickListener(this);
		
		mViewer = Viewer.getViewer();
		mMyViewerHelper = MyViewerHelper.getInstance(getApplicationContext());
		mMyViewerHelper.setCameraStateListener(this);
		mCameraDefaulThumb = BitmapFactory.decodeResource(getResources(), R.drawable.avs_type_android);
		
		mCameraInfoManager = new CameraInfoManager(this);
		mCameraInfos = mCameraInfoManager.getAllCameraInfos();
		if(null == mCameraInfos) mCameraInfos = new ArrayList<CameraInfo>();
		for (CameraInfo info : mCameraInfos) {
			addStreamer(info.getCid(), info.getCameraUser(), info.getCameraPwd());
		}
		mCameraListAdapter = new CameraListAdapter(this, mCameraInfos);
		mCameraListView.setAdapter(mCameraListAdapter);
		mCameraListView.setOnItemClickListener(this);

		mShowChinese = "zh".equals(Locale.getDefault().getLanguage().toLowerCase());
		
		//update
		UmengUpdateAgent.setUpdateOnlyWifi(false);
		UmengUpdateAgent.update(this);
		
		//admob ad
		RelativeLayout adContainer = (RelativeLayout) findViewById(R.id.adLayout);
		AdView ad = new AdView(this, AdSize.BANNER, AD_UNIT_ID);
		//.addTestDevice("703C305FC29B7ED91BD7625874CFDEBC")
		ad.loadAd(new AdRequest());
		adContainer.addView(ad);
	}
	
	private CameraInfo getCameraInfo(long cid){
		for (CameraInfo info : mCameraInfos) {
			if(cid == info.getCid()){
				return info;
			}
		}
		return null;
	}
	
	//添加采集端
	public void addStreamer(long streamerCid, String user, String pass){
		boolean ret = mViewer.connectStreamer(streamerCid, user, pass);
		CameraInfo info = getCameraInfo(streamerCid);
		if(ret) {
			if(null == info){
				StreamerInfo  sinfo = mViewer.getStreamerInfoMgr().getStreamerInfo(streamerCid);
				info = new CameraInfo();
				info.setCid(streamerCid);
				String name = sinfo.getDeviceName();
				info.setCameraName((null == name) ? "" : name);
				info.setCameraUser(user);
				info.setCameraPwd(pass);
				info.setCameraThumb(mCameraDefaulThumb);
				info.setIsOnline(false);
				info.setPwdIsRight(true);
				info.setOS(sinfo.getOsVersion());
				mCameraInfoManager.addCameraInfo(info);
				mCameraInfos.add(info);
				mCameraListAdapter.notifyDataSetChanged();
			}
		}else{
			if(null != info){
				info.setPwdIsRight(false);
				mCameraListAdapter.notifyDataSetChanged();
			}
		}
	}
	
	//删除采集端
	public void removeStreamer(long streamerCid){
		mViewer.disconnectStreamer(streamerCid);
	}
	
	@Override
	public void onCameraConnectionChange(long streamerCID, boolean connected) {
		long lastTime = (null == mThumbsGetTime.get(streamerCID)) ? 0 : mThumbsGetTime.get(streamerCID);
		long cur = System.currentTimeMillis();
		
		//do not get thumb so busy.
		if(cur - lastTime > GET_THUMB_PERIOD){
			mThumbsGetTime.put(streamerCID, cur);
			long requestId =  mViewer.getMedia().requestJpeg(streamerCID, 0, 0, RvsJpegType.ICON, new RecvJpegListener() {
				
				@Override
				public void onRecvJpeg(long requestId,byte[] data) {
					if(null == mThumbRequestMap.get(requestId)) return;
					long cid = mThumbRequestMap.get(requestId);
					Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
					CameraInfo info = getCameraInfo(cid);
					if(null != info && null != bmp){
						info.setCameraThumb(bmp);
						mCameraInfoManager.update(info);
						mHandler.post(new Runnable() {
							
							@Override
							public void run() {
								mCameraListAdapter.notifyDataSetChanged();
							}
						});
					}
				}
			});
			mThumbRequestMap.put(requestId, streamerCID);
		}
	}
	
	@Override
	public void onCameraStateChange(long streamerCid, StreamerPresenceState state) {
		CameraInfo info = getCameraInfo(streamerCid);
		if(null != info){
			StreamerInfo  sinfo = mViewer.getStreamerInfoMgr().getStreamerInfo(streamerCid);
			String name = sinfo.getDeviceName();
			if(null != name && (!info.getCameraName().equals(name))){
				info.setCameraName(name);
				mCameraInfoManager.update(info);
			}
			if(StreamerPresenceState.USRNAME_PWD_ERR == state && info.getPwdIsRight()){
				info.setIsOnline(false);
				info.setPwdIsRight(false);
				mCameraListAdapter.notifyDataSetChanged();
			}else {
				boolean online = false;
				if(StreamerPresenceState.ONLINE == state){
					online = true;
				}
				if(info.getIsOnline() != online){
					info.setIsOnline(online);
					mCameraListAdapter.notifyDataSetChanged();
				}
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch(id){
		case R.id.menu:
			mMenu.show();
			break;
			default:
				break;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		CameraInfo cameraInfo = mCameraInfos.get(position);
		if(cameraInfo.getIsOnline()){
			Intent intent = new Intent(this, WatchActivity.class);
			intent.putExtra(Constants.INTENT_CID, cameraInfo.getCid());
			intent.putExtra(Constants.INTENT_CAMERA_NAME, cameraInfo.getCameraName());
			startActivity(intent);
		}else{
			Toast.makeText(this, R.string.camera_offline, Toast.LENGTH_LONG).show();
		}
	}
	
	public void showAddCameraDlg(){
		if(null != mAddCameraDlg){
			mAddCameraDlg.show();
		}else{
			View view = mLayoutInflater.inflate(R.layout.add_camera_dialog, null);
			final EditText cidView = (EditText) view.findViewById(R.id.cid);
			final EditText passwordView = (EditText) view.findViewById(R.id.password);
			mAddCameraDlg = new AlertDialog.Builder(this)
			.setView(view)
			.setTitle(R.string.add_camera_dlg_title)
			.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String cid = cidView.getText().toString();
					String pwd = passwordView.getText().toString();
					if(null == cid || null == pwd){
						Toast.makeText(CameraListActivity.this, R.string.empty_info, Toast.LENGTH_LONG).show();
						return;
					}
					if("".equals(cid) || "".equals(pwd)){
						Toast.makeText(CameraListActivity.this, R.string.empty_info, Toast.LENGTH_LONG).show();
						return;
					}
					addStreamer(Long.parseLong(cid), DEFAULT_USER, pwd);
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create();
			mAddCameraDlg.show();
		}
	}
	public void showAboutDialog(){
		if(null != mAboutDialog){
			mAboutDialog.show();
		}else{
			View view = mLayoutInflater.inflate(R.layout.about_dialog, null);
			TextView aboutView = (TextView) view.findViewById(R.id.about);
			aboutView.setText(String.format(getString(R.string.about_str), 
					getString(R.string.app_name), AppUtils.getAppVersionName(CameraListActivity.this)));
			mAboutDialog = new AlertDialog.Builder(this)
			.setView(view)
			.setTitle(R.string.about)
			.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create();
			mAboutDialog.show();
		}
	}
	
	private void showDisclaimerDlg(){
		if(null != mDisclaimerDialog){
			mDisclaimerDialog.show();
		}else{
			WebView webView = new WebView(CameraListActivity.this);
			webView.loadUrl(mShowChinese ? DISCLAIMER_URL_CN : DISCLAIMER_URL_EN);
			mDisclaimerDialog = new AlertDialog.Builder(this)
			.setView(webView)
			.setTitle(R.string.disclaimer)
			.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create();
			mDisclaimerDialog.show();
		}
	}
	
	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_cid:
			showAddCameraDlg();
			break;
		case R.id.add_cid_by_qr:
			Intent intent = new Intent();
			intent.setClass(this, CaptureActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
			break;
		case R.id.help:
			Intent guideIntent = new Intent();
			guideIntent.setClass(getApplicationContext(), GuideActivity.class);
			guideIntent.putExtra(GuideActivity.START_AVS_ACTIVITY, false);
			startActivity(guideIntent);
			break;
		case R.id.feedback:
			FeedbackAgent agent = new FeedbackAgent(this);
			agent.startFeedbackActivity();
			break;
		case R.id.about:
			showAboutDialog();
			break;
		case R.id.disclaimer:
			showDisclaimerDlg();
			break;
		default:
			break;
		}
		return false;
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
		case SCANNIN_GREQUEST_CODE:
			if(resultCode == RESULT_OK){
				Bundle bundle = data.getExtras();
				String barcode = bundle.getString(Intents.Scan.RESULT);
				String[] results = barcode.split("&");
				String cid = results[0].replace("cid=", "");
				String userName = results[1].replace("username=", "");
				String password = results[2].replace("password=", "");
				Log.i(TAG,"cid = " + cid + ", userName = " + userName + ", password = " + password);
				addStreamer(Long.parseLong(cid), userName, password);
			}
			break;
		}
    }
	
	private void showExitDlg(){
		if(null != mExitDialog){
			mExitDialog.show();
		}else{
			mExitDialog = new AlertDialog.Builder(CameraListActivity.this)
			.setTitle(R.string.exit_str)
			.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					CameraListActivity.this.finish();
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.create();
			mExitDialog.show();
		}
	}
	
	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		showExitDlg();
	}
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
	
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		for (CameraInfo info : mCameraInfos) {
			removeStreamer(info.getCid());
		}
		mMyViewerHelper.setCameraStateListener(null);
		mMyViewerHelper.logout();
		android.os.Process.killProcess(android.os.Process.myPid());
	}
	
	public class CameraListAdapter extends BaseAdapter{

		private LayoutInflater mLayoutInflater;
		private List<CameraInfo> mCameraInfos;
		
		public CameraListAdapter(Context context, List<CameraInfo> infos){
			mLayoutInflater = LayoutInflater.from(context);
			mCameraInfos = infos;
		}
		
		@Override
		public int getCount() {
			return mCameraInfos.size();
		}

		@Override
		public Object getItem(int position) {
			return mCameraInfos.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = null;
			if(null == convertView){
				view = mLayoutInflater.inflate(R.layout.camera_list_item, null);
			}else{
				view = convertView;
			}
			
			ImageView thumbView = (ImageView) view.findViewById(R.id.thumb);
			TextView cameraName = (TextView) view.findViewById(R.id.cameraName);
			ImageView cameraStateView = (ImageView) view.findViewById(R.id.cameraState);
			TextView cameraStateTxtView = (TextView) view.findViewById(R.id.cameraStateTxt);
			ImageView editView = (ImageView) view.findViewById(R.id.edit);
			ImageView deleteView = (ImageView) view.findViewById(R.id.delete);
			final CameraInfo info = mCameraInfos.get(position);
			thumbView.setImageBitmap(info.getCameraThumb());
			cameraName.setText(info.getCameraName());
			cameraStateView.setImageResource(getStateDrawable(info));
			cameraStateTxtView.setText(getStateTxtDrawable(info));
			editView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
				}
			});
			deleteView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					new AlertDialog.Builder(CameraListActivity.this)
					.setTitle(R.string.delete_camera_dlg_title)
					.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							removeStreamer(info.getCid());
							mCameraInfoManager.delete(info);
							mCameraInfos.remove(info);
							mCameraListAdapter.notifyDataSetChanged();
						}
					})
					.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.create().show();
				}
			});
			return view;
		}
		
		private int getStateDrawable(CameraInfo info){
			if(info.getPwdIsRight()){
				return info.getIsOnline() ? R.drawable.avs_status_connected : R.drawable.avs_status_unknow;
			}else{
				return R.drawable.avs_status_pwderror;
			}
		}
		
		private int getStateTxtDrawable(CameraInfo info){
			if(info.getPwdIsRight()){
				return info.getIsOnline() ? R.string.online : R.string.offline;
			}else{
				return R.string.pwd_wrong;
			}
		}
	}

}
