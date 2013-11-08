package com.scoreflex.facebook;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.internal.Utility;
import com.scoreflex.SocialCallback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScoreflexFacebookWrapper {
	public static class FacebookException extends java.lang.Exception {
		private static final long serialVersionUID = 3069729850758580472L;

		public FacebookException(String message) {
			super(message);
		}
		
		public FacebookException(Exception e) {
			super(e);
		}
	}
	
	public static boolean isFacebookAvailable(Context context) {
		try {
			Class.forName("com.facebook.FacebookSdkVersion");
			return null != Utility.getMetadataApplicationId(context);
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static void login(Activity activity, final SocialCallback callback) throws ScoreflexFacebookWrapper.FacebookException {
		if (!isFacebookAvailable(activity))
			throw new ScoreflexFacebookWrapper.FacebookException("Facebook SDK is not available");

		try {
			Session.openActiveSession(activity, true,
					new Session.StatusCallback() {

						@Override
						public void call(Session session, SessionState state,
								Exception exception) {
							String accessToken = null;
							if (null != session && session.isOpened())
								accessToken = session.getAccessToken();
							callback.call(accessToken, exception);
						}
					});
		} catch (Exception e) {
			throw new FacebookException(e);
		}
	}
	
	public static void logout(Activity activity) throws ScoreflexFacebookWrapper.FacebookException {
		if (!isFacebookAvailable(activity))
			throw new ScoreflexFacebookWrapper.FacebookException("Facebook SDK is not available");
			
		try {
			Session session = Session.getActiveSession();
			if (null != session)
				session.closeAndClearTokenInformation();
		} catch (Exception e) {
			throw new FacebookException(e);
		}
	}

	public static void onActivityResult(Activity activity, int requestCode,
			int resultCode, Intent data) {
		if (!isFacebookAvailable(activity))
			return;

		Session session = Session.getActiveSession();
		if (null != session)
			session.onActivityResult(activity, requestCode, resultCode, data);
	}
}
