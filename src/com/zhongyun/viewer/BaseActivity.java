package com.zhongyun.viewer;

import com.zhongyun.viewer.utils.AppUtils;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class BaseActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		AppUtils.setStatusBarTransparent(this, getResources().getColor(R.color.title_red));
	}
}
