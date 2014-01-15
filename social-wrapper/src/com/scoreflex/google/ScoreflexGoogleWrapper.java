package com.scoreflex.google;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.model.people.Person;
import com.scoreflex.SocialCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class ScoreflexGoogleWrapper {

	static Stack<SocialCallback> sCallbackStack = new Stack<SocialCallback>();

	public static int REQUEST_CODE_START_RESOLUTION_FOR_RESULT = 634885;
	public static int REQUEST_CODE_SHARING = 634884;
	private static final String GOOOGLE_SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email";
	private static PlusClient mPlusClient;
	
	public static class GoogleException extends java.lang.Exception {

		private static final long serialVersionUID = 2345287315358228130L;
		private int mErrorCode;

		public GoogleException(String message) {
			super(message);
		}

		public GoogleException(int errorCode) {
			super(String.format("Google service exception code=%d", errorCode));
			mErrorCode = errorCode;
		}

		public int getErrorCode() {
			return mErrorCode;
		}

		public GoogleException(Exception e) {
			super(e);
		}
	}

	public static boolean isGoogleAvailable(Context context) {
		try {
			Class.forName("com.google.android.gms.plus.PlusClient");
			return true;
		} catch (ClassNotFoundException e) {
			Log.d("Scoreflex", "Google not available");
			return false;
		}
	}

	/*
	 * public static void logout(Activity activity) { if
	 * (!isGoogleAvailable(activity)) return;
	 * 
	 * final PlusClient plusClient = newPlusClient(activity); Log.d("Scoreflex",
	 * plusClient.getAccountName()); sCallbackStack.push(new SocialCallback() {
	 * 
	 * @Override public void call(String accessToken, Exception exception) { if
	 * (plusClient.isConnected()) { plusClient.revokeAccessAndDisconnect(new
	 * PlusClient.OnAccessRevokedListener() {
	 * 
	 * @Override public void onAccessRevoked(ConnectionResult status) {
	 * Log.d("Scoreflex", "Plus access revoked:" + status.getErrorCode()); } }); }
	 * } }); plusClient.connect(); }
	 */

	protected static class PlusClientCallbackHandler implements
			PlusClient.ConnectionCallbacks,
			GooglePlayServicesClient.OnConnectionFailedListener {
		PlusClient mPlusClient;
		Activity mActivity;

		public PlusClientCallbackHandler(Activity activity) {
			mActivity = activity;
		}

		public void setPlusClient(PlusClient plusClient) {
			mPlusClient = plusClient;
		}

		public PlusClient getPlusClient() {
			return mPlusClient;
		}

		@Override
		public void onDisconnected() {
//			Log.e("Scoreflex", "Google plus disconnected");
		}

		@Override
		public void onConnected(Bundle connectionHint) {

			if (!sCallbackStack.empty()) {
				final SocialCallback callback = sCallbackStack.pop();

//				Log.d("Scoreflex", "GooglePlus connected");

				// Extract access token on a separate thread
				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							final String accessToken = GoogleAuthUtil.getToken(mActivity,
									mPlusClient.getAccountName(), "oauth2:" + Scopes.PLUS_LOGIN
											+ " " + GOOOGLE_SCOPE_EMAIL); // http://stackoverflow.com/questions/12689858/newly-released-authentication-with-google-play-services-problems-with-getting

							// Call back on the main thread
							mActivity.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									callback.call(accessToken, null);
								}
							});
						} catch (Exception e) {
							callback.call(null, e);
						}
					}
				}).start();
			} else {
				Log.w("Scoreflex", "Callback stack is empty, could not message success");
			}
		}

		@Override
		public void onConnectionFailed(ConnectionResult result) {
			if (result.hasResolution()) {
//				Log.d("Scoreflex", "ConnectionFailed, has resolution");
				try {
					result.startResolutionForResult(mActivity,
							REQUEST_CODE_START_RESOLUTION_FOR_RESULT);
				} catch (SendIntentException e) {
					Log.e("Scoreflex", "Could not startResolutionForResult", e);
				}

			} else {
//				Log.d("Scoreflex", "ConnectionFailed, no resolution");
				GoogleException e = new GoogleException(result.getErrorCode());
				if (!sCallbackStack.empty()) {
					SocialCallback callback = sCallbackStack.pop();
					callback.call(null, e);
				} else {
					Log.w("Scoreflex",
							"Callback stack is empty, could not message error", e);
				}
			}
		}
	}

	protected static PlusClient newPlusClient(final Activity activity) {
//		Log.d("Scoreflex", "Created new plus client");
		PlusClientCallbackHandler handler = new PlusClientCallbackHandler(activity);
		PlusClient plusClient = new PlusClient.Builder(activity, handler, handler)
				.setScopes(Scopes.PLUS_LOGIN, GOOOGLE_SCOPE_EMAIL).setActions("http://schemas.google.com/AddActivity").build();
		handler.setPlusClient(plusClient);
		return plusClient;
	}

	public static void login(Activity activity, SocialCallback callback)
			throws ScoreflexGoogleWrapper.GoogleException {
		if (!isGoogleAvailable(activity))
			throw new ScoreflexGoogleWrapper.GoogleException(
					"GooglePlus SDK is not available");

		try {

			// Store the callback
			sCallbackStack.push(callback);

			// Connect on google plus.
			mPlusClient = newPlusClient(activity);
			mPlusClient.connect();

		} catch (Exception e) {
			throw new GoogleException(e);
		}
	}
	
	public static void ensureConnectedAndRun(final Activity activity, final Runnable runable) {
		mPlusClient = newPlusClient(activity);
		SocialCallback callback = new SocialCallback() {

			@Override
			public void call(String accessToken, Exception exception) {
				runable.run();
			}
		};
		sCallbackStack.push(callback);
		mPlusClient.connect();
	}
	
	
	public static void shareUrl(final Activity activity, final String text, final String url) {
		ensureConnectedAndRun(activity, new Runnable() {
			@Override
			public void run() {
				PlusShare.Builder builder = new PlusShare.Builder(activity, mPlusClient);
				Uri uri = null;
				if (!TextUtils.isEmpty(url)) {
					uri = Uri.parse(url);
				} else {
					uri = Uri.parse("http://www.scoreflex.com");
				}

				builder.setType("text/plain");
				builder.setText(text);
				if (uri != null) {
					builder.setContentUrl(uri);
				} 
				activity.startActivityForResult(builder.getIntent(), ScoreflexGoogleWrapper.REQUEST_CODE_SHARING);
			}
		});
	}

	public static void sendInvitation(final Activity activity, final String text, final List<String> friendIds, final String url ,final String deepLinkPath) {
		ensureConnectedAndRun(activity, new Runnable() {
			@Override
			public void run() {
				PlusShare.Builder builder = new PlusShare.Builder(activity, mPlusClient);
				Uri uri = null;
				if (!TextUtils.isEmpty(url)) {
					uri = Uri.parse(url);
				} else {
					uri = Uri.parse("http://www.scoreflex.com");
				}
					
				
				if (deepLinkPath != null && uri != null) {
					builder.addCallToAction("ACCEPT",uri,deepLinkPath);
				}
				
				if (uri != null) {
					builder.setContentUrl(uri);
				} 
				
				if (deepLinkPath != null) {
					builder.setContentDeepLinkId(deepLinkPath, null, null, null);
				}
				
				if (friendIds != null) {
					List<Person> invited = new ArrayList<Person>();
//					invited.add(PlusShare.createPerson("110375645949560550333", "test"));
					for (String friendId : friendIds) { 
//						Log.d("Scoreflex", "Added friend: " + friendId);
						invited.add(PlusShare.createPerson(friendId, "Redwan Meslem"));
					}	
					builder.setRecipients(invited);
						
				}
				
				builder.setText(text);
				activity.startActivityForResult(builder.getIntent(), ScoreflexGoogleWrapper.REQUEST_CODE_SHARING);
			}
		});
	}

	public static void onActivityResult(Activity activity, int requestCode,
			int responseCode, Intent intent) {
		if (ScoreflexGoogleWrapper.REQUEST_CODE_START_RESOLUTION_FOR_RESULT == requestCode) {
			if (responseCode == Activity.RESULT_OK) {
				mPlusClient = newPlusClient(activity);
			  mPlusClient.connect();
			} else {
				Log.i("Scoreflex", "Resolution for result with NOK response code");
				if (!sCallbackStack.empty()) {
					SocialCallback callback = sCallbackStack.pop();
					callback.call(
							null,
							new GoogleException(String.format(
									"Resolution for result brought response code %d",
									responseCode)));
				} else {
					Log.w("Scoreflex",
							"Callback stack empty, could not message resolution problem");
				}
			}
		}
		if (ScoreflexGoogleWrapper.REQUEST_CODE_SHARING == requestCode) { 
			
		}
	}

}
