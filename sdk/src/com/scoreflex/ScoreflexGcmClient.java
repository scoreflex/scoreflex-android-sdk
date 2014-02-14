/*
 * Licensed to Scoreflex (www.scoreflex.com) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Scoreflex licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.scoreflex;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.scoreflex.google.ScoreflexGcmWrapper;
import com.scoreflex.google.ScoreflexGoogleWrapper;
import com.scoreflex.Scoreflex.Response;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;


/**
 * A class that handles all the messages form Google Cloud Messaging service
 *
 */
public class ScoreflexGcmClient {
	private static final String GCM_REGISTRATION_ID_PREF_NAME = "__scoreflex_gcm_registration_id";
	private static final String GCM_REGISTRATION_APP_VERSION_PREF_NAME = "__scoreflex_gxm_registration_app_version";
	private static final String SCOREFLEX_CUSTOM_DATA_EXTRA_KEY = "custom";
	private static final String SCOREFLEX_NOTIFICATION_EXTRA_KEY = "_sfx";


	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	private static void storeRegistrationIdToScoreflex(String registrationId){
		Scoreflex.RequestParams params = new Scoreflex.RequestParams();
		params.put("token", registrationId);
		Scoreflex.postEventually("/notifications/deviceTokens", params, new Scoreflex.ResponseHandler(){
			public void onFailure(Throwable e, Response errorResponse) {

			}

			public void onSuccess(Response response) {

			}
		});
	}

	protected static String getRegistrationId(Context c) {
		final SharedPreferences prefs = Scoreflex.getSharedPreferences(c);
		String registrationId = prefs.getString(GCM_REGISTRATION_ID_PREF_NAME, "");
		if (registrationId == null || registrationId.length() >0) {
			return "";
		}

		int registeredVersion = prefs.getInt(
				GCM_REGISTRATION_APP_VERSION_PREF_NAME, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(Scoreflex.getApplicationContext());
		if (registeredVersion != currentVersion) {
			return "";
		}
		return registrationId;
	}

	protected static void storeRegistrationId(String registrationId, Context c) {
		final SharedPreferences prefs = Scoreflex.getSharedPreferences(c);
		int appVersion = getAppVersion(c);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(GCM_REGISTRATION_ID_PREF_NAME, registrationId);
		editor.putInt(GCM_REGISTRATION_APP_VERSION_PREF_NAME, appVersion);
		editor.commit();
	}

	protected static PendingIntent buildPendingIntent(JSONObject scoreflexData, Context context, Class<? extends Activity> activity) {
		Intent resultIntent = new Intent(context, activity);

		resultIntent.putExtra(Scoreflex.NOTIFICATION_EXTRA_KEY,scoreflexData.toString());

		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		stackBuilder.addParentStack(activity);
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
       stackBuilder.getPendingIntent(
           0,
           PendingIntent.FLAG_UPDATE_CURRENT
       );
		return resultPendingIntent;
	}

	protected static Notification buildNotification(String text, Context context, int iconResource, PendingIntent pendingIntent)
	{
    final PackageManager pm = context.getApplicationContext().getPackageManager();
    ApplicationInfo ai;
    try {
        ai = pm.getApplicationInfo( context.getPackageName(), 0);
    } catch (final NameNotFoundException e) {
        ai = null;
    }
    final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
    NotificationCompat.Builder mBuilder =
       new NotificationCompat.Builder(context)
       .setContentTitle(applicationName)
       .setContentText(text).setSmallIcon(iconResource);

		mBuilder.setContentIntent(pendingIntent);
		Notification notification = mBuilder.build();
		notification.defaults =  Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		return notification;
	}

	protected static boolean onBroadcastReceived(Context context, Intent intent, int iconResource, Class<? extends Activity> activity) {
		 Bundle extras = intent.getExtras();

     if (extras.isEmpty()) {  // has effect of unparcelling Bundle
    	 return false;
     }
     String customDataJson = extras.getString(SCOREFLEX_CUSTOM_DATA_EXTRA_KEY);
     if (null == customDataJson) {
    	 return false;
     }

     try {

    	JSONObject customData = new JSONObject(customDataJson);
        JSONObject data = customData.getJSONObject(SCOREFLEX_NOTIFICATION_EXTRA_KEY);
        JSONObject sfxData = data.optJSONObject("data");

            if (data.getInt("code") < Scoreflex.NOTIFICATION_TYPE_CHALLENGE_INVITATION) {
                return false;
            }
            String targetPlayerId = sfxData.optString("targetPlayerId");
			String loggedPlayerId = ScoreflexRestClient.getPlayerId(context);

			if (!targetPlayerId.equals(loggedPlayerId)) {
				return false;
			}
			PendingIntent pendingIntent = buildPendingIntent(data, context, activity);
			Notification notification= buildNotification(extras.getString("alert"), context, iconResource, pendingIntent);

			NotificationManager mNotificationManager =
					(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(data.getInt("code"), notification);
			intent.removeExtra(SCOREFLEX_CUSTOM_DATA_EXTRA_KEY);
		} catch (JSONException e1) {

		}

		return false;
	}

	private static void registerInBackground(final String senderId, final Context activity) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                    new AsyncTask<Object, Object, Object>() {
                        @Override
                        protected Object doInBackground(Object... arg0) {
                            String msg = "";
                            try {
                                String regid = ScoreflexGcmWrapper.register(Scoreflex.getApplicationContext(), senderId);
                                if (regid == null) {
                                    return null;
                                }
                                msg = "Device registered, registration ID=" + regid;
                                storeRegistrationId(regid, activity);
                                storeRegistrationIdToScoreflex(regid);
                            } catch (IOException ex) {
                                msg = "Error :" + ex.getMessage();
                            }
                            return msg;
                        }

                        @Override
                        protected void onPostExecute(Object msg) {

                    }
                }.execute(null, null, null);
            }
        });
    }

	/**
	 * Start the registration process for GCM.
	 * @param senderID The sender ID to register to.
	 * @param context A valid context
	 */
	@SuppressLint("NewApi")
	public static void registerForPushNotification(Context context) {
		String regid = getRegistrationId(context);
	  String pushSenderId = null;
	  try {
	  	ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
	  	Bundle bundle = ai.metaData;
	  	pushSenderId = bundle.getString("com.scoreflex.push.SenderId");
	  } catch (NameNotFoundException e) {
	  	Log.e("Scoreflex", "Could not get com.scoreflex.push.SenderId meta data from your manifest did you add : <meta-data android:name=\"com.scoreflex.push.SenderId\" android:value=\"@string/push_sender_id\"/>");
	  } catch (NullPointerException e) {
	  	Log.e("Scoreflex", "Could not get com.scoreflex.push.SenderId meta data from your manifest did you add : <meta-data android:name=\"com.scoreflex.push.SenderId\" android:value=\"@string/push_sender_id\"/>");
	  }

	  if (pushSenderId == null)  {
	  	return;
	  }

	  if (TextUtils.isEmpty(regid)) {
	  	registerInBackground(pushSenderId, context);
	  } else {
	  	storeRegistrationIdToScoreflex(regid);
	  }
	  return;
	}
}
