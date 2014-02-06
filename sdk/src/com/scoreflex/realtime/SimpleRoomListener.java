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
 * A convenience class to extend when you only want to listen for a subset of
 * the room events. This implements all methods in the {@link RoomListener}
 * interface but does nothing. You just need to override the ones you need.
 */
public class SimpleRoomListener implements RoomListener {
  /**
   * Called when a player attempts to create a realtime room.
   *
   * @see RoomListener#onRoomCreated
   */
  public void onRoomCreated(int status_code, Room room) {
  }

  /**
   * Called when a realtime room is closed.
   *
   * @see RoomListener#onRoomClosed
   */
  public void onRoomClosed(int status_code, String room_id) {
  }

  /**
   * Called when a client attempts to join realtime room.
   *
   * @see RoomListener#onRoomJoined
   */
  public void onRoomJoined(int status_code, Room room) {
  }

  /**
   * Called when a client attempts to leave a realtime room.
   *
   * @see RoomListener#onRoomLeft
   */
  public void onRoomLeft(int status_code, String room_id) {
  }

  /**
   * Called when a participant joins a room.
   *
   * @see RoomListener#onPeerJoined
   */
  public void onPeerJoined(Room room, Participant peer) {
  }

  /**
   * Called when a participant leave a room.
   *
   * @see RoomListener#onPeerLeft
   */
  public void onPeerLeft(Room room, String peer_id) {
  }

  /**
   * Called when the match's state of a room change.
   *
   * @see RoomListener#onMatchStateChanged
   */
  public void onMatchStateChanged(int status_code, Room room,
                                  MatchState new_state) {
  }

  /**
   * Called when a property of a room change.
   *
   * @see RoomListener#onRoomPropertyChanged
   */
  public void onRoomPropertyChanged(Room room, Participant from, String key) {
  }

  /**
   * Called when an attempt to change a room's property failed.
   *
   * @see RoomListener#onSetRoomPropertyFailed
   */
  public void onSetRoomPropertyFailed(int status_code, Room room, String key) {
  }
}
