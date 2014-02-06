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
 * Describes the connection's state of the realtime session.
 */
public enum ConnectionState
{
  /**
   * The connection is connecting. The player should wait until the connection
   * is fully connected.
   */
  CONNECTING,

  /**
   * The connection is connected. The player can try to create/join/leave rooms
   * and exchange data with room's participants.
   */
  CONNECTED,

  /**
   * The connection is disconnected. The player should call {@link
   * Session#connect} or {@link Session#reconnect} to open a new connection.
   */
  DISCONNECTED
}
