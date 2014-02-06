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
 * session} in reply to a Ping request.
 */
public interface PingListener {
  /**
   * This method is called when a reply to a ping request is received before the
   * timeout.
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#ping
   *
   * @param latency The latency in milliseconds to receive the ping reply.
   */
  public void onPong(long latency);

  /**
   * This method is called when a ping request timed out.
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#ping
   */
  public void onPang();
}
