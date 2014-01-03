package com.scoreflex.realtime;

public class Connection {

	public static class Builder {

		public String host;
		public int port;

		public String getHost() {
			return host;
		}

		public Builder setHost(String host) {
			this.host = host;
			return this;
		}

		public int getPort() {
			return port;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Connection build() {
			return new Connection(host, port);
		}

	}

	public static interface Listener {

		public void onConnected(Session session);

		public void onClosed();

		public void onFailed();

		public void onPong();

	}

	public static class AbstractListener implements Listener {

		@Override
		public void onConnected(Session session) {
		};

		@Override
		public void onClosed() {
		}

		@Override
		public void onFailed() {
		}

		@Override
		public void onPong() {
		}

	}

	public final String host;
	public final int port;

	public Session session;

	public Connection(String host, int port) {
		this.host = host;
		this.port = port;
		this.session = null;
		// TODO Establish connection
	}

	public Session getSession() {
		return session;
	}

	public void connect() {
		// TODO
	}

	public void disconnect() {
		// TODO
	}

	public void sync() {
		// TODO
	}

	public void ping() {
		// TODO
	}

}
