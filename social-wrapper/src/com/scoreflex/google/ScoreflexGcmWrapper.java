package com.scoreflex.google;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class ScoreflexGcmWrapper {
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static GoogleCloudMessaging mGcm;
	
	public static boolean isGcmAvailable() { 
		try {
			Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static boolean isGooglePlayServiceAvailable(Activity activity) { 
		try {
			Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
			int resultCode = GooglePlayServicesUtil
					.isGooglePlayServicesAvailable(activity);
			if (resultCode != ConnectionResult.SUCCESS) {
				if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
					GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
							PLAY_SERVICES_RESOLUTION_REQUEST).show();
				} else {
					Log.d(
							"Scoreflex",
							"this device does not support google play service no push notification will be received");
				}
				return false;
			}		
			return true;
		} catch (ClassNotFoundException e) { 
			return false;
		}
	}
	
	public static String register(Context context, String senderId) throws IOException { 
		 if (mGcm == null) {
			 mGcm = GoogleCloudMessaging.getInstance(context);
		 }
		return mGcm.register(senderId);
	}
	
}
