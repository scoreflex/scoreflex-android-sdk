package com.scoreflex.realtime;

public class Session {

	public static interface Listener {

		public void onRoomCreated(Room room);

		public void onRoomJoined(Room room);

		public void onRoomWatched(Room room);

	}

	public static class AbstractListener implements Listener {

		@Override
		public void onRoomCreated(Room room) {
		}

		@Override
		public void onRoomJoined(Room room) {
		}

		@Override
		public void onRoomWatched(Room room) {
		}

	}

	private final Connection connection;
	private final String gameId;
	private final String clientId;
	private final String accessToken;

	public Room room;

	Session(Connection connection, String gameId, String clientId,
			String accessToken) {
		this.connection = connection;
		this.gameId = gameId;
		this.clientId = clientId;
		this.accessToken = accessToken;
	}

	public Connection getConnection() {
		return connection;
	}

	public String getGameId() {
		return gameId;
	}

	public String getClientId() {
		return clientId;
	}

	public Room getRoom() {
		return room;
	}

	public void createRoom() {
		// TODO
	}

	public void joinRoom(String roomId) {
		// TODO
	}

	public void watchRoom(String roomId) {
		// TODO
	}

}
