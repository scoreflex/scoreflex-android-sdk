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

import java.lang.IllegalStateException;

/**
 * The configuration used by players to create or join a room.
 */
// FIXME: Add exemple here
public class RoomConfig {
  private final RoomListener            room_listener;
  private       MessageReceivedListener message_listener;
  private       RealtimeMap             room_config;


  /**
   * Builder class for {@link RoomConfig}
   */
  public static class Builder {
    private final RoomListener            room_listener;
    private       MessageReceivedListener message_listener;
    private       Integer                 max_participants;
    private       Integer                 min_participants;
    private       Integer                 tick_time;
    private       Boolean                 auto_start;
    private       Boolean                 auto_stop;


    private Builder(RoomListener listener) {
      this.room_listener = listener;
    }

    /**
     * Sets the maximum number of participants allowed to join the room. This is
     * a required parameter.
     *
     * @param n the maximum number of participants.
     */
    public Builder setMaxParticipants(int n) {
      max_participants = n;
      return this;
    }

    /**
     * Sets the minimum number of participants required to start a match. This
     * is an optional parameter. If not set, it is same than the maximum number
     * of participants allowed to join the room.
     *
     * @param n the minimum number of participants.
     */
    public Builder setMinParticipants(int n) {
      min_participants = n;
      return this;
    }

    /**
     * Sets the room's tick-time, in milliseconds. This is an optional
     * parameter. If no tick-time is defined, every messages will be dispatched
     * by the sent as soon as possible.
     *
     * @param t The tick-time value.
     */
    public Builder setTickTime(int t) {
      tick_time = t;
      return this;
    }

    /**
     * Sets the auto-start flag for the room. This is a required parameter.
     *
     * @param b The auto-start flag value.
     */
    public Builder setAutoStart(boolean b) {
      auto_start = b;
      return this;
    }

    /**
     * Sets the auto-stop flag for the room. This is a required parameter.
     *
     * @param b The auto-stop flag value.
     */
    public Builder setAutoStop(boolean b) {
      auto_stop = b;
      return this;
    }

    /**
     * Sets the listener used to notify the player of messages received from a
     * participant.
     *
     * @param listener The {@link MessageReceivedListener}.
     */
    public Builder setMessageListener(MessageReceivedListener listener) {
      message_listener = listener;
      return this;
    }

    /**
     * Builds a new {@link RoomConfig} object.
     *
     * @return The build {@link RoomConfig} instance.
     */
    public RoomConfig build() {
      // FIXME: check the configuration here
      return new RoomConfig(this);
    }
  }

  /**
   * Creates a builder to create a {@link RoomConfig}. a listener should be
   * provide, and it must not be <code>null</code>.
   *
   * @param listener The {@link RoomListener} used to notify the application of
   * the room changes.
   *
   * @return An instance of a builder.
   */
  public static Builder builder(RoomListener listener) {
    // FIXME: listener must be != null
    return new Builder(listener);
  }

  private RoomConfig(Builder builder) {
    this.room_listener = builder.room_listener;
    this.room_config   = new RealtimeMap();

    if (builder.max_participants != null)
      this.room_config.put("max_players", builder.max_participants);
    if (builder.min_participants != null)
      this.room_config.put("min_players", builder.min_participants);
    if (builder.tick_time != null)
      this.room_config.put("tick_time", builder.tick_time);
    if (builder.auto_start != null)
      this.room_config.put("auto_start", builder.auto_start);
    if (builder.auto_stop != null)
      this.room_config.put("auto_stop", builder.auto_stop);

    this.message_listener = builder.message_listener;
  }

  /**
   * Retrieves the listener used to notify the player of the room changes.
   *
   * @return The configured {@link RoomListener}.
   */
  public RoomListener getRoomListener() {
    return room_listener;
  }

  /**
   * Retrieves the listener used to notify the player of messages received from
   * a participant.
   *
   * @return The configured {@link MessageReceivedListener}
   */
  public MessageReceivedListener getMessageListener() {
    return message_listener;
  }

  /**
   * Retrieves the room's configuration as defined by the builder used to create
   * the {@link RoomConfig}.
   *
   * @return a {@link RealtimeMap} representing the room's configuration.
   */
  public RealtimeMap getRoomConfig() {
    return room_config;
  }
}
