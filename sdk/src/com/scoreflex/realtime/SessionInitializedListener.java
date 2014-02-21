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
 * An interface that contains the callbacks used by the {@link Session realtime
 * session} to notify the application of the result of its initialization.
 *
 * @see Session#initialize
 */
public interface SessionInitializedListener {
  /**
   * This method is called asynchronously after a call to {@link
   * Session#initialize} if the initialization succeeds.
   */
  public void onSuccess();

  /**
   * This method is called when an error occurred during the realtime session
   * initialization.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_NETWORK_ERROR} A network error occurred.
   *   <li>{@link Session#STATUS_PERMISSION_DENIED} The application does not
   *   have permissions to use the realtime service.</li>
   *   <li>{@link Session#STATUS_INTERNAL_ERROR} A unexpected error
   *   occurred.</li>
   * </ul>
   *
   * @param status_code a status code indicating the reason of the failure.
   */
  public void onFailure(int status_code);
}
