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
 * Describes the match's state in a room.
 *
 * <p>A room can be configured to start automatically a match when the minimum
 * number of participants required is reached. Else a match can be started
 * manually by calling {@link Session#startMatch}.<br>
 *
 * In a same way, a room can be configured to stop a match automatically when
 * the number of participants become lower than the minimum required. Else a
 * match can be stopped manually by calling {@link Session#stopMatch}.<br>
 *
 * When a match is in the {@link #FINISHED} state, it should be reset before
 * starting a new match by calling {@link Session#resetMatch}.</p>
 */
public enum MatchState
{
  /**
   * The match is not started yet. The minimum number of participants required
   * to start a match is not reached yet.
   */
  PENDING,

  /**
   * The match is ready to be started. The room was configured to not start
   * matches automatically. So to start a match, {@link Session#startMatch}
   * should be called.
   */
   READY,

  /**
   * The match is started. It will remain in this state until {@link
   * Session#stopMatch} is called or the auto-stop condition is triggered.
   */
   RUNNING,

  /**
   * The match is finished. To start a new match, {@link Session#resetMatch}
   * should be called.
   */
   FINISHED,

  /**
   * This is a special state used in {@link RoomListener#onMatchStateChanged}
   * callback when an error occurred.
   */
   UNKNOWN
}
