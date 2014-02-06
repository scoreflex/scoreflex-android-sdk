package com.scoreflex;

import com.scoreflex.ScoreflexView.ScoreflexViewListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

public class ScoreflexActivity extends Activity {

	public final static String INTENT_SHOW_EXTRA_KEY = "__sfx_show";
	public final static String INTENT_EXTRA_REQUEST_PARAMS_KEY = "__sfx_requestParams";
	public final static String INTENT_EXTRA_SHOW_PLAYER_PROFILE = "playerProfile";
	public final static String INTENT_EXTRA_PLAYER_PROFILE_ID = "playerProfileId";
	public final static String INTENT_EXTRA_SHOW_PLAYER_FRIENDS = "playerFriends";
	public final static String INTENT_EXTRA_SHOW_PLAYER_NEWS_FEED = "playerNewsFeed";
	public final static String INTENT_EXTRA_SHOW_PLAYER_PROFILE_EDIT = "playerProfileEdit";
	public final static String INTENT_EXTRA_SHOW_PLAYER_SETTINGS = "playerSettings";
	public final static String INTENT_EXTRA_SHOW_PLAYER_RATING = "playerRating";
	public final static String INTENT_EXTRA_SHOW_DEVELOPER_PROFILE = "developerProfile";
	public final static String INTENT_EXTRA_SHOW_FULLSCREEN_VIEW = "fullScreenView";
	public final static String INTENT_EXTRA_FULLSCREEN_RESOURCE = "fullScreenViewResource";
	public final static String INTENT_EXTRA_SHOW_CHALLENGE_DETAIL = "challengeDetail";
	public final static String INTENT_EXTRA_SHOW_DEVELOPER_GAMES = "developerGames";
	public final static String INTENT_EXTRA_CHALLENGE_INSTANCE_ID = "challengeInstanceId";
	public final static String INTENT_EXTRA_DEVELOPER_PROFILE_ID = "developerProfileId";
	public final static String INTENT_EXTRA_SHOW_GAME_DETAIL = "gameDetail";
	public final static String INTENT_EXTRA_GAME_ID = "gameId";
	public final static String INTENT_EXTRA_SHOW_GAME_PLAYERS = "gamePlayers";
	public final static String INTENT_EXTRA_SHOW_LEADERBOARD = "leaderboard";
	public final static String INTENT_EXTRA_LEADERBOARD_ID = "leaderboardId";
	public final static String INTENT_EXTRA_SHOW_LEADERBOARD_OVERVIEW = "leaderboardOverview";
	public final static String INTENT_EXTRA_SHOW_CHALLENGES = "challenges";
	public final static String INTENT_EXTRA_SHOW_SEARCH = "search";
	public final static String INTENT_EXTRA_SHOW_ABSTRACT_URL = "abstractUrl";
	public final static String INTENT_EXTRA_ABSTRACT_URL = "url";



	protected String getPlayerId(Intent intent) {
		String playerId = intent.getStringExtra(INTENT_EXTRA_PLAYER_PROFILE_ID);
		if (playerId == null) {
			playerId = "me";
		}
		return playerId;
	}

	protected String getDeveloperId(Intent intent) {
		return intent.getStringExtra(INTENT_EXTRA_DEVELOPER_PROFILE_ID);
	}

	protected String getGameId(Intent intent) {
		return intent.getStringExtra(INTENT_EXTRA_GAME_ID);
	}

	protected String getLeaderboardId(Intent intent) {
		return intent.getStringExtra(INTENT_EXTRA_LEADERBOARD_ID);
	}

