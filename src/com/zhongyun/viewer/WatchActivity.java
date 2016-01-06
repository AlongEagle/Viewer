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

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.ichano.rvs.viewer.ui.GLMediaView;
import com.ichano.rvs.viewer.ui.GLMediaView.LinkCameraStatusListener;
import com.umeng.analytics.MobclickAgent;
import com.zhongyun.viewer.utils.AppUtils;
import com.zhongyun.viewer.utils.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WatchActivity extends Activity {

	private static final String TIME_UP_ERROR = "TIME_UP";
	private static final int DEFAULT_CAMERA_INDEX = 0;
	private static final String AD_UNIT_ID = "ca-app-pub-4895175297664182/3943432951";
	private GLMediaView mGLMediaView;
	private long mCid;
	private Handler mHandler = new Handler();
	private Dialog mLinkFailDlg;
	private Dialog mExitDialog;
	private ProgressDialog mWaitingDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		AppUtils.setStatusBarTransparent(this, getResources().getColor(R.color.title_red));
		TextView titleView = (TextView) findViewById(R.id.title);
		mGLMediaView = (GLMediaView) findViewById(R.id.media_view);
		mCid = getIntent().getLongExtra(Constants.INTENT_CID, 0);
		String title = getIntent().getStringExtra(Constants.INTENT_CAMERA_NAME);
		titleView.setText(title);
		mGLMediaView.bindCid(mCid, DEFAULT_CAMERA_INDEX);
		mGLMediaView.openAudio(true);
		mGLMediaView.setOnLinkCameraStatusListener(new LinkCameraStatusListener() {
			
			@Override
			public void startToLink() {
				mWaitingDialog.show();
			}
			
			@Override
			public void linkSucces() {
				mWaitingDialog.dismiss();
			}
			
			@Override
			public void linkFailed(String msg) {
				if(TIME_UP_ERROR.equals(msg)){
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							Toast.makeText(WatchActivity.this, R.string.time_up_error, Toast.LENGTH_LONG).show();
						}
					});
				}else{
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							mWaitingDialog.dismiss();
							showLinkFailDlg();
						}
					});
				}
			}
		});
		
		mWaitingDialog = new ProgressDialog(this);
		mWaitingDialog.setMessage(getString(R.string.waiting));
		mWaitingDialog.setIndeterminate(true);
		mWaitingDialog.setCancelable(true);
		
		//admob ad
		RelativeLayout adContainer = (RelativeLayout) findViewById(R.id.adLayout);
		AdView ad = new AdView(this, AdSize.BANNER, AD_UNIT_ID);
		//.addTestDevice("703C305FC29B7ED91BD7625874CFDEBC")
		ad.loadAd(new AdRequest());
		adContainer.addView(ad);
	}
	
	private void showLinkFailDlg(){
		if(null != mLinkFailDlg){
			mLinkFailDlg.show();
		}else{
			mLinkFailDlg = new AlertDialog.Builder(WatchActivity.this)
			.setTitle(R.string.camera_link_fail)
			.setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					WatchActivity.this.finish();
				}
			})
			.setOnKeyListener(new OnKeyListener() {
				
				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if(KeyEvent.KEYCODE_BACK == keyCode){
						return true;
					}
					return false;
				}
			})
			.create();
			mLinkFailDlg.show();
		}
	}
	
	private void showExitDlg(){
		if(null != mExitDialog){
			mExitDialog.show();
		}else{
			mExitDialog = new AlertDialog.Builder(WatchActivity.this)
			.setTitle(R.string.exit_camera)
			.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					WatchActivity.this.finish();
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
	
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
	
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
	
	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		showExitDlg();
	}
}
