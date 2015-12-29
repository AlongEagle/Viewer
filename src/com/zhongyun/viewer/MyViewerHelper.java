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

import android.content.Context;
import android.widget.Toast;

import com.ichano.rvs.viewer.constant.LoginError;
import com.ichano.rvs.viewer.constant.LoginState;
import com.ichano.rvs.viewer.constant.RvsSessionState;
import com.ichano.rvs.viewer.constant.StreamerConfigState;
import com.ichano.rvs.viewer.constant.StreamerPresenceState;
import com.ichano.rvs.viewer.ui.ViewerInitHelper;

public class MyViewerHelper extends ViewerInitHelper{

	private static MyViewerHelper mViewer;
	private LoginListener mLoginListener;
	private CameraStateListener mCameraStateListener;
	
	public static MyViewerHelper getInstance(Context applicationContext){
		if(null == mViewer){
			mViewer = new MyViewerHelper(applicationContext);
		}
		mViewer.login();
		return mViewer;
	}
	
	private MyViewerHelper(Context applicationContext) {
		super(applicationContext);
	}
	
	@Override
	public String getAppID() {
		return "open_source";
	}

	@Override
	public String getCompanyID() {
		return "open_source";
	}

	@Override
	public long getCompanyKey() {
		return 0;
	}

	@Override
	public String getLicense() {
		return "open_source";
	}

	@Override
	public void onLoginResult(LoginState loginState, int progressRate, LoginError errorCode) {
		if(LoginState.CONNECTED == loginState){
			if(null != mLoginListener) mLoginListener.onLoginResult(true);
		}else if(LoginState.DISCONNECT == loginState){
			if(null != mLoginListener) mLoginListener.onLoginResult(false);
			//如果想做成产品，需要到我们的官网注册并得到授权，否则只能做演示使用。
			if(errorCode == LoginError.ERR_WRONG_PACKAGE){
				Toast.makeText(context, R.string.wrong_package_name, Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public void onSessionStateChange(long remoteCID, RvsSessionState sessionState) {
		if(null != mCameraStateListener){
			 mCameraStateListener.onCameraConnectionChange(remoteCID, RvsSessionState.CONNECTED == sessionState);
		 }
	}

	@Override
	public void onStreamerConfigState(long streamerCID, StreamerConfigState state) {
		
	}

	@Override
	public void onStreamerPresenceState(long streamerCID, StreamerPresenceState state) {
		 if(null != mCameraStateListener){
			 mCameraStateListener.onCameraStateChange(streamerCID, state);
		 }
	}

	@Override
	public void onUpdateCID(long cid) {
		
	}

	public void setLoginListener(LoginListener l){
		mLoginListener = l;
	}
	
	public void setCameraStateListener(CameraStateListener l){
		mCameraStateListener = l;
	}
	
	public interface LoginListener{
		public void onLoginResult(boolean success);
	}
	
	public interface CameraStateListener{
		public void onCameraStateChange(long streamerCID, StreamerPresenceState state);
		public void onCameraConnectionChange(long streamerCID, boolean connected);
	}
}
