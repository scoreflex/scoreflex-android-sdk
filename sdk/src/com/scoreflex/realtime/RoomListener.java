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

/**
 * An interface that contains callbacks used by the {@link Session realtime
 * session} to notify the player when the state of the room or the status of its
 * participants change.
 */
public interface RoomListener {
  /**
   * This method is called when a player attempts to create a realtime room. If
   * the room is successfully created, then the player joins the room
   * automatically.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SUCCESS} The room was successfully
   *   created.</li>
   *   <li>{@link Session#STATUS_SESSION_NOT_CONNECTED} The attempt to create
   *   the room failed because the player's session is not connected to the
   *   service.</li>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} The attempt to create the room
   *   failed due to a network error.</li>
   *   <li>{@link Session#STATUS_PERMISSION_DENIED} The attempt to create the
   *   room failed because the player does not have permissions to create
   *   it.</li>
   *   <li>{@link Session#STATUS_ROOM_ALREADY_CREATED} The attempt to create the
   *   room failed because another room with the some ID already exists.</li>
   *   <li>{@link Session#STATUS_INVALID_DATA} The attempt to create the room
   *   because of an invalid room's configuration.</li>
   *   <li>{@link Session#STATUS_INTERNAL_ERROR} The attempt to create the room
   *   due to an unexpected error.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#createRoom(String, RoomConfig, Map, Map)
   *
   * @param status_code A status code indication the result of the operation.
   * @param room The room that was created. If an error occurred, the room is
   * <code>null</code>.
   */
  public void onRoomCreated(int status_code, Room room);

  /**
   * This method is called when a realtime room is closed.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_ROOM_CLOSED} The room was closed normally by an
   *   external way.</li>
   *   <li>{@link Session#STATUS_INTERNAL_ERROR} The room was closed due to an
   *   unexpected error.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @param status_code A status code indication the reason of the closure.
   * @param room_id The room's ID which was closed.
   */
  public void onRoomClosed(int status_code, String room_id);

  /**
   * This method is called when a client attempts to join realtime room.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SUCCESS} The player has joined the room
   *   successfully.</li>
   *   <li>{@link Session#STATUS_SESSION_NOT_CONNECTED} The attempt to join the
   *   room failed because the player's session is not connected to the
   *   service.</li>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} The attempt to join the room
   *   failed due to a network error.</li>
   *   <li>{@link Session#STATUS_PERMISSION_DENIED} The attempt to join the room
   *   failed because the player does not have permissions to join it.</li>
   *   <li>{@link Session#STATUS_ROOM_NOT_FOUND} The attempt to join the room
   *   failed because the room does not exists.</li>
   *   <li>{@link Session#STATUS_ROOM_FULL} The attempt to join the room failed
   *   because the maximum number of participants allowed to join the room was
   *   reached.</li>
   *   <li>{@link Session#STATUS_INTERNAL_ERROR} The attempt to join the room
   *   failed due to an unexpected error.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#joinRoom(String, RoomConfig, Map)
   *
   * @param status_code A status code indication the result of the operation.
   * @param room The room that was joined. If an error occurred, the room is
   * <code>null</code>.
   */
  public void onRoomJoined(int status_code, Room room);

  /**
   * This method is called when a client attempts to leave a realtime room.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SUCCESS} The player has left the room
   *   successfully.</li>
   *   <li>{@link Session#STATUS_SESSION_NOT_CONNECTED} The attempt to leave the
   *   room failed because the player's session is not connected to the
   *   service.</li>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} The attempt to leave the room
   *   failed due to a network error.</li>
   *   <li>{@link Session#STATUS_INTERNAL_ERROR} The attempt to leave the room
   *   failed due to an unexpected error.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#leaveRoom()
   *
   * @param status_code A status code indication the result of the operation.
   * @param room_id The room's ID which was left.
   */
  public void onRoomLeft(int status_code, String room_id);

