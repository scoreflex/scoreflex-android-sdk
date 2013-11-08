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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.net.Uri;
import android.util.Log;

/**
 * A collection of static helpers that manipulate Uri's and resources.
 *
 *
 */
class ScoreflexUriHelper {

	static Uri sBaseUri;

	/**
	 * Extracts the resource path from a Uri.
	 * @param uri
	 * @return The resource path for that Uri, starting with a '/' after the API version number. null if the provided Uri is not a Scoreflex uri (isAPIUri returns false).
	 */
	protected static String getResource(Uri uri) {
		if (!isAPIUri(uri))
			return null;

		String scheme = uri.getScheme();
		String apiScheme = getBaseUri().getScheme();

		// Strip out the protocol and store the result in the "remainder" variable
		String remainder = uri.toString().substring(scheme.length());

		if (null == remainder)
			return null;

		// Strip out the protocol from the base URI
		String apiRemainder = getBaseUri().toString().substring(apiScheme.length());

		// Check that the remainder starts with the apiRemainder
		if (!remainder.startsWith(apiRemainder))
			return null;

		// Return the path, stripped out of the base uri's path
		return uri.getPath().substring(getBaseUri().getPath().length());
	}

	/**
	 * Extracts the query parameters as {@link Scoreflex.RequestParams}
	 * @param uri
	 * @return
	 */
	protected static Scoreflex.RequestParams getParams(Uri uri) {
		Scoreflex.RequestParams params = new Scoreflex.RequestParams();
		try {
			List<NameValuePair> parsedParams = URLEncodedUtils.parse(new URI(uri.toString()), "UTF-8");
			for (NameValuePair pair : parsedParams) {
				params.put(pair.getName(), pair.getValue());

			}
		} catch (URISyntaxException e) {
			Log.e("Scoreflex", "Invalid URI", e);
		}
		return params;
	}

	/**
	 * Checks that the provided URI points to the Scoreflex REST server
	 * @param uri
	 * @return
	 */
	protected static boolean isAPIUri(Uri uri) {
		if (null == uri)
			return false;

		return getBaseUri().getHost().equals(uri.getHost());
	}

	/**
	 * @return The scoreflex base URL as a {@link android.net.Uri}
	 */
	protected static Uri getBaseUri() {
		if (null == sBaseUri && null != Scoreflex.getBaseURL())
			sBaseUri = Uri.parse(Scoreflex.getBaseURL());
		return sBaseUri;
	}

	/**
	 * Returns the absolute url for the given resource
	 * @param resource The resource path, which may or may not start with "/"+Scoreflex.API_VERSION
	 * @return
	 */
	public static String getAbsoluteUrl(String resource) {
		if (resource.startsWith("/"+Scoreflex.API_VERSION))
			resource = resource.substring(1 + Scoreflex.API_VERSION.length());
		return Scoreflex.getBaseURL() + resource;
	}

	/**
	 * Returns the non secure absolute url for the given resource
	 * @param resource The resource path, which may or may not start with "/"+Scoreflex.API_VERSION
	 * @return
	 */
	public static String getNonSecureAbsoluteUrl(String resource) {
		if (resource.startsWith("/"+Scoreflex.API_VERSION))
			resource = resource.substring(1 + Scoreflex.API_VERSION.length());
		return Scoreflex.getNonSecureBaseURL() + resource;
	}

}
