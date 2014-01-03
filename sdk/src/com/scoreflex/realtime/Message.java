package com.scoreflex.realtime;

import java.util.Map;

public class Message {

	private final String roomId;
	private final long timestamp;
	private final String fromId;
	private final String toId;
	private final int flags;
	private final int tag;
	private final Map<String, Object> payload;

	Message(String roomId, long timestamp, String fromId, String toId,
			int flags, int tag, Map<String, Object> payload) {
		this.roomId = roomId;
		this.timestamp = timestamp;
		this.fromId = fromId;
		this.toId = toId;
		this.flags = flags;
		this.tag = tag;
		this.payload = payload;
	}

	public String getRoomId() {
		return roomId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getFromId() {
		return fromId;
	}

	public String getToId() {
		return toId;
	}

	public int getFlags() {
		return flags;
	}

	public int getTag() {
		return tag;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

}
