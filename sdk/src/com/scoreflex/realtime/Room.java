package com.scoreflex.realtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Room {

	public static enum State {
		PENDING, READY, RUNNING, FINISHED;
	}

	public static interface Listener {

		public void onLeft();

		public void onUnwatched();

		public void onAck();

		public void onPeerJoined(Player player);

		public void onPeerLeft(Player player);

		public void onStateChanged(State fromState, State toState);

		public void onMessageReceived(Message message);

		public void onPropertiesUpdated(Map<String, Object> properties);

		public void onPlayerPropertiesUpdated(Player player,
				Map<String, Object> properties);

	}

	public static final class AbstractListener implements Listener {

		@Override
		public void onLeft() {
		}

		@Override
		public void onUnwatched() {
		}

		@Override
		public void onAck() {
		}

		@Override
		public void onPeerJoined(Player player) {
		}

		@Override
		public void onPeerLeft(Player player) {
		}

		@Override
		public void onStateChanged(State fromState, State toState) {
		}

		@Override
		public void onMessageReceived(Message message) {
		}

		@Override
		public void onPropertiesUpdated(Map<String, Object> properties) {
		}

		@Override
		public void onPlayerPropertiesUpdated(Player player,
				Map<String, Object> properties) {
		}

	}

	private final Session session;
	private final String id;

	private State state;
	private final Map<String, Object> config;
	private final Map<String, Object> configView;
	private final Map<String, Object> properties;
	private final Map<String, Object> propertiesView;
	private final List<Player> players;
	private final List<Player> playersView;

	Room(Session session, String id) {
		this.session = session;
		this.id = id;

		this.state = null; // TODO
		this.config = new HashMap<String, Object>();
		this.properties = new HashMap<String, Object>();
		this.players = new ArrayList<Player>();

		this.configView = Collections.unmodifiableMap(this.config);
		this.propertiesView = Collections.unmodifiableMap(this.properties);
		this.playersView = Collections.unmodifiableList(this.players);
	}

	public Session getSession() {
		return session;
	}

	public String getId() {
		return id;
	}

	public State getState() {
		return state;
	}

	public Map<String, Object> getConfig() {
		return configView;
	}

	public Map<String, Object> getProperties() {
		return propertiesView;
	}

	public List<Player> getPlayers() {
		return playersView;
	}

	public void leave() {
		// TODO
	}

	public void unwatch() {
		// TODO
	}

	public void start() {
		// TODO
	}

	public void stop() {
		// TODO
	}

	public void reset() {
		// TODO
	}

	public void setProperty(String key, Object value) {
		// TODO
	}

	public void setPlayerProperty(String key, Object value) {
		// TODO
	}

	public void sendMessage(Map<String, Object> payload, String from, String to, int tag) {
		// TODO
	}

	public void sendReliableMessage(Map<String, Object> payload, Listener ackListener, String from, String to, int tag) {
		// TODO
	}

}