  /**
   * This method is called when a participant joins a room.
   *
   * <br>
   * This callback is called on the main thread.
   *
   * @param room The room that the participant joined.
   * @param peer The participant that joins the room.
   */
  public void onPeerJoined(Room room, Participant peer);

  /**
   * This method is called when a participant leave a room.
   *
   * <br>
   * This callback is called on the main thread.
   *
   * @param room The room that the participant left.
   * @param peer_id The participant's ID that leaves the room.
   */
  public void onPeerLeft(Room room, String peer_id);

  /**
   * This method is called when the match's state of a room change. It can
   * change automatically or manually by calling {@link Session#startMatch()},
   * {@link Session#stopMatch()} or {@link Session#resetMatch()}.
   * <br>
   * When a participant tries to change the match's state, if it succeed, all
   * participants will be notified. But, he will be the only one notified when
   * an error occurred.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SUCCESS} The match's state has changed.</li>
   *   <li>{@link Session#STATUS_SESSION_NOT_CONNECTED} The attempt to change
   *   the match's state failed because the player's session is not connected to
   *   the service.</li>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} The attempt to change
   *   the match's state failed due to a network error.</li>
   *   <li>{@link Session#STATUS_ROOM_NOT_JOINED} The attempt to change the
   *   match's state failed because the player is not a room's participant.</li>
   *   <li>{@link Session#STATUS_BAD_STATE} The attempt to change the match's
   *   state failed because the player tries to do an invalid change (e.g. the
   *   match cannot be stopped if it is not running).</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#startMatch()
   * @see Session#stopMatch()
   * @see Session#resetMatch()
   *
   * @param status_code A status code indication the result of the operation.
   * @param room The room.
   * @param new_state The new match's state. If an error occurred, the state is
   * {@link MatchState#UNKNOWN}.
   */
  public void onMatchStateChanged(int status_code, Room room,
                                  MatchState new_state);

  /**
   * This method is called when a property of a room change. This is done by
   * calling {@link Session#setRoomProperty(String, Object)}. If the operation
   * succeed, all participants will be notified. But, he will be the only one
   * notified when an error occurred.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SUCCESS} The room's property has changed.</li>
   *   <li>{@link Session#STATUS_SESSION_NOT_CONNECTED} The attempt to change
   *   the room's property failed because the player's session is not connected
   *   to the service.</li>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} The attempt to change the room's
   *   property failed due to a network error.</li>
   *   <li>{@link Session#STATUS_ROOM_NOT_JOINED} The attempt to change the
   *   room's property failed because the player is not a room's
   *   participant.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#setRoomProperty(String, Object)
   *
   * @param status_code A status code indication the result of the operation.
   * @param room The room.
   * @param participant_id The participant's ID that performs the operation.
   * @param key The property key that was changed.
   */
  public void onRoomPropertyChanged(int status_code, Room room,
                                    String participant_id, String key);

  /**
   * This method is called when a property of a participant change. This is done
   * by calling {@link Session#setCurrentParticipantProperty(String,
   * Object)}. If the operation succeed, all participants will be notified. But,
   * he will be the only one notified when an error occurred.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SUCCESS} The participant's property has
   *   changed.</li>
   *   <li>{@link Session#STATUS_SESSION_NOT_CONNECTED} The attempt to change
   *   the participant's property failed because the player's session is not
   *   connected to the service.</li>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} The attempt to change the
   *   participant's property failed due to a network error.</li>
   *   <li>{@link Session#STATUS_ROOM_NOT_JOINED} The attempt to change the
   *   participant's property failed because the player is not a room's
   *   participant.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#setCurrentParticipantProperty(String, Object)
   *
   * @param status_code A status code indication the result of the operation.
   * @param room The room.
   * @param participant_id The participant's ID that performs the operation.
   * @param key The property key that was changed.
   */
  public void onParticipantPropertyChanged(int status_code, Room room,
                                           String participant_id, String key);
}
