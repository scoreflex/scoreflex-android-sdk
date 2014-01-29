package com.scoreflex.realtime;

import java.util.Map;

/**
 * A convenience class to extend when you only want to listen for a subset of
 * the connection events. This implements all methods in the {@link
 * ConnectionListener} interface but does nothing. You just need to override the
 * ones you need.
 */
public class SimpleConnectionListener implements ConnectionListener {
  /**
   * Called when a new connection is established.
   *
   * @see ConnectionListener#onConnected
   *
   * @param session_info a {@link Map} containing information about the player's
   * session.
   */
  public void onConnected(Map<String, Object> session_info) {
  }

  /**
   * Called when an error occurred on the connection.
   *
   * @see ConnectionListener#onConnectionFailed
   *
   * @param status_code a status code indicating the reason of the connection
   * failure.
   */
  public void onConnectionFailed(int status_code) {
  }

  /**
   * Called when the player's session is closed or when a connection is replaced
   * by a new one.
   *
   * @see ConnectionListener#onConnectionClosed
   *
   * @param status_code a status code indicating the reason of the connection
   * closure.
   */
  public void onConnectionClosed(int status_code) {
  }

  /**
   * Called to notify the application that the connection needs to be reopenned.
   *
   * @see ConnectionListener#onReconnecting
   *
   * @param status_code a status code indicating the reason of the reconnection.
   */
  public void onReconnecting(int status_code) {
  }
}
