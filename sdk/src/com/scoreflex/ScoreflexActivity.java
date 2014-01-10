package com.scoreflex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

public class ScoreflexActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		Scoreflex.registerNetworkReceiver(this);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int responseCode,
			Intent intent) {
		super.onActivityResult(requestCode, responseCode, intent);
		Scoreflex.onActivityResult(this, requestCode, responseCode, intent);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Scoreflex.unregisterNetworkReceiver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Scoreflex.unregisterNetworkReceiver(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Scoreflex.registerNetworkReceiver(this);
	}

	@Override
	public void onBackPressed() {
		if (Scoreflex.backButtonPressed() == false) {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& Scoreflex.backButtonPressed() == true) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