	@Override
	protected void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		Scoreflex.registerNetworkReceiver(this);
		Intent startIntent = getIntent();
	  String action = startIntent.getStringExtra(INTENT_SHOW_EXTRA_KEY);
	  ScoreflexView sfxView = null;
	  if (action != null) {
	  	Scoreflex.RequestParams params = startIntent.getParcelableExtra(INTENT_EXTRA_REQUEST_PARAMS_KEY);
	  	if (action.equals(INTENT_EXTRA_SHOW_PLAYER_PROFILE)) {
	  		String playerId = getPlayerId(startIntent);
	  		sfxView = Scoreflex.showPlayerProfile(this, playerId,params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_PLAYER_FRIENDS)) {
	  		String playerId = getPlayerId(startIntent);
	  		sfxView = Scoreflex.showPlayerFriends(this, playerId, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_PLAYER_NEWS_FEED)) {
	  		sfxView = Scoreflex.showPlayerNewsFeed(this, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_PLAYER_PROFILE_EDIT)) {
	  		sfxView = Scoreflex.showPlayerProfileEdit(this, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_PLAYER_SETTINGS)) {
	  		sfxView = Scoreflex.showPlayerSettings(this, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_PLAYER_RATING)) {
	  		sfxView = Scoreflex.showPlayerRating(this, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_DEVELOPER_PROFILE)) {
	  		String developerId = getDeveloperId(startIntent);
	  		if (developerId != null) {
	  			sfxView = Scoreflex.showDeveloperProfile(this, developerId, params);
	  		}
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_DEVELOPER_GAMES)) {
	  		String developerId = getDeveloperId(startIntent);
	  		if (developerId != null) {
	  			sfxView = Scoreflex.showDeveloperGames(this, developerId, params);
	  		}
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_GAME_DETAIL)) {
	  		String gameId = getGameId(startIntent);
	  		if (gameId != null) {
	  			sfxView = Scoreflex.showGameDetails(this, gameId, params);
	  		}
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_GAME_PLAYERS)) {
	  		String gameId = getGameId(startIntent);
	  		if (gameId != null) {
	  			sfxView = Scoreflex.showGamePlayers(this, gameId, params);
	  		}
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_LEADERBOARD)) {
	  		String leaderboardId = getLeaderboardId(startIntent);
	  		if (leaderboardId != null) {
	  			sfxView = Scoreflex.showLeaderboard(this, leaderboardId, params);
	  		}
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_LEADERBOARD_OVERVIEW)) {
	  		String leaderboardId = getLeaderboardId(startIntent);
	  		if (leaderboardId != null) {
	  			sfxView = Scoreflex.showLeaderboardOverview(this, leaderboardId, params);
	  		}

	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_CHALLENGE_DETAIL)) {
	  		String challengeInstanceId = startIntent.getStringExtra(INTENT_EXTRA_CHALLENGE_INSTANCE_ID);
	  		final String resource = "/web/challenges/instances/" + challengeInstanceId;
	  		sfxView = Scoreflex.showFullScreenView(this, resource, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_CHALLENGES)) {
	  		sfxView = Scoreflex.showChallenges(this, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_SEARCH)) {
	  		sfxView = Scoreflex.showSearch(this, params);
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_FULLSCREEN_VIEW)) {
	  		String resource = startIntent.getStringExtra(INTENT_EXTRA_FULLSCREEN_RESOURCE);
	  		if (resource != null) {
	  			sfxView = Scoreflex.showFullScreenView(this, resource, params);
	  		}
	  	}
	  	if (action.equals(INTENT_EXTRA_SHOW_ABSTRACT_URL)) {
	  		String url = startIntent.getStringExtra(INTENT_EXTRA_ABSTRACT_URL);
	  		if (url != null) {
	  			sfxView = Scoreflex.showFullScreenView(this, url);
	  		}
	  	}
	  	if (sfxView != null) {
	  		sfxView.setScoreflexViewListener(new ScoreflexViewListener() {

					@Override
					public void onViewClosed() {
						ScoreflexActivity.this.finish();
					}

					@Override
					public boolean handleOpenNewFullscreenView(
							String fullUrlString) {
						return false;
					}
	  		});
	  	}
	  }
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
