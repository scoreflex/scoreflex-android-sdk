package com.scoreflex;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

public class ScoreflexBroadcastReceiver extends BroadcastReceiver {
	private static final String METADATA_ICON_KEYNAME = "notificationIcon";
	private static final String METADATA_ACTIVITY_KEYNAME = "activityName";
	
	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Context context, Intent intent) {
			ComponentName receiver = new ComponentName(context, this.getClass());
			try {
				Bundle metaData = context.getPackageManager().getReceiverInfo(receiver, PackageManager.GET_META_DATA).metaData;
				int iconId = metaData.getInt(METADATA_ICON_KEYNAME, -1); 
				String activityName = metaData.getString(METADATA_ACTIVITY_KEYNAME);
				if (iconId == -1) {
					Log.e("Scoreflex","You should specify a <meta-data android:name=\"notificationIcon\" android:resource=YourApplicationIcon/> in your Receiver definition of your AndroidMabnifest.xml");
					return;
				}
				if (activityName == null) { 
					Log.e("Scoreflex","You should specify a <meta-data android:name=\"activityName\" android:value=YourMainActivity/> in your Receiver definition of your AndroidMabnifest.xml");
					return;
				}
				ScoreflexGcmClient.onBroadcastReceived(context, intent, iconId, (Class<? extends Activity>) Class.forName(activityName));
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				Log.e("Scoreflex","The activity you provided in  android:name=\"activityName\" doesn't exist");
				e.printStackTrace();
			}
	}

}
