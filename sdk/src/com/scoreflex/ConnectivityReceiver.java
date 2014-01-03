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

import com.scoreflex.Scoreflex.Response;
import com.scoreflex.Scoreflex.ResponseHandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


/**
 * A class that monitors the connectivity state of the device
 *
 */
public class ConnectivityReceiver extends BroadcastReceiver {

	private boolean checkCaptivePortalSafe(NetworkInfo.DetailedState state) {
		try {
			return state == NetworkInfo.DetailedState.valueOf("CAPTIVE_PORTAL_CHECK");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Method called by the {@link #android.net.ConnectivityManager} to notify connectivity changes.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			ConnectivityManager cm = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (intent.getExtras() != null) {
				NetworkInfo ni = (NetworkInfo) cm.getActiveNetworkInfo();
				if (ni != null
						&& ni.isConnected()
						&& !checkCaptivePortalSafe(ni.getDetailedState())) {
					if (Scoreflex.isInitialized()) {
						Scoreflex.get("/network/ping", null, new ResponseHandler() {

							@Override
							public void onFailure(Throwable e, Response errorResponse) {

							}

							@Override
							public void onSuccess(Response response) {

							}
						});
					}
				} else {
					Scoreflex.setNetworkAvailable(false);
				}
			}
			if (intent.getExtras().getBoolean(
					ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE)) {
				Scoreflex.setNetworkAvailable(false);
			}
		} catch (Exception e) {
			Log.d(
					"Scoreflex",
					"You might want to add : android.permission.ACCESS_NETWORK_STATE to your permissions");
			return;
		}
	}

}
