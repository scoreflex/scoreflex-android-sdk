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

package com.scoreflex.realtime;

import java.util.Map;

/**
 * An interface that contains the callbacks used by the {@link Session realtime
 * session} to notify the application when the connection's state changes.
 */
public interface ConnectionListener {
  /**
   * This method is called asynchronously after a call to {@link
   * Session#connect(ConnectionListener)} or {@link Session#reconnect()} if the
   * connection is successfully established. It can also be called when during
   * an automatic reconnection.
   *
   * <p>After this callback, the connection's state is {@link
   * ConnectionState#CONNECTED} and the player can try to create/join/leave
   * rooms and exchange data with room's participants.
   * <br>
   * This callback is called on the main thread.
   * </p>
   *
   * @see Session#connect(ConnectionListener)
   * @see Session#reconnect()
   *
   * @param session_info a {@link Map} containing information about the player's
   * session.
   */
  public void onConnected(Map<String, Object> session_info);

  /**
   * This method is called when an error occurred on the connection. After this
   * call, the connection's state is {@link ConnectionState#DISCONNECTED}.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *
   *   <li>{@link Session#STATUS_NETWORK_ERROR} A network error occurred. The
   *   player will need to reconnect by calling {@link
   *   Session#connect(ConnectionListener)} or {@link Session#reconnect()}</li>
   *   <li>{@link Session#STATUS_PERMISSION_DENIED} The player does not have
   *   permissions to use the realtime service.</li>
   *   <li>{@link Session#STATUS_INVALID_MESSAGE} An malformed message was sent
   *   to the server.</li>
   *   <li>{@link Session#STATUS_PROTOCOL_ERROR} An unknown message was sent to
   *   the server.</li>
   *   <li>{@link Session#STATUS_ALREADY_CONNECTED} The player has already a
   *   opened session on another device.</li>
   *   <li>{@link Session#STATUS_INTERNAL_ERROR} A unexpected error
   *   occurred.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @param status_code a status code indicating the reason of the connection
   * failure.
   */
  public void onConnectionFailed(int status_code);

  /**
   * This method is called when the player's session is closed or when a
   * connection is replaced by a new one. It can be called from the time the
   * connection is established.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SESSION_CLOSED} The player's session was closed
   *   on the server side. He was unsubscribe from joined room, if any and all
   *   registered listeners was removed. After this call, the connection's state
   *   is {@link ConnectionState#DISCONNECTED}</li>
   *   <li>{@link Session#STATUS_REPLACED_BY_NEW_CONNECTION} The current
   *   connection was closed by a new one. This is an informative reason and it
   *   could be ignored.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @param status_code a status code indicating the reason of the connection
   * closure.
   */
  public void onConnectionClosed(int status_code);

  /**
   * This method is called to notify the application that the connection needs
   * to be reopenned.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} A network error occurred. If
   *   automatic reconnection was configured (see {@link
   *   Session#setReconnectFlag(boolean)}), when a network error is detected,
   *   the connection will be automatically reopened.</li>
   *   <li>{@link Session#STATUS_NEW_SERVER_LOCATION} The server requests the
   *   client to reconnect on a specific host.</li>
   * </ul>
   * <br> This callback is
   * called on the main thread.
   *
   * @param status_code a status code indicating the reason of the reconnection.
   */
  public void onReconnecting(int status_code);
}
