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

package com.scoreflex;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.http.NoHttpResponseException;
import org.json.JSONException;

import com.scoreflex.Scoreflex.Response;

import android.util.Log;

/**
 * This class will make sure important {@link ScoreflexRestClient.Request}
 * objects are run eventually, even if the user is currently offline and the app
 * terminated.
 *
 *
 */
class ScoreflexRequestVault {

	private static ScoreflexRequestVault sDefaultVault;

	public static ScoreflexRequestVault getDefaultVault() {
		return sDefaultVault;
	}

	/**
	 * Start the default vault.
	 */
	public static void initialize() {
		if (null == sDefaultVault)
			sDefaultVault = new ScoreflexRequestVault(
					ScoreflexJobQueue.getDefaultQueue());
	}

	private ScoreflexJobQueue mJobQueue;
	private Thread mThread;

	public ScoreflexRequestVault(ScoreflexJobQueue jobQueue) {
		mJobQueue = jobQueue;
		mThread = new Thread(getRunnable());
		mThread.start();
	}

	/**
	 * Save a request in the vault for future retry
	 *
	 * @param request
	 * @throws JSONException
	 */
	public void put(ScoreflexRestClient.Request request) throws JSONException {
		mJobQueue.postJobWithDescription(request.toJSON());
	}

	private Runnable getRunnable() {
		return new Runnable() {

			@Override
			public void run() {
				try {
					while (true) {

						// Sleep for 10 seconds
						Thread.sleep(1000 * 10);

						// Blocks
						final ScoreflexJobQueue.Job job = mJobQueue.nextJob();

						final ScoreflexRestClient.Request request;
						try {
							request = new ScoreflexRestClient.Request(
									job.getJobDescription());
						} catch (JSONException e) {
							Log.e("Scoreflex", "Could not restore request", e);
							continue;
						}

						request.setHandler(new Scoreflex.ResponseHandler() {

							@Override
							public void onFailure(Throwable e,
									Response errorResponse) {

								// Post back to job queue if this is a network
								// error
								if (e instanceof NoHttpResponseException
										|| e instanceof UnknownHostException
										|| e instanceof SocketException) {
									job.repost();
									return;
								}

//								super.onFailure(e, errorResponse);
							}

							@Override
							public void onSuccess(Response response) {
								// TODO Auto-generated method stub

							}

						});
						ScoreflexRestClient.requestAuthenticated(request);

					}
				} catch (InterruptedException e) {
					Log.i("Scoreflex", "Vault interrupted", e);
				}

			}
		};
	}
}
