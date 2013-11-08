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

import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * A simple persistent job queue which saves itself on disk using SharedPreferences
 * and wraps an {@link ArrayBlockingQueue}.
 *
 *
 */
class ScoreflexJobQueue {
	private static int DEFAULT_CAPACITY = 100;

	/**
	 * Queued objects.
	 *
	 *
	 */
	public interface Job {
		public String getId();

		public JSONObject getJobDescription();

		public void repost();
	}

	private static ScoreflexJobQueue sDefaultQueue = new ScoreflexJobQueue(
			"DefaultScoreflexJobQueue", DEFAULT_CAPACITY);

	/**
	 * Returns the default job queue.
	 * @return
	 */
	public static ScoreflexJobQueue getDefaultQueue() {
		return sDefaultQueue;
	}

	private String mQueueName;
	private Object mMutex = new Object();
	private ArrayBlockingQueue<InternalJob> mQueue;

	/**
	 * Creates a queue with the specified name
	 * @param queueName The name of the queue, which determines the queue's storage location
	 * @param capacity The maximum number of jobs the queue can hold
	 */
	public ScoreflexJobQueue(String queueName, int capacity) {
		mQueueName = queueName;
		mQueue = new ArrayBlockingQueue<ScoreflexJobQueue.InternalJob>(capacity);
		restore();
	}

	/**
	 * Creates and stores a job in the queue based on the provided description
	 * @param jobDescription
	 * @return The stored job or null if something went wrong (the queue is full for instance)
	 */

	public synchronized Job postJobWithDescription(JSONObject jobDescription) {
		if (0 == mQueue.remainingCapacity())
			return null;

		String jobId = UUID.randomUUID().toString();
		InternalJob job = new InternalJob(jobId, jobDescription);
		mQueue.add(job);
		save();
		return job;
	}

	/**
	 * This call blocks until the next job is available.
	 * @return
	 * @throws InterruptedException
	 */
	public Job nextJob() throws InterruptedException {
		InternalJob job = mQueue.take();
		save();
		return job;
	}

	private String getPrefName() {
		return String.format("_scoreflex_job_queue_%s", mQueueName);
	}

	/**
	 * Saves the job queue on disk.
	 */
	protected void save() {
		synchronized(mMutex) {
			try {
				InternalJob jobs[] = new InternalJob[mQueue.size()];
				mQueue.toArray(jobs);
				JSONArray jsonArray = new JSONArray();
				for (int i = 0; i < jobs.length; i++) {
					jsonArray.put(jobs[i].toJSON());
				}

				SharedPreferences prefs = Scoreflex.getSharedPreferences();
				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(getPrefName(), jsonArray.toString());
				editor.commit();

			} catch (JSONException e) {
				Log.e("Scoreflex", "Could not save job queue", e);
			}

		}
	}

	/**
	 * Restores the job queue from its on-disk version.
	 */
	protected void restore() {
		synchronized(mMutex) {
			try {
				SharedPreferences prefs = Scoreflex.getSharedPreferences();
				String jsonString = prefs.getString(getPrefName(), "[]");
				JSONArray jsonArray = new JSONArray(jsonString);

				mQueue.clear();

				for (int i = 0; i < jsonArray.length(); i++) {
					mQueue.add(new InternalJob(jsonArray.getJSONObject(i)));
				}
			} catch (JSONException e) {
				Log.e("Scoreflex", "Could not restore job queue");
			}
		}
	}

	private class InternalJob implements Job {
		protected String mId;
		protected JSONObject mJobDescription;

		public InternalJob(String id, JSONObject description) {
			mId = id;
			mJobDescription = description;
		}

		public InternalJob(JSONObject json) throws JSONException {
			mId = json.getString("id");
			mJobDescription = json.getJSONObject("description");
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("id", mId);
			json.put("description", mJobDescription);
			return json;
		}

		public String getId() {
			return mId;
		}

		public JSONObject getJobDescription() {
			return mJobDescription;
		}

		public void repost() {
			mQueue.add(this);
			save();
		}

		@Override
		public boolean equals(Object object) {

			if (!(object instanceof InternalJob))
				return false;

			InternalJob job = (InternalJob)object;

			if (null == mId && null == job.mId)
				return super.equals(object);

			if (null == job.mId)
				return false;

			return job.mId.equals(mId);

		}
	}
}
