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
 * A participant is a player inside a realtime room.
 */
public class Participant {
  private final String      id;
  private final String      room_id;

  protected Participant(String id, String room_id) {
    this.id             = id;
    this.room_id        = room_id;
  }

  /**
   * Retrieves the participant's ID.
   *
   * @return The participant's ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Retrieves the room's ID of the participant.
   *
   * @return The room's ID.
   */
  public String getRoomId() {
    return room_id;
  }
}
