package com.zhongyun.viewer;

import com.ichano.rvs.viewer.Command;
import com.ichano.rvs.viewer.StreamerInfoMgr;
import com.ichano.rvs.viewer.Viewer;
import com.ichano.rvs.viewer.callback.CommandCallback;
import com.zhongyun.viewer.db.CameraInfo;
import com.zhongyun.viewer.db.CameraInfoManager;
import com.zhongyun.viewer.utils.Constants;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CameraInfoSettingActivity extends BaseActivity
	implements View.OnClickListener, CommandCallback{

	private TextView titleView;
	private EditText deviceNameView;
	private EditText passwordView;
	private EditText confirmPasswordView;
	private Button modifyBtn;
	private Command command;
	private StreamerInfoMgr streamerInfoMgr;
	private CameraInfoManager cameraInfoManager;
	private CameraInfo cameraInfo;
	private long changePwdRequestId = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_info_setting);
		
		titleView = (TextView) findViewById(R.id.title);
		titleView.setText(R.string.modify_camera_info);
		deviceNameView = (EditText) findViewById(R.id.device_name);
		passwordView = (EditText) findViewById(R.id.password);
		confirmPasswordView = (EditText) findViewById(R.id.password_confirm);
		modifyBtn = (Button) findViewById(R.id.modify);
		modifyBtn.setOnClickListener(this);
		
		command = Viewer.getViewer().getCommand();
		command.setCmdCallback(this);
		streamerInfoMgr = Viewer.getViewer().getStreamerInfoMgr();
		
		cameraInfoManager = new CameraInfoManager(this);
		Intent intent = getIntent();
		if(null != intent){
			long cid = intent.getLongExtra(Constants.INTENT_CID, 0);
			cameraInfo = cameraInfoManager.getCameraInfo(cid);
			deviceNameView.setText(cameraInfo.getCameraName());
			passwordView.setText(cameraInfo.getCameraPwd());
			confirmPasswordView.setText(cameraInfo.getCameraPwd());
		}
		
	}
	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch(id){
		case R.id.modify:
			String deviceName = deviceNameView.getText().toString();
			String pwd = passwordView.getText().toString();
			String pwdConfirm = confirmPasswordView.getText().toString();
			
			if(null == deviceName || null == pwd || null == pwdConfirm){
				Toast.makeText(CameraInfoSettingActivity.this, R.string.empty_info, Toast.LENGTH_LONG).show();
				return;
			}
			if("".equals(deviceName) || "".equals(pwd) || "".equals(pwdConfirm)){
				Toast.makeText(CameraInfoSettingActivity.this, R.string.empty_info, Toast.LENGTH_LONG).show();
				return;
			}
			if(!pwdConfirm.equals(pwd)){
				Toast.makeText(CameraInfoSettingActivity.this, R.string.confirm_pwd_error, Toast.LENGTH_LONG).show();
				return;
			}
			if(pwd.length()<6){
				Toast.makeText(CameraInfoSettingActivity.this, R.string.short_pwd, Toast.LENGTH_LONG).show();
				return;
			}
			if(pwd.matches("[a-zA-Z]+")||pwd.matches("[0-9]+")){
				Toast.makeText(CameraInfoSettingActivity.this, R.string.invalid_pwd, Toast.LENGTH_LONG).show();
				return;
			}
			if (!deviceName.matches("[\\[\\]\\{\\}\\(\\)\\*@!\":;,\\.%#\\|\\?\\/_\\+-\\\\='~\\$^&<>a-zA-Z0-9_\u4e00-\u9fa5]*")){
				Toast.makeText(CameraInfoSettingActivity.this, R.string.invalid_device_name, Toast.LENGTH_LONG).show();
				return;
			}
			if(!deviceName.equals(cameraInfo.getCameraName())){
				boolean ret = streamerInfoMgr.setStreamerName(cameraInfo.getCid(), deviceName);
				if(ret) {
					cameraInfo.setCameraName(deviceName);
					cameraInfoManager.update(cameraInfo);
					Toast.makeText(CameraInfoSettingActivity.this, R.string.change_device_name_success, Toast.LENGTH_LONG).show();
				}else{
					Toast.makeText(CameraInfoSettingActivity.this, R.string.change_device_name_fail, Toast.LENGTH_LONG).show();
				}
			}
			if(!pwd.equals(cameraInfo.getCameraPwd())){
				changePwdRequestId = command.changeStreamerLoginUserPwd(cameraInfo.getCid(), cameraInfo.getCameraUser(), pwd);
				cameraInfo.setCameraPwd(pwd);
			}
			
			break;
		}
	}
	
	@Override
	public void onCmdRequestStatus(long requestID, int statusCode) {
		if(changePwdRequestId == requestID){
			if(0 == statusCode){
				cameraInfoManager.update(cameraInfo);
				Toast.makeText(CameraInfoSettingActivity.this, R.string.change_password_success, Toast.LENGTH_LONG).show();
			}else{
				Toast.makeText(CameraInfoSettingActivity.this, R.string.change_password_fail, Toast.LENGTH_LONG).show();
			}
		}
	}
}
