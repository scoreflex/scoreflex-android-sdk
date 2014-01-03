package com.scoreflex.realtime;

import java.util.Collections;
import java.util.Map;

public class Player {

	private final String id;
	private final Map<String, Object> properties;

	Player(String id, Map<String, Object> properties) {
		this.id = id;
		this.properties = Collections.unmodifiableMap(properties);
	}

	public String getId() {
		return id;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

}
