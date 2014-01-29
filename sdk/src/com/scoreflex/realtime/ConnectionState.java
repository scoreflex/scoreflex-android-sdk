package com.scoreflex.realtime;

/**
 * Describes the connection's state of the realtime session.
 */
public enum ConnectionState
{
  /**
   * The connection is connecting. The player should wait until the connection
   * is fully connected.
   */
  CONNECTING,

  /**
   * The connection is connected. The player can try to create/join/leave rooms
   * and exchange data with room's participants.
   */
  CONNECTED,

  /**
   * The connection is disconnected. The player should call {@link
   * Session#connect(ConnectionListener)} or {@link Session#reconnect()} to open
   * a new connection.
   */
  DISCONNECTED
}
