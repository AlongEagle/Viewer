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
import com.zhongyun.viewer.utils.Constants;
import com.zhongyun.viewer.utils.ZYDateUtils;
import com.zhongyun.viewer.utils.FileUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

public class WatchActivity extends BaseActivity 
	implements View.OnClickListener{

	private static final String TIME_UP_ERROR = "TIME_UP";
	private static final int DEFAULT_CAMERA_INDEX = 0;
	private static final String AD_UNIT_ID = "ca-app-pub-4895175297664182/3943432951";
	private GLMediaView mGLMediaView;
	private long mCid;
	private Handler mHandler = new Handler();
	private Dialog mLinkFailDlg;
	private Dialog mExitDialog;
	private ProgressDialog mWaitingDialog;
	
	private LinearLayout mSoundSwitcherView;
	private ImageView mSoundSwitcherIconView;
	private TextView mSoundSwitcherNameView;
	
	private LinearLayout mRecordVideoView;
	private ImageView mRecordVideoIconView;
	private TextView mRecordVideoNameView;
	private String mRecordVideoPath;
	
	private LinearLayout mHoldTalkView;
	private ImageView mHoldTalkIconView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_watch);
		TextView titleView = (TextView) findViewById(R.id.title);
		initOperateView();
		mGLMediaView = (GLMediaView) findViewById(R.id.media_view);
		mCid = getIntent().getLongExtra(Constants.INTENT_CID, 0);
		String title = getIntent().getStringExtra(Constants.INTENT_CAMERA_NAME);
		titleView.setText(title);
		mGLMediaView.bindCid(mCid, DEFAULT_CAMERA_INDEX);
		mGLMediaView.openAudio(true);//打开音频采集。
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
		ad.loadAd(new AdRequest());
		adContainer.addView(ad);
	}
	
	private void initOperateView(){
		mSoundSwitcherView = (LinearLayout) findViewById(R.id.sound_switcher);
		mSoundSwitcherView.setOnClickListener(this);
		mSoundSwitcherIconView = (ImageView) findViewById(R.id.sound_switcher_icon);
		mSoundSwitcherNameView = (TextView) findViewById(R.id.sound_switcher_name);
		
		mRecordVideoView = (LinearLayout) findViewById(R.id.record_video);
		mRecordVideoView.setOnClickListener(this);
		mRecordVideoIconView = (ImageView) findViewById(R.id.record_video_icon);
		mRecordVideoNameView = (TextView) findViewById(R.id.record_video_name);
		mRecordVideoPath = FileUtils.mkdirsOnSDCard(Constants.RECORD_VIDEO_PATH).getAbsolutePath();
		
		mHoldTalkView = (LinearLayout) findViewById(R.id.hold_talk);
		mHoldTalkView.setOnClickListener(this);
		mHoldTalkIconView = (ImageView) findViewById(R.id.hold_talk_icon);
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

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch(id){
		case R.id.sound_switcher:
			if(mGLMediaView.isSoundOn()){
				mGLMediaView.soundOff();
				mSoundSwitcherIconView.setImageResource(R.drawable.sound_on);
				mSoundSwitcherNameView.setText(R.string.sound_on);
			}else{
				mGLMediaView.soundOn();
				mSoundSwitcherIconView.setImageResource(R.drawable.sound_off);
				mSoundSwitcherNameView.setText(R.string.sound_off);
			}
			break;
		case R.id.record_video:
			if(mGLMediaView.isRecordingVideo()){
				boolean ret = mGLMediaView.stopRecordVideo();
				mRecordVideoIconView.setImageResource(R.drawable.record_off);
				mRecordVideoNameView.setText(R.string.record);
				if(ret){
					String toastStr = getResources().getString(R.string.recording_saved, mRecordVideoPath);
					Toast.makeText(this, toastStr, Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(this, R.string.record_failed, Toast.LENGTH_LONG).show();
				}
			}else{
				if(FileUtils.hasSDCard()){
					String path = mRecordVideoPath + "/" + ZYDateUtils.getTime() + Constants.VIDEO_MP4;
					mGLMediaView.startRecordVideo(path);
					mRecordVideoIconView.setImageResource(R.drawable.record_on);
					mRecordVideoNameView.setText(R.string.recording);
				}
			}
			break;
		case R.id.hold_talk:
			if(mGLMediaView.isSendRevAudio()){
				mGLMediaView.stopSendRevAudio();
				mHoldTalkIconView.setImageResource(R.drawable.hold_talk);
			}else{
				mGLMediaView.startSendRevAudio();
				mHoldTalkIconView.setImageResource(R.drawable.hold_talk_pressed);
			}
			break;
			default:
				break;
		}
		
	}
}
