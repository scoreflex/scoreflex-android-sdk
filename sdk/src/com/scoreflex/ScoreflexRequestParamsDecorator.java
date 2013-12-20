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

import java.util.ArrayList;
import java.util.List;

import com.scoreflex.facebook.ScoreflexFacebookWrapper;
import com.scoreflex.google.ScoreflexGoogleWrapper;

import android.location.Location;
import android.util.Log;

/**
 * A static helper class that will add parameters to a
 * {@link Scoreflex.RequestParams} object depending on the resource path and
 * user configuration of the {@link Scoreflex} object.
 *
 *
 *
 */
class ScoreflexRequestParamsDecorator {

	protected static void decorate(String resource,
			Scoreflex.RequestParams params) {

		// Always add lang
		addParameterIfNotPresent(params, "lang", Scoreflex.getLang());

		// Always add location
		addParameterIfNotPresent(params, "location", Scoreflex.getLocation());

		// Add the SID for web resources
		if (resource.startsWith("/web"))
			params.put("sid", ScoreflexRestClient.getSID());

		List<String> handledServices = new ArrayList<String>();
		// Supported client-side authentication methods
		if (ScoreflexFacebookWrapper.isFacebookAvailable(Scoreflex
				.getApplicationContext())) {
			handledServices.add("Facebook:login|invite|share");
		}
		if (ScoreflexGoogleWrapper.isGoogleAvailable(Scoreflex
				.getApplicationContext())) {
			handledServices.add("Google:login|invite|share");
		}

		if (0 < handledServices.size()) {
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < handledServices.size(); i++) {
				if (0 < i)
					buf.append(',');
				buf.append(handledServices.get(i));
			}
			addParameterIfNotPresent(params, "handledServices", buf.toString());
		}




	}

	private static void addParameterIfNotPresent(
			Scoreflex.RequestParams params, String paramName, String paramValue) {
		if (null == params || null == paramName || null == paramValue)
			return;

		if (params.has(paramName))
			return;

		params.put(paramName, paramValue);
	}

	private static void addParameterIfNotPresent(
			Scoreflex.RequestParams params, String paramName,
			Location paramValue) {
		if (null == paramValue)
			return;

		addParameterIfNotPresent(params, paramName,
				"" + paramValue.getLatitude() + "," + paramValue.getLongitude());
	}
}
