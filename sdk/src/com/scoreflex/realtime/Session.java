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

package com.scoreflex.realtime;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.scoreflex.Scoreflex;

/**
 * The main entry point for realtime games. It represents the player's session
 * and handles connections with the Scoreflex's realtime service.
 */
public final class Session extends Thread {
  private static Session            session = new Session();
  private        Handler            main_handler;
  private        Handler            local_handler;
  private        Boolean            is_initialized = false;
  private        Boolean            is_started     = false;

  private       ConnectionListener  connection_listener;
  private       ConnectionState     connection_status;
  private       TCPConnection       connection;
  private       UDPConnection       udp_connection;
  private       String              host;
  private       int                 port;
  private       boolean             reconnect_flag;
  private       int                 reconnect_timeout;
  private       int                 max_retries;
  private       int                 retries;

  private       String              session_id;
  private       Map<String, Object> session_info;
  private       Room                current_room;
  private       int                 mm_time;
  private       int                 mm_latency;
  private       long                mm_clock_last_update;
  private       int                 last_msgid;
  private       int                 last_ackid;
  private       int                 last_reliable_id;
  private       int                 last_unreliable_id;

  private       int                 tcp_heartbeat_timeout = 200;
  private       int                 udp_heartbeat_timeout = 200;

  private Map<Integer, Proto.InMessage>   inmsg_queue;
  private Map<Integer, Proto.OutMessage>  outmsg_queue;

  private Map<Integer, PingListener>             ping_listeners;
  private Map<String,  RoomListener>             room_listeners;
  private Map<String,  MessageReceivedListener>  rcv_message_listeners;
  private Map<Integer, MessageSentListener>      snd_message_listeners;

  /**
   * The status code used in callbacks when an operation was successful.
   */
  public static final int STATUS_SUCCESS                    =  0;

  /**
   * The status code used in callbacks when an unexpected error occurred.
   */
  public static final int STATUS_INTERNAL_ERROR             =  1;

  /**
   * The status code used in callbacks when a network error occurred.
   */
  public static final int STATUS_NETWORK_ERROR              =  2;

  /**
   * The status code used in {@link ConnectionListener#onConnectionClosed(int)}
   *   when the player's session is closed on the server side.
   */
  public static final int STATUS_SESSION_CLOSED             =  3;

  /**
   * The status code used used in {@link
   * ConnectionListener#onConnectionClosed(int)} when the current connection is
   * closed by a new one.
   */
  public static final int STATUS_REPLACED_BY_NEW_CONNECTION =  4;

  /**
   * The status code used in {@link ConnectionListener#onReconnecting(int)} when
   *   the server requests the client to reconnect on a specific host.
   */
  public static final int STATUS_NEW_SERVER_LOCATION        =  5;

  /**
   * The status code used in {@link ConnectionListener#onConnectionFailed(int)}
   * and {@link MessageSentListener#onMessageSent(int, int)} when a malformed
   * message is sent by the player.
   */
  public static final int STATUS_INVALID_MESSAGE            =  6;

  /**
   * The status code used in {@link ConnectionListener#onConnectionFailed(int)}
   * when an unknown message was sent to the server.
   */
  public static final int STATUS_PROTOCOL_ERROR             =  7;

  /**
   * The status code used in callbacks when the player does not have permissions
   * to perform an operation.
   */
  public static final int STATUS_PERMISSION_DENIED          =  8;

  /**
   * The status code used in {@link ConnectionListener#onConnectionFailed(int)}
   * when the player has already a opened session on another device.
   */
  public static final int STATUS_ALREADY_CONNECTED          =  9;


  /**
   * The status code used in {@link RoomListener} callbacks when the player
   * attempts to perform an operation while his session is not connected on the
   * service.
   */
  public static final int STATUS_SESSION_NOT_CONNECTED      = 10;

  /**
   * The status code used in {@link RoomListener} callbacks when the player
   * attempts to perform an operation on a room that he did not joined first.
   */
  public static final int STATUS_ROOM_NOT_JOINED            = 11;

  /**
   * The status code used in {@link RoomListener#onRoomCreated(int, Room)} when
   * the player attempts to create a room with the same ID than an existing one.
   */
  public static final int STATUS_ROOM_ALREADY_CREATED       = 12;

  /**
   * The status code used {@link RoomListener#onRoomClosed(int, String)} when
   * the the room is closed normally by an external way.
   */
  public static final int STATUS_ROOM_CLOSED                = 13;

  /**
   * The status code used {@link RoomListener#onRoomJoined(int, Room)} when the
   * player attempts to join a unknown room.
   */
  public static final int STATUS_ROOM_NOT_FOUND             = 14;

  /**
   * The status code used in {@link RoomListener#onRoomJoined(int, Room)} when
   * the player attempts to join a room which the maximum number of participants
   * allowed was reached.
   */
  public static final int STATUS_ROOM_FULL                  = 15;

  /**
   * The status code used in {@link RoomListener#onRoomCreated(int, Room)} when
   * the player uses an invalid configuration to create a room.
   */
  public static final int STATUS_INVALID_DATA               = 16;

  /**
   * The status code used in {@link RoomListener#onMatchStateChanged(int, Room,
   * MatchState)} when the player attempts to do an invalid change of the
   * match's state.
   */
  public static final int STATUS_BAD_STATE                  = 17;

  /**
   * The status code used in {@link MessageSentListener#onMessageSent(int, int)}
   * when the player attempts to send a reliable message to a unknown
   * participant.
   */
  public static final int STATUS_PEER_NOT_FOUND             = 18;

  private Session() {
  }

  private static void checkInstance() {
    if (!isInitialized())
      throw new IllegalStateException("Realtime session not initialized yet");
  }

  /**
   * Initializes the realtime session. This method should be called only
   * once. The Scoreflex SDK should be initialized first (see {@link
   * Scoreflex#initialize}).
   *
   * @param host The server address which to connect.
   * @param port The port number the server is listening on.
   *
   * @throws IllegalStateException if the Scoreflex SDK is not initialized or if
   * the realtime session was already initialized.
   */
  public static void initialize(String host, int port) {
    if (!Scoreflex.isInitialized())
      throw new IllegalStateException("Scoreflex SDK is not initialized");

    synchronized (session) {
      if (isInitialized())
        throw new IllegalStateException("Realtime session already initialized");

      try {
        session.start();
        while (session.is_started == false)
          session.wait();
      }
      catch (InterruptedException e) {
      }

      session.connection_listener = null;
      session.connection_status   = ConnectionState.DISCONNECTED;
      session.connection          = null;
      session.udp_connection      = null;
      session.host                = host;
      session.port                = port;
      session.reconnect_flag      = true;
      session.reconnect_timeout   = 1000;
      session.max_retries         = 3;

      session.session_id          = null;
      session.session_info        = null;
      session.current_room        = null;
      session.retries             = 0;
      session.last_msgid          = 0;
      session.last_ackid          = 0;
      session.last_reliable_id    = 0;
      session.last_unreliable_id  = 0;

      session.inmsg_queue  = new HashMap<Integer, Proto.InMessage>();
      session.outmsg_queue = new HashMap<Integer, Proto.OutMessage>();

      session.ping_listeners        = new HashMap<Integer, PingListener>();
      session.room_listeners        = new HashMap<String,  RoomListener>();
      session.rcv_message_listeners = new HashMap<String,  MessageReceivedListener>();
      session.snd_message_listeners = new HashMap<Integer, MessageSentListener>();

      session.is_initialized = true;
    }
  }

  /**
   * Checks if the realtime session is initialized.
   *
   * @return <code>true</code> if the realtime session is initialized,
   * <code>false</code> otherwise.
   */
  public static boolean isInitialized() {
    return session.is_initialized;
  }

  /**
   * Sets the value of the reconnect flag. By setting it to <code>true</code>,
   * the session will be automatically reconnected when an error occurs. The
   * reconnect attempts will be made with a configurable delay. After a number
   * of consecutive reconnection failures, an error is notified.
   * <br><br>
   * Default value: <code>true</code>.
   *
   * @see ConnectionListener#onReconnecting(int)
   * @see ConnectionListener#onConnectionFailed(int)
   * @see #setMaxRetries(int)
   * @see #setReconnectTimeout(int)
   *
   * @param flag <code>true</code> to enable the automatic reconnection,
   * <code>false</code> otherwise.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static void setReconnectFlag(boolean flag) {
    checkInstance();
    synchronized (session) {
      session.reconnect_flag = flag;
    }
  }

  /**
   * Retrieves value of the reconnect flag.
   *
   * @return <code>true</code> if the automatic reconnection is enabled,
   * <code>false</code> otherwise.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static boolean getReconnectFlag() {
    checkInstance();
    return session.reconnect_flag;
  }

  /**
   * Sets the delay, in milliseconds, before an automatic reconnect attempt.
   * <br><br>
   * Default value: 1000ms.
   *
   * @see #setReconnectFlag(boolean)
   *
   * @param timeout The milliseconds to wait before an automatic reconnection.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static void setReconnectTimeout(int timeout) {
    checkInstance();
    synchronized (session) {
      session.reconnect_timeout = timeout;
    }
  }

  /**
   * Retrieves the delay used before an automatic reconnect attempt.
   *
   * @return The delay, in milliseconds, before an automatic reconnect attempt.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static int getReconnectTimeout() {
    checkInstance();
    return session.reconnect_timeout;
  }

  /**
   * Sets the maximum number of consecutive reconnection failures allowed before
   * notifying an error.
   * <br><br>
   * Default value: 3.
   *
   * @see #setReconnectFlag(boolean)
   *
   * @param n The maximum number of consecutive reconnection failures.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static void setMaxRetries(int n) {
    checkInstance();
    synchronized (session) {
      session.max_retries = 3;
    }
  }

  /**
   * Retrieves the maximum of consecutive reconnection failures.
   *
   * @return The maximum number of consecutive reconnection failures.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static int getMaxRetries() {
    checkInstance();
    return session.max_retries;
  }

  /**
   * Sets the TCP heartbeat timeout, in milliseconds.
   * <br><br>
   * Default value: 200ms.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static void setTcpHeartbeatTimeout(int t) {
    checkInstance();
    session.tcp_heartbeat_timeout = t;
  }

  /**
   * Retrieves the TCP heartbeat timeout.
   *
   * @return The TCP heartbeat timeout, in milliseconds.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static int getTcpHeartbeatTimeout() {
    checkInstance();
    return session.tcp_heartbeat_timeout;
  }

  /**
   * Sets the UDP heartbeat timeout, in milliseconds.
   * <br><br>
   * Default value: 200ms.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static void setUdpHeartbeatTimeout(int t) {
    checkInstance();
    session.udp_heartbeat_timeout = t;
  }

  /**
   * Retrieves the UDP heartbeat timeout.
   *
   * @return The UDP heartbeat timeout, in milliseconds.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static int getUdpHeartbeatTimeout() {
    checkInstance();
    return session.udp_heartbeat_timeout;
  }

  /**
   * Retrieves the connection state of the realtime session.
   *
   * @return the {@link ConnectionState} of the connection.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static ConnectionState getConnectionState() {
    checkInstance();
    return session.connection_status;
  }

  /**
   * Checks if the connection is connected.
   *
   * @return <code>true</code> is the connection's state is {@link
   * ConnectionState#CONNECTED}, <code>false</code> otherwise.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static boolean isConnected() {
    checkInstance();
    return (session.connection_status == ConnectionState.CONNECTED);
  }

  /**
   * Retrieves information associated to the realtime session.
   *
   * @return A Map representing the session's information. If the session is not
   * connected, this method returns <code>null</code>.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static Map<String, Object> getSessionInfo() {
    checkInstance();
    return session.session_info;
  }

  /**
   * Retrieves the room which the player joined.
   *
   * @return The current player's {@link Room}.  if the player has not joined
   * any rooms, this method returns <code>null</code>.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static Room getCurrentRoom() {
    checkInstance();
    return session.current_room;
  }


  /***************************************************************************/
  /**
   * @hide
   */
  @Override
  public void run() {
    Looper.prepare();

    main_handler  = new Handler(Looper.getMainLooper());
    local_handler = new Handler() {
      public void handleMessage(Message msg) {
      }
    };
    synchronized (this) {
      is_started = true;
      notifyAll();
    }
    Looper.loop();
  }

  protected Handler getHandler() {
    return local_handler;
  }



  /***************************************************************************/
  /**
   * Connects the session to the Scoreflex realtime service.
   * <br>
   * This method will return immediatly, and will call {@link
   * ConnectionListener#onConnected(Map)} or {@link
   * ConnectionListener#onConnectionFailed(int)} depending of the operation's
   * result.
   *
   * @param listener The {@link ConnectionListener} to use to notify the player
   * of the connection's state changes.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   * @throws IllegalArgumentException if the listener is <code>null</code>
   */
  public static void connect(final ConnectionListener listener) {
    checkInstance();
    if (listener == null)
      throw new IllegalArgumentException("Connection listener cannot be null");

    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.connection_listener = listener;
        session.doConnect();
      }
    });
  }
  /**
   *
   * Reconnects the session to the Scoreflex realtime service by keeping the
   * same {@link ConnectionListener}.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   * @throws IllegalArgumentException if the listner is <code>null</code>
   */
  public static void reconnect() {
    checkInstance();
    if (session.connection_listener == null)
      throw new IllegalArgumentException("Connection listener is not defined");

    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.doConnect();
      }
    });
  }
  private void doConnect() {
    if (retries >= max_retries) {
      retries = 0;
      onConnectionFailed(STATUS_NETWORK_ERROR);
      return;
    }

    if (udp_connection != null) {
      udp_connection.disconnect();
      udp_connection = null;
    }

    connection        = new TCPConnection(session, host, port);
    connection_status = ConnectionState.CONNECTING;

    Proto.Connect.Builder builder = Proto.Connect.newBuilder()
      .setClientId(Scoreflex.getPlayerId())
      .setGameId(Scoreflex.getClientId())
      .setAccessToken(Scoreflex.getAccessToken());
    if (session_id != null) {
      builder.setSessionId(session_id);
    }
    Proto.Connect msg = builder.build();

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "Open new connection on "+host+":"+port+
          " [last_reliable_id="+last_reliable_id+"]");

    Proto.InMessage inmsg =
      InMessageBuilder.build(0, last_reliable_id, true, msg);
    connection.connect(inmsg);
  }


  /**
   *
   * Closes the connection to the Scoreflex realtime service. This methods also
   * destroys the player's session on the server.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static void disconnect() {
    checkInstance();
    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.doDisconnect();
      }
    });
  }
  private void doDisconnect() {
    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "Disconnect session");

    if (udp_connection != null) {
      udp_connection.disconnect();
      udp_connection = null;
    }

    if (isSessionConnected()) {
      Proto.Disconnect message = Proto.Disconnect.newBuilder().build();
      connection.disconnect(InMessageBuilder.build(0, 0, true, message));
    }

    connection_status  = ConnectionState.DISCONNECTED;
    connection         = null;
    udp_connection     = null;
    session_id         = null;
    session_info       = null;
    last_msgid         = 0;
    last_ackid         = 0;
    last_unreliable_id = 0;
    last_reliable_id   = 0;
    synchronized (this) { current_room = null; }
    inmsg_queue.clear();
    outmsg_queue.clear();
    ping_listeners.clear();
    room_listeners.clear();
    rcv_message_listeners.clear();
    snd_message_listeners.clear();
  }


  /**
   * Sends a ping requests to the server. If a reply is received before the
   * timeout is reached, {@link PingListener#onPong(long)} is called. If the
   * requests timed out, {@link PingListener#onPang()} is called.
   *
   * @param listener The listener to use to notify the player of the request
   * result.
   * @param timeout The ping request timeout.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   * @throws IllegalArgumentException if the listener is <code>null</code>
   */
  public static void ping(final PingListener listener, final int timeout) {
    checkInstance();
    if (listener == null)
      throw new IllegalArgumentException("Room listener cannot be null");
    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.sendPing(listener, timeout);
      }
    });
  }
  private void sendPing(final PingListener listener, int timeout) {
    if (!isSessionConnected()) {
      onPang(listener);
      return;
    }

    final int id   = (int)getMmTime();
    Proto.Ping msg = Proto.Ping.newBuilder()
      .setId(id)
      .setTimestamp(id)
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(id, last_reliable_id, false, msg);

    boolean res;
    if (udp_connection != null && udp_connection.isConnected()) {
      res = udp_connection.sendMessage(inmsg);
    }
    else {
      res = connection.sendMessage(inmsg);
    }

    if (!res) {
      Log.i("Scoreflex", "Failed to send Ping message - id="+id);
      onPang(listener);
      return;
    }
    Log.i("Scoreflex", "Ping message sent - id="+id);

    ping_listeners.put(id, listener);
    local_handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        onPang(ping_listeners.remove(id));
      }
    }, timeout);
  }


  /**
   * Same as {@link #createRoom(String, RoomConfig, Map, Map) createRoom(id,
   * config, null, null)}.
   */
  public static void createRoom(String id, RoomConfig config) {
    createRoom(id, config, null, null);
  }
  /**
   * Same as {@link #createRoom(String, RoomConfig, Map, Map) createRoom(id,
   * config, room_props, null)}.
   */
  public static void createRoom(String id, RoomConfig config,
                                Map<String, Object> room_props) {
    createRoom(id, config, room_props, null);
  }
  /**
   * Creates a realtime room. The result of this operation will be notified by
   * the callback {@link RoomListener#onRoomCreated(int, Room)} to the given
   * {@link RoomListener} in {@link RoomConfig}.
   *
   * @param id The room's ID.
   * @param config The room's configuration.
   * @param room_props The room's properties.
   * @param participant_props The participant's properties.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   * @throws IllegalArgumentException if the room's configuration is <code>null</code>
   */
  public static void createRoom(final String id, final RoomConfig config,
                                final Map<String, Object> room_props,
                                final Map<String, Object> participant_props) {
    checkInstance();
    if (config == null)
      throw new IllegalArgumentException("Room configuration cannot be null");
    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.sendCreateRoom(id, config, room_props, participant_props);
      }
    });
  }
  private void sendCreateRoom(String id, RoomConfig config,
                              Map<String, Object> room_props,
                              Map<String, Object> participant_props) {
    if (!isConnected()) {
      onRoomCreated(config.getRoomListener(), STATUS_SESSION_NOT_CONNECTED,
                    null);
      return;
    }

    room_listeners.put(id, config.getRoomListener());
    rcv_message_listeners.put(id, config.getMessageListener());

    Proto.CreateRoom msg = Proto.CreateRoom.newBuilder()
      .setRoomId(id)
      .addAllRoomConfig(javaMapToProtoMap(config.getRoomConfig()))
      .addAllRoomProperties(javaMapToProtoMap(room_props))
      .addAllPlayerProperties(javaMapToProtoMap(participant_props))
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex", "Failed to send CreateRoom message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      onRoomCreated(config.getRoomListener(), STATUS_NETWORK_ERROR, null);
      return;
    }

    Log.i("Scoreflex", "CreateRoom message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Same as {@link #joinRoom(String, RoomListener, MessageReceivedListener,
   * Map) joinRoom(id, room_listener, message_listener, null)}.
   */
  public static void joinRoom(String id, RoomListener room_listener,
                              MessageReceivedListener message_listener) {
    joinRoom(id, room_listener, message_listener, null);
  }
  /**
   * Joins a realtime room. The result of this operation will be notified by the
   * callback {@link RoomListener#onRoomJoined(int, Room)} to the given {@link
   * RoomListener}.
   *
   * @param id The room's ID.
   * @param room_listener The {@link RoomListener} used to notify the player of
   * room's state changes.
   * @param message_listener The {@link MessageReceivedListener} used to notify
   * the player when a message is received.
   * @param participant_props The participant's properties.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   * @throws IllegalArgumentException if the one of listeners is
   * <code>null</code>
   */
  public static void joinRoom(final String id, final RoomListener room_listener,
                              final MessageReceivedListener message_listener,
                              final Map<String, Object> participant_props) {
    checkInstance();
    if (room_listener == null)
      throw new IllegalArgumentException("Room listener cannot be null");
    if (message_listener == null)
      throw new IllegalArgumentException("Message listener cannot be null");
    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.sendJoinRoom(id, room_listener, message_listener,
                             participant_props);
      }
    });
  }
  private void sendJoinRoom(String id, RoomListener room_listener,
                            MessageReceivedListener message_listener,
                            Map<String, Object> participant_props) {
    if (!isConnected()) {
      onRoomJoined(room_listener, STATUS_SESSION_NOT_CONNECTED,
                   null);
      return;
    }

    room_listeners.put(id, room_listener);
    rcv_message_listeners.put(id, message_listener);

    Proto.JoinRoom msg = Proto.JoinRoom.newBuilder()
      .setRoomId(id)
      .addAllPlayerProperties(javaMapToProtoMap(participant_props))
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex", "Failed to send JoinRoom message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      onRoomJoined(room_listener, STATUS_NETWORK_ERROR, null);
      return;
    }

    Log.i("Scoreflex", "JoinRoom message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Leaves the current room. The result of this operation will be notified by
   * the callback {@link RoomListener#onRoomLeft(int, String)} to the given
   * {@link RoomListener} set when the player has created or joined the room.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   */
  public static void leaveRoom() {
    checkInstance();
    Runnable r;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final String       room_id       = session.current_room.getId();
      final RoomListener room_listener = session.room_listeners.get(room_id);

      r = new Runnable() {
        @Override
        public void run() {
          session.sendLeaveRoom(room_id, room_listener);
        }
      };
    }
    session.local_handler.post(r);
  }
  private void sendLeaveRoom(String room_id, RoomListener room_listener) {
    if (!isSessionConnected()) {
      onRoomLeft(room_listener, STATUS_SESSION_NOT_CONNECTED, room_id);
      return;
    }

    Proto.LeaveRoom msg = Proto.LeaveRoom.newBuilder()
      .setRoomId(room_id)
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex", "Failed to send LeaveRoom message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      onRoomLeft(room_listener, STATUS_NETWORK_ERROR, room_id);
      return;
    }

    Log.i("Scoreflex", "LeaveRoom message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets the match's state to {@link MatchState#RUNNING}. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onMatchStateChanged(int, Room, MatchState)} to the given {@link
   * RoomListener} set when the player has created or joined the room.
   * <br>
   * The current match's state should be {@link MatchState#READY} to have a
   * chance to succeed.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   */
  public static void startMatch() {
    checkInstance();
    Runnable r;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final Room         room          = session.current_room;
      final RoomListener room_listener = session.room_listeners.get(room.getId());

      r = new Runnable() {
        @Override
        public void run() {
          session.sendStartMatch(room, room_listener);
        }
      };
    }
    session.local_handler.post(r);
  }
  private void sendStartMatch(Room room, RoomListener room_listener) {
    if (!isSessionConnected()) {
      onMatchStateChanged(room_listener, STATUS_SESSION_NOT_CONNECTED, room,
                         MatchState.UNKNOWN);
      return;
    }

    Proto.StartGame msg = Proto.StartGame.newBuilder()
      .setRoomId(room.getId())
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex", "Failed to send StartGame message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      onMatchStateChanged(room_listener, STATUS_NETWORK_ERROR, room,
                          MatchState.UNKNOWN);
      return;
    }

    Log.i("Scoreflex", "StartGame message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets the match's state to {@link MatchState#FINISHED}. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onMatchStateChanged(int, Room, MatchState)} to the given {@link
   * RoomListener} set when the player has created or joined the room.
   * <br>
   * The current match's state should be {@link MatchState#RUNNING} to have a
   * chance to succeed.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   */
  public static void stopMatch() {
    checkInstance();
    Runnable r;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final Room         room          = session.current_room;
      final RoomListener room_listener = session.room_listeners.get(room.getId());

      r = new Runnable() {
        @Override
        public void run() {
          session.sendStopMatch(room, room_listener);
        }
      };
    }
    session.local_handler.post(r);
  }
  private void sendStopMatch(Room room, RoomListener room_listener) {
    if (!isSessionConnected()) {
      onMatchStateChanged(room_listener, STATUS_SESSION_NOT_CONNECTED, room,
                          MatchState.UNKNOWN);
      return;
    }

    Proto.StopGame msg = Proto.StopGame.newBuilder()
      .setRoomId(room.getId())
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex", "Failed to send StopGame message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");

      onMatchStateChanged(room_listener, STATUS_NETWORK_ERROR, room,
                          MatchState.UNKNOWN);
      return;
    }

    Log.i("Scoreflex", "StopGame message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets the match's state to {@link MatchState#PENDING}. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onMatchStateChanged(int, Room, MatchState)} to the given {@link
   * RoomListener} set when the player has created or joined the room.
   * <br>
   * The current match's state should be {@link MatchState#FINISHED} to have a
   * chance to succeed.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   */
  public static void resetMatch() {
    checkInstance();
    Runnable r;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final Room         room          = session.current_room;
      final RoomListener room_listener = session.room_listeners.get(room.getId());

      r = new Runnable() {
        @Override
        public void run() {
          session.sendResetMatch(room, room_listener);
        }
      };
    }
    session.local_handler.post(r);
  }
  private void sendResetMatch(Room room, RoomListener room_listener) {
    if (!isSessionConnected()) {
      onMatchStateChanged(room_listener, STATUS_SESSION_NOT_CONNECTED, room,
                          MatchState.UNKNOWN);
      return;
    }

    Proto.ResetGame msg = Proto.ResetGame.newBuilder()
      .setRoomId(room.getId())
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex", "Failed to send ResetGame message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      onMatchStateChanged(room_listener, STATUS_NETWORK_ERROR, room,
                          MatchState.UNKNOWN);
          return;
        }

        Log.i("Scoreflex", "RestGame message sent - id="+(last_msgid+1)+
              " [last_reliable_id="+last_reliable_id+"]");
        last_msgid++;
        inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets or Updates a room's property given its key. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onRoomPropertyChanged(int, Room, String, String)} to the given
   * {@link RoomListener} set when the player has created or joined the room.
   * <br>
   * If the value is <code>null</code>, the property will be removed.
   *
   * @param key The property's key.
   * @param value The new property's value.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   * @throws IllegalArgumentException if the property's key is <code>null</code>
   */
  public static void setRoomProperty(final String key, final Object value) {
    checkInstance();
    if (key == null)
      throw new IllegalArgumentException("Property key cannot be null");
    Runnable r;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final Room         room          = session.current_room;
      final RoomListener room_listener = session.room_listeners.get(room.getId());

      r = new Runnable() {
        @Override
        public void run() {
          session.sendSetRoomProperty(room, room_listener, key, value);
        }
      };
    }
    session.local_handler.post(r);
  }
  private void sendSetRoomProperty(Room room, RoomListener room_listener,
                                   String key, Object value) {
    if (!isSessionConnected()) {
      onRoomPropertyChanged(room_listener, STATUS_SESSION_NOT_CONNECTED, room,
                            Scoreflex.getPlayerId(), key);
      return;
    }

    Proto.SetRoomProperty msg = Proto.SetRoomProperty.newBuilder()
      .setRoomId(room.getId())
      .setProperty(objectToMapEntry(key, value))
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex",
            "Failed to send SetRoomProperty message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      onRoomPropertyChanged(room_listener, STATUS_NETWORK_ERROR, room,
                            Scoreflex.getPlayerId(), key);
      return;
    }

    Log.i("Scoreflex", "SetRoomProperty message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets or Updates a player's property given its key. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onParticipantPropertyChanged(int, Room, String, String)} to
   * the given {@link RoomListener} set when the player has created or joined
   * the room.
   * <br>
   * If the value is <code>null</code>, the property will be removed.
   *
   * @param key The property's key.
   * @param value The new property's value.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   * @throws IllegalArgumentException if the property's key is <code>null</code>
   */
  public static void setCurrentParticipantProperty(final String key,
                                                   final Object value) {
    checkInstance();
    if (key == null)
      throw new IllegalArgumentException("Property key cannot be null");
    Runnable r;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final Room         room          = session.current_room;
      final RoomListener room_listener = session.room_listeners.get(room.getId());

      r = new Runnable() {
        @Override
        public void run() {
          session.sendSetCurrentParticipantProperty(room, room_listener,
                                                    key, value);
        }
      };
    }
    session.local_handler.post(r);
  }
  private void sendSetCurrentParticipantProperty(Room room,
                                                 RoomListener room_listener,
                                                 String key, Object value) {
    if (!isSessionConnected()) {
      onParticipantPropertyChanged(room_listener, STATUS_SESSION_NOT_CONNECTED,
                                   room, Scoreflex.getPlayerId(), key);
      return;
    }

    Proto.SetPlayerProperty msg = Proto.SetPlayerProperty.newBuilder()
      .setRoomId(room.getId())
      .setProperty(objectToMapEntry(key, value))
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex",
            "Failed to send SetPlayerProperty message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      onParticipantPropertyChanged(room_listener, STATUS_NETWORK_ERROR, room,
                                   Scoreflex.getPlayerId(), key);
      return;
    }

    Log.i("Scoreflex", "SetPlayerProperty message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Same as {@link #sendUnreliableMessage(String, int, Map)
   * sendUnreliableMessage(null, 0, payload)}.
   */
  public static int sendUnreliableMessage(Map<String, Object> payload) {
    return sendUnreliableMessage(null, 0, payload);
  }
  /**
   * Same as {@link #sendUnreliableMessage(String, int, Map)
   * sendUnreliableMessage(peer_id, 0, payload)}.
   */
  public static int sendUnreliableMessage(String peer_id,
                                          Map<String, Object> payload) {
    return sendUnreliableMessage(peer_id, 0, payload);
  }
  /**
   * Same as {@link #sendUnreliableMessage(String, int, Map)
   * sendUnreliableMessage(null, tag, payload)}.
   */
  public static int sendUnreliableMessage(int tag,
                                          Map<String, Object> payload) {
    return sendUnreliableMessage(null, tag, payload);
  }
  /**
   * Sends a unreliable message to a participant in the room. If peer_id is
   * <code>null</code>, the message will be broadcasted to all participants in
   * the room.
   *
   * @param peer_id The participant's ID to send the message to.
   * @param tag The tag of the message.
   * @param payload The message's data to sent.
   *
   * @return {@link #STATUS_SUCCESS} on a successful attempt, {@link
   * #STATUS_SESSION_NOT_CONNECTED} if the player's session is not connected on
   * the service, {@link #STATUS_NETWORK_ERROR} if a network error occurs or
   * {@link #STATUS_INVALID_DATA} if an unexpected error occurs.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   */
  public static int sendUnreliableMessage(final String peer_id, final int tag,
                                          final Map<String, Object> payload) {
    checkInstance();
    FutureTask<Integer> t;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final String       room_id       = session.current_room.getId();

      t = new FutureTask<Integer>(
        new Callable<Integer>() {
          @Override
          public Integer call() {
            return session.sendUnreliableMessage(room_id, peer_id, tag,
                                                 payload);
          }
        });
    }
    session.local_handler.post(t);

    try {
      Integer result = t.get();
      return result.intValue();
    }
    catch (Exception e) {
      Log.i("Scoreflex", "Failed to run future task: "+e);
      return STATUS_INTERNAL_ERROR;
    }
  }
  private int sendUnreliableMessage(String room_id, String peer_id,
                                    int tag, Map<String, Object> payload) {
    if (!isSessionConnected()) {
      return STATUS_SESSION_NOT_CONNECTED;
    }

    int msgid = (int)getMmTime();
    Proto.RoomMessage.Builder builder = Proto.RoomMessage.newBuilder()
      .setRoomId(room_id)
      .setTimestamp(msgid)
      .setTag(tag)
      .setIsReliable(false)
      .addAllPayload(javaMapToProtoMap(payload));
    if (peer_id != null) {
      builder.setToId(peer_id);
    }

    Proto.RoomMessage msg = builder.build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(msgid, last_reliable_id, false, msg);

    boolean res;
    if (udp_connection != null && udp_connection.isConnected()) {
      res = udp_connection.sendMessage(inmsg);
    }
    else {
      res = connection.sendMessage(inmsg);
    }

    if (!res) {
      Log.i("Scoreflex",
            "Failed to send unreliable RoomMessage message - id="+msgid+
            " [last_reliable_id="+last_reliable_id+"]");
      return STATUS_NETWORK_ERROR;
    }

    Log.i("Scoreflex", "Unreliable RoomMessage message sent - id="+msgid+
          " [last_reliable_id="+last_reliable_id+"]");

    return STATUS_SUCCESS;
  }


  /**
   * Same as {@link #sendReliableMessage(MessageSentListener, String, int, Map)
   * sendReliableMessage(listener, peer_id, 0, payload)}.
   */
  public static int sendReliableMessage(MessageSentListener listener,
                                        String peer_id,
                                        Map<String, Object> payload) {
    return sendReliableMessage(listener, peer_id, 0, payload);
  }
  /**
   * Sends a reliable message to a participant in the room. The caller will
   * receive a callback to report the status of the send message operation.
   *
   * @param listener the {@link MessageSentListener} used to notify the player
   * of the operation's result.
   * @param peer_id The participant's ID to send the message to.
   * @param tag The tag of the message.
   * @param payload The message's data to sent.
   *
   * @return The ID of the message sent, which will be returned in the callback
   * {@link MessageSentListener#onMessageSent(int, int)} or {@link
   * #STATUS_SESSION_NOT_CONNECTED} if the player's session is not connected on
   * the service, {@link #STATUS_NETWORK_ERROR} if a network error occurs,
   * {@link #STATUS_INVALID_DATA} if an unexpected error occurs.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   * @throws IllegalArgumentException if the listener is <code>null</code>
   */
  public static int sendReliableMessage(final MessageSentListener listener,
                                        final String peer_id, final int tag,
                                        final Map<String, Object> payload) {
    checkInstance();
    if (listener == null)
      throw new IllegalArgumentException("Room listener cannot be null");
    FutureTask<Integer> t;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");
      final String       room_id       = session.current_room.getId();

      t = new FutureTask<Integer>(
        new Callable<Integer>() {
          @Override
          public Integer call() {
            return session.sendReliableMessage(room_id, listener, peer_id, tag,
                                               payload);
          }
        });
    }
    session.local_handler.post(t);

    try {
      Integer result = t.get();
      return result.intValue();
    }
    catch (Exception e) {
      Log.i("Scoreflex", "Failed to run future task: "+e);
      return STATUS_INTERNAL_ERROR;
    }
  }
  private int sendReliableMessage(String room_id, MessageSentListener listener,
                                  String peer_id, int tag,
                                  Map<String, Object> payload) {
    if (!isSessionConnected()) {
      return STATUS_SESSION_NOT_CONNECTED;
    }

    Proto.RoomMessage.Builder builder = Proto.RoomMessage.newBuilder()
      .setRoomId(room_id)
      .setTimestamp((int)getMmTime())
      .setTag(tag)
      .setIsReliable(true)
      .addAllPayload(javaMapToProtoMap(payload));
    if (peer_id != null) {
      builder.setToId(peer_id);
    }

    Proto.RoomMessage msg = builder.build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      Log.i("Scoreflex",
            "Failed to send reliable RoomMessage message - id="+(last_msgid+1)+
            " [last_reliable_id="+last_reliable_id+"]");
      return STATUS_NETWORK_ERROR;
    }

    Log.i("Scoreflex", "Reliable RoomMessage message sent - id="+(last_msgid+1)+
          " [last_reliable_id="+last_reliable_id+"]");
    last_msgid++;
    snd_message_listeners.put(last_msgid, listener);
    inmsg_queue.put(last_msgid, inmsg);
    return last_msgid;
  }


  /***************************************************************************/
  private void onConnected() {
    if (connection_listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onConnected");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        connection_listener.onConnected(session_info);
      }
    });
  }

  private void onReconnecting(final int status_code) {
    if (connection_listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onReconnecting");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        connection_listener.onReconnecting(status_code);
      }
    });
  }

  private void onConnectionFailed(final int status_code) {
    if (connection_listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onConnectionFailed");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        connection_listener.onConnectionFailed(status_code);
      }
    });
  }

  private void onConnectionClosed(final int status_code) {
    if (connection_listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onConnectionClosed");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        connection_listener.onConnectionClosed(status_code);
      }
    });
  }

  private void onPong(final PingListener listener,
                      final long latency) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onPong");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onPong(latency);
      }
    });
  }

  private void onPang(final PingListener listener) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onPang");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onPang();
      }
    });
  }

  private void onRoomCreated(final RoomListener listener,
                             final int status_code,
                             final Room room) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onRoomCreated");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onRoomCreated(status_code, room);
      }
    });
  }

  private void onRoomClosed(final RoomListener listener,
                            final int status_code,
                            final String room_id) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onRoomClosed");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onRoomClosed(status_code, room_id);
      }
    });
  }

  private void onRoomJoined(final RoomListener listener,
                            final int status_code,
                            final Room room) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onRoomJoined");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onRoomJoined(status_code, room);
      }
    });
  }

  private void onRoomLeft(final RoomListener listener,
                          final int status_code,
                          final String room_id) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onRoomLeft");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onRoomLeft(status_code, room_id);
      }
    });
  }

  private void onPeerJoined(final RoomListener listener,
                            final Room room,
                            final Participant peer) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onPeerJoined");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onPeerJoined(room, peer);
      }
    });
  }

  private void onPeerLeft(final RoomListener listener,
                          final Room room,
                          final String peer_id) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onPeerLeft");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onPeerLeft(room, peer_id);
      }
    });
  }

  private void onMatchStateChanged(final RoomListener listener,
                                   final int status_code,
                                   final Room room,
                                   final MatchState state) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onMatchStateChanged");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onMatchStateChanged(status_code, room, state);
      }
    });
  }

  private void onRoomPropertyChanged(final RoomListener listener,
                                     final int status_code,
                                     final Room room,
                                     final String from,
                                     final String name) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onRoomPropertyChanged");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onRoomPropertyChanged(status_code, room, from, name);
      }
    });
  }

  private void onParticipantPropertyChanged(final RoomListener listener,
                                            final int status_code,
                                            final Room room,
                                            final String peer_id,
                                            final String name) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onParticipantPropertyChanged");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onParticipantPropertyChanged(status_code, room, peer_id, name);
      }
    });
  }

  private void onRealTimeMessageReceived(final MessageReceivedListener listener,
                                         final Message msg) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onMessageReceived");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onMessageReceived(msg);
      }
    });
  }

  private void onRealTimeMessageSent(final MessageSentListener listener,
                                     final int status_code,
                                     final int msg_id) {
    if (listener == null)
      return;

    Log.i("Scoreflex", "[thread-id="+Thread.currentThread().getId()+"] " +
          "onMessageSent");

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onMessageSent(status_code, msg_id);
      }
    });
  }

  /***************************************************************************/

  protected void onMessageReceived(UDPConnection from, Proto.OutMessage msg) {
    if (from != this.udp_connection)
      return;
    onMessageReceived(msg);
  }
  protected void onMessageReceived(TCPConnection from, Proto.OutMessage msg) {
    if (from != this.connection)
      return;
    onMessageReceived(msg);
  }

  private void onMessageReceived(Proto.OutMessage msg) {
    ackReliableMessages(msg.getAckid());

    if (msg.getMsgid() == 0) {
      handleOutMessage(msg);
    }
    else if (!msg.getIsReliable()) {
      onUnreliableMessageReceived(msg);
    }
    else {
      onReliableMessageReceived(msg);
    }
  }

  private void ackReliableMessages(int ackid) {
    for (; last_ackid <= ackid; last_ackid++) {
      Log.i("Scoreflex", "Ack reliable msg "+last_ackid);
      inmsg_queue.remove(last_ackid);
    }
  }

  private void onReliableMessageReceived(Proto.OutMessage msg) {
    int msgid = msg.getMsgid();

    if (msgid <= last_reliable_id) {
      Log.i("Scoreflex", "Drop reliable msg "+msgid+
            " [last_reliable_id="+last_reliable_id+"]");
    }
    else if (msgid > last_reliable_id + 1) {
      Log.i("Scoreflex", "Queue reliable msg "+msgid+
            " [last_reliable_id="+last_reliable_id+"]");
      outmsg_queue.put(msgid, msg);
    }
    else {
      Log.i("Scoreflex", "Handle reliable msg "+msgid+
            " [last_reliable_id="+last_reliable_id+"]");
      last_reliable_id = msgid;
      handleOutMessage(msg);

      while (!outmsg_queue.isEmpty()) {
        Proto.OutMessage nextmsg = outmsg_queue.remove(last_reliable_id + 1);
        if (nextmsg == null)
          break;
        Log.i("Scoreflex", "Handle queued reliable msg "+nextmsg.getMsgid()+
              " [last_reliable_id="+last_reliable_id+"]");
        last_reliable_id = nextmsg.getMsgid();
        handleOutMessage(nextmsg);
      }
    }
  }

  private void onUnreliableMessageReceived(Proto.OutMessage msg) {
    int msgid = msg.getMsgid();

    if (msgid <= last_unreliable_id) {
      Log.i("Scoreflex", "Drop unreliable msg "+msgid+
            " [last_unreliable_id="+last_unreliable_id+"]");
    }
    else {
      Log.i("Scoreflex", "Handle unreliable msg "+msgid+
            " [last_unreliable_id="+last_unreliable_id+"]");
      last_unreliable_id = msgid;
      handleOutMessage(msg);
    }
  }

  private void handleOutMessage(Proto.OutMessage msg) {
    switch (msg.getType()) {
      case CONNECTED:
        handleOutMessage(msg.getConnected());
        break;
      case CONNECTION_FAILED:
        handleOutMessage(msg.getConnectionFailed());
        break;
      case CONNECTION_CLOSED:
        handleOutMessage(msg.getConnectionClosed());
        break;
      case SYNC:
        handleOutMessage(msg.getSync());
        break;
      case PING:
        handleOutMessage(msg.getPing());
        break;
      case PONG:
        handleOutMessage(msg.getPong());
        break;
      case ROOM_CREATED:
        handleOutMessage(msg.getRoomCreated());
        break;
      case ROOM_CLOSED:
        handleOutMessage(msg.getRoomClosed());
        break;
      case ROOM_JOINED:
        handleOutMessage(msg.getRoomJoined());
        break;
      case ROOM_LEFT:
        handleOutMessage(msg.getRoomLeft());
        break;
      case PEER_JOINED_ROOM:
        handleOutMessage(msg.getPeerJoinedRoom());
        break;
      case PEER_LEFT_ROOM:
        handleOutMessage(msg.getPeerLeftRoom());
        break;
      case GAME_STATE_CHANGED:
        handleOutMessage(msg.getGameStateChanged());
        break;
      case ROOM_PROPERTY_UPDATED:
        handleOutMessage(msg.getRoomPropertyUpdated());
        break;
      case PLAYER_PROPERTY_UPDATED:
        handleOutMessage(msg.getPlayerPropertyUpdated());
        break;
      case ROOM_MESSAGE:
        handleOutMessage(msg.getRoomMessage());
        break;
      case ACK:
        handleOutMessage(msg.getAck());
        break;
      default:
        Log.e("Scoreflex", "Unhandled outgoing message: "+msg.getType());
    }
  }
  private void handleOutMessage(Proto.Connected msg) {
    Map<String, Object> info = protoMapToJavaMap(msg.getInfoList());

    connection_status    = ConnectionState.CONNECTED;
    retries              = 0;
    session_id           = msg.getSessionId();
    session_info         = Collections.unmodifiableMap(info);
    mm_time              = msg.getMmTime();
    mm_clock_last_update = getMonotonicTime();

    Log.i("Scoreflex", "Client connected - mm_time="+(int)getMmTime());
    onConnected();

    int udp_port = msg.getUdpPort();
    if (udp_port != 0) {
      try {
        udp_connection = new UDPConnection(this, host, udp_port);
        udp_connection.connect();
        Log.i("Scoreflex", "UDP connection created");
      }
      catch (Exception e) {
        udp_connection = null;
      }
    }

    for (Map.Entry<Integer, Proto.InMessage> entry: inmsg_queue.entrySet()) {
      Proto.InMessage inmsg = Proto.InMessage.newBuilder()
        .mergeFrom(entry.getValue())
        .setAckid(last_reliable_id)
        .build();
      connection.sendMessage(inmsg);
    }
  }
  private void handleOutMessage(Proto.ConnectionFailed msg) {
    if (udp_connection != null) {
      udp_connection.disconnect();
      udp_connection = null;
    }

    connection_status = ConnectionState.DISCONNECTED;
    connection        = null;
    session_info      = null;
    ping_listeners.clear();


    switch(msg.getStatus()) {
      case INTERNAL_ERROR:
        retries = 0;
        onConnectionFailed(STATUS_INTERNAL_ERROR);
        break;
      case INVALID_MESSAGE:
        retries = 0;
        onConnectionFailed(STATUS_INVALID_MESSAGE);
        break;
      case PROTOCOL_ERROR:
        retries = 0;
        onConnectionFailed(STATUS_PROTOCOL_ERROR);
        break;
      case NETWORK_ERROR:
      case CONNECT_TIMEOUT:
        if (reconnect_flag) {
          onReconnecting(STATUS_NETWORK_ERROR);
          reconnectSession();
        }
        else {
          retries = 0;
          onConnectionFailed(STATUS_NETWORK_ERROR);
        }
        break;
      case PERMISSION_DENIED:
        retries = 0;
        onConnectionFailed(STATUS_PERMISSION_DENIED);
        break;
      case ALREADY_CONNECTED:
        retries = 0;
        onConnectionFailed(STATUS_ALREADY_CONNECTED);
        break;
    }
  }
  private void handleOutMessage(Proto.ConnectionClosed msg) {
    if (udp_connection != null) {
      udp_connection.disconnect();
      udp_connection = null;
    }

    switch(msg.getStatus()) {
      case SESSION_CLOSED:
        connection         = null;
        connection_status  = ConnectionState.DISCONNECTED;
        session_id         = null;
        session_info       = null;
        retries            = 0;
        last_msgid         = 0;
        last_ackid         = 0;
        last_unreliable_id = 0;
        last_reliable_id   = 0;
        synchronized (this) { current_room = null; }
        inmsg_queue.clear();
        outmsg_queue.clear();
        ping_listeners.clear();
        room_listeners.clear();
        rcv_message_listeners.clear();
        snd_message_listeners.clear();

        onConnectionClosed(STATUS_SESSION_CLOSED);
        break;
      case REPLACED_BY_NEW_CONNECTION:
        connection        = null;
        connection_status = ConnectionState.DISCONNECTED;
        session_info      = null;
        retries           = 0;
        ping_listeners.clear();

        onConnectionClosed(STATUS_REPLACED_BY_NEW_CONNECTION);
        break;
      case UNRESPONSIVE_CLIENT:
        connection        = null;
        connection_status = ConnectionState.DISCONNECTED;
        session_info      = null;
        ping_listeners.clear();

        if (reconnect_flag) {
          onReconnecting(STATUS_NETWORK_ERROR);
          reconnectSession();
        }
        else {
          retries = 0;
          onConnectionFailed(STATUS_NETWORK_ERROR);
        }
        break;
      case NEW_SERVER_LOCATION:
        host    = msg.getHostname();
        port    = msg.getPort();
        retries = 0;
        ping_listeners.clear();

        if (isSessionConnected()) {
          onReconnecting(STATUS_NEW_SERVER_LOCATION);
        }
        doConnect();
        break;
    }
  }
  private void handleOutMessage(Proto.Sync msg) {
    mm_latency            = msg.getLatency();
    mm_clock_last_update -= mm_latency;

    Log.i("Scoreflex", "Client synchronized - mm_time="+(int)getMmTime());
  }
  private void handleOutMessage(Proto.Ping msg) {
    mm_time              = msg.getTimestamp();
    mm_clock_last_update = getMonotonicTime();

    Proto.Pong reply = Proto.Pong.newBuilder()
      .setId(msg.getId())
      .setTimestamp(msg.getTimestamp())
      .build();

    Proto.InMessage inmsg = InMessageBuilder.build((int)getMmTime(),
                                                   last_reliable_id,
                                                   false, reply);
    if (udp_connection != null && udp_connection.isConnected()) {
      udp_connection.sendMessage(inmsg);
    }
    else {
      connection.sendMessage(inmsg);
    }
  }
  private void handleOutMessage(Proto.Pong msg) {
    onPong(ping_listeners.remove(msg.getId()), getMmTime() - (long)msg.getTimestamp());
  }
  private void handleOutMessage(Proto.RoomCreated msg) {
    Proto.Room r = msg.getRoom();

    switch (msg.getStatus()) {
      case SUCCESS:
        Map<String, Participant> participants = new HashMap<String, Participant>();

        for (Proto.Player entry: r.getPlayersList()) {
          Map<String, Object> props = protoMapToJavaMap(entry.getPropertiesList());
          participants.put(entry.getClientId(),
                           new Participant(entry.getClientId(), r.getRoomId(),
                                           props));
        }

        MatchState state;
        switch (r.getGameState()) {
          case PENDING:
            state = MatchState.PENDING;
            break;
          case READY:
            state = MatchState.READY;
            break;
          case RUNNING:
            state = MatchState.RUNNING;
            break;
          case FINISHED:
            state = MatchState.FINISHED;
            break;
          default:
            state = MatchState.PENDING;
            break;
        }

        current_room = Room.builder()
          .setId(r.getRoomId())
          .setSession(this)
          .setMatchState(state)
          .setConfig(protoMapToJavaMap(r.getConfigList()))
          .setProperties(protoMapToJavaMap(r.getPropertiesList()))
          .setParticipants(participants)
          .build();

        onRoomCreated(room_listeners.get(r.getRoomId()),
                      STATUS_SUCCESS, current_room);
        break;

      case INTERNAL_ERROR:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomCreated(room_listeners.remove(r.getRoomId()),
                      STATUS_INTERNAL_ERROR, null);
        break;

      case PERMISSION_DENIED:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomCreated(room_listeners.remove(r.getRoomId()),
                      STATUS_PERMISSION_DENIED, null);
        break;

      case ALREADY_CREATED:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomCreated(room_listeners.remove(r.getRoomId()),
                      STATUS_ROOM_ALREADY_CREATED, null);
        break;

      case INVALID_DATA:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomCreated(room_listeners.remove(r.getRoomId()),
                      STATUS_INVALID_DATA, null);
        break;
    }
  }
  private void handleOutMessage(Proto.RoomClosed msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    synchronized (this) { current_room = null; }
    rcv_message_listeners.remove(msg.getRoomId());

    switch (msg.getStatus()) {
      case INTERNAL_ERROR:
        onRoomClosed(room_listeners.remove(msg.getRoomId()),
                     STATUS_INTERNAL_ERROR, msg.getRoomId());
        break;

      case ROOM_CLOSED:
        onRoomClosed(room_listeners.remove(msg.getRoomId()),
                     STATUS_ROOM_CLOSED, msg.getRoomId());
        break;
    }
  }
  private void handleOutMessage(Proto.RoomJoined msg) {
    Proto.Room r = msg.getRoom();

    switch (msg.getStatus()) {
      case SUCCESS:
        Map<String, Participant> participants = new HashMap<String, Participant>();

        for (Proto.Player entry: r.getPlayersList()) {
          Map<String, Object> props = protoMapToJavaMap(entry.getPropertiesList());
          participants.put(entry.getClientId(),
                           new Participant(entry.getClientId(), r.getRoomId(),
                                           props));
        }

        MatchState state;
        switch (r.getGameState()) {
          case PENDING:
            state = MatchState.PENDING;
            break;
          case READY:
            state = MatchState.READY;
            break;
          case RUNNING:
            state = MatchState.RUNNING;
            break;
          case FINISHED:
            state = MatchState.FINISHED;
            break;
          default:
            state = MatchState.PENDING;
            break;
        }

        synchronized (this) {
          current_room = Room.builder()
            .setId(r.getRoomId())
            .setSession(this)
            .setMatchState(state)
            .setConfig(protoMapToJavaMap(r.getConfigList()))
            .setProperties(protoMapToJavaMap(r.getPropertiesList()))
            .setParticipants(participants)
            .build();
        }

        onRoomJoined(room_listeners.get(r.getRoomId()),
                     STATUS_SUCCESS, current_room);
        break;

      case INTERNAL_ERROR:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomJoined(room_listeners.remove(r.getRoomId()),
                     STATUS_INTERNAL_ERROR, null);
        break;

      case PERMISSION_DENIED:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomJoined(room_listeners.remove(r.getRoomId()),
                     STATUS_PERMISSION_DENIED, null);
        break;

      case ROOM_NOT_FOUND:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomJoined(room_listeners.remove(r.getRoomId()),
                     STATUS_ROOM_NOT_FOUND, null);
        break;

      case ROOM_FULL:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomJoined(room_listeners.remove(r.getRoomId()),
                     STATUS_ROOM_FULL, null);
        break;
    }
  }
  private void handleOutMessage(Proto.RoomLeft msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    synchronized (this) { current_room = null; }
    rcv_message_listeners.remove(msg.getRoomId());

    switch (msg.getStatus()) {
      case SUCCESS:
        onRoomLeft(room_listeners.remove(msg.getRoomId()),
                   STATUS_SUCCESS, msg.getRoomId());
        break;

      case ROOM_NOT_JOINED:
        onRoomLeft(room_listeners.remove(msg.getRoomId()),
                   STATUS_SUCCESS, msg.getRoomId());
        break;

      case INTERNAL_ERROR:
        onRoomLeft(room_listeners.remove(msg.getRoomId()),
                   STATUS_INTERNAL_ERROR, msg.getRoomId());
        break;
    }
  }
  private void handleOutMessage(Proto.PeerJoinedRoom msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    Proto.Player p   = msg.getPlayer();
    Participant peer = new Participant(p.getClientId(), msg.getRoomId(),
                                       protoMapToJavaMap(p.getPropertiesList()));
    current_room.addParticipant(peer);

    onPeerJoined(room_listeners.get(current_room.getId()), current_room, peer);
  }
  private void handleOutMessage(Proto.PeerLeftRoom msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    current_room.removeParticipant(msg.getClientId());
    onPeerLeft(room_listeners.get(current_room.getId()), current_room,
               msg.getClientId());
  }
  private void handleOutMessage(Proto.GameStateChanged msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    switch (msg.getStatus()) {
      case SUCCESS:
        MatchState state;
        switch (msg.getGameState()) {
          case PENDING:
            state = MatchState.PENDING;
            break;
          case READY:
            state = MatchState.READY;
            break;
          case RUNNING:
            state = MatchState.RUNNING;
            break;
          case FINISHED:
            state = MatchState.FINISHED;
            break;
          default:
            state = current_room.getMatchState();
            break;
        }
        current_room.setMatchState(state);
        onMatchStateChanged(room_listeners.get(current_room.getId()),
                            STATUS_SUCCESS, current_room, state);
        break;

      case ROOM_NOT_JOINED:
        onMatchStateChanged(room_listeners.get(current_room.getId()),
                            STATUS_ROOM_NOT_JOINED, current_room,
                           MatchState.UNKNOWN);
        break;

      case BAD_STATE:
        onMatchStateChanged(room_listeners.get(current_room.getId()),
                            STATUS_BAD_STATE, current_room,
                            MatchState.UNKNOWN);
        break;
    }
  }
  private void handleOutMessage(Proto.RoomPropertyUpdated msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    String name  = msg.getProperty().getName();
    Object value = mapEntrytoObject(msg.getProperty());

    switch (msg.getStatus()) {
      case SUCCESS:
        if (value == null)
          current_room.removeProperty(name);
        else
          current_room.addProperty(name, value);
        onRoomPropertyChanged(room_listeners.get(current_room.getId()),
                              STATUS_SUCCESS, current_room,
                              msg.getClientId(), name);
        break;
      case ROOM_NOT_JOINED:
        onRoomPropertyChanged(room_listeners.get(current_room.getId()),
                              STATUS_ROOM_NOT_JOINED, current_room,
                              Scoreflex.getPlayerId(), name);
        break;
    }
  }
  private void handleOutMessage(Proto.PlayerPropertyUpdated msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    String name  = msg.getProperty().getName();
    Object value = mapEntrytoObject(msg.getProperty());

    switch (msg.getStatus()) {
      case SUCCESS:
        if (value == null)
          current_room.removeParticipantProperty(msg.getClientId(), name);
        else
          current_room.addParticipantProperty(msg.getClientId(), name, value);
        onParticipantPropertyChanged(room_listeners.get(current_room.getId()),
                                     STATUS_SUCCESS, current_room,
                                     msg.getClientId(), name);
        break;
      case ROOM_NOT_JOINED:
        onParticipantPropertyChanged(room_listeners.get(current_room.getId()),
                                     STATUS_ROOM_NOT_JOINED, current_room,
                                     Scoreflex.getPlayerId(), name);
        break;
    }
  }
  private void handleOutMessage(Proto.RoomMessage msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    Message message = Message.builder()
      .setRoomId(current_room.getId())
      .setSenderId(msg.getFromId())
      .setTag(msg.getTag())
      .setPayload(protoMapToJavaMap(msg.getPayloadList()))
      .build();

    onRealTimeMessageReceived(rcv_message_listeners.get(current_room.getId()),
                              message);
  }
  private void handleOutMessage(Proto.Ack msg) {
    int status;
    switch (msg.getStatus()) {
      case SUCCESS:
        status = STATUS_SUCCESS;
        break;
      case INTERNAL_ERROR:
        status = STATUS_INTERNAL_ERROR;
        break;
      case ROOM_NOT_JOINED:
        status = STATUS_ROOM_NOT_JOINED;
        break;
      case PEER_NOT_FOUND:
        status = STATUS_PEER_NOT_FOUND;
        break;
      case INVALID_MESSAGE:
        status = STATUS_INVALID_MESSAGE;
        break;
      default:
        status = STATUS_SUCCESS;
    }
    onRealTimeMessageSent(snd_message_listeners.remove(msg.getMsgid()),
                          status, msg.getMsgid());
  }

  /***************************************************************************/
  private static Map<String, Object> protoMapToJavaMap(List<Proto.MapEntry> pmap) {
    HashMap<String, Object> jmap = new HashMap<String, Object>();

    for (Proto.MapEntry entry: pmap) {
      Object value = mapEntrytoObject(entry);
      if (value != null)
        jmap.put(entry.getName(), value);
    }
    return jmap;
  }

  private static Object mapEntrytoObject(Proto.MapEntry entry) {
    switch (entry.getType()) {
      case VOID:
        return null;
      case INT32:
        return new Integer(entry.getInt32Val());
      case UINT32:
        return new Integer(entry.getUint32Val());
      case SINT32:
        return new Integer(entry.getSint32Val());
      case INT64:
        return new Long(entry.getInt64Val());
      case UINT64:
        return new Long(entry.getSint64Val());
      case SINT64:
        return new Long(entry.getSint64Val());
      case DOUBLE:
        return new Double(entry.getDoubleVal());
      case BOOL:
        return new Boolean(entry.getBoolVal());
      case STRING:
        return entry.getStringVal();
      case BYTES:
        return ByteBuffer.wrap(entry.getBytesVal().toByteArray());
      default:
        return null;
    }
  }

  private static List<Proto.MapEntry> javaMapToProtoMap(Map<String, Object> jmap) {
    List<Proto.MapEntry> pmap = new ArrayList<Proto.MapEntry>();
    if (jmap == null)
      return pmap;

    for (Map.Entry<String, Object> entry: jmap.entrySet()) {
      pmap.add(objectToMapEntry(entry.getKey(), entry.getValue()));
    }
    return pmap;
  }

  private static Proto.MapEntry objectToMapEntry(String name, Object value) {
    Proto.MapEntry.Builder entry_builder = Proto.MapEntry.newBuilder()
      .setName(name);

    if (value == null) {
      entry_builder.setType(Proto.MapEntry.Type.VOID);
    }
    else if (value instanceof Integer) {
      int i = ((Integer)value).intValue();
      if (i >= 0) {
        entry_builder.setType(Proto.MapEntry.Type.UINT32);
        entry_builder.setUint32Val(i);
      }
      else {
        entry_builder.setType(Proto.MapEntry.Type.SINT32);
        entry_builder.setSint32Val(i);
      }
    }
    else if (value instanceof Long) {
      long l = ((Long)value).longValue();
      if (l >= 0) {
        entry_builder.setType(Proto.MapEntry.Type.UINT64);
        entry_builder.setUint64Val(l);
      }
      else {
        entry_builder.setType(Proto.MapEntry.Type.SINT64);
        entry_builder.setSint64Val(l);
      }
    }
    else if (value instanceof Double) {
      entry_builder.setType(Proto.MapEntry.Type.DOUBLE);
      entry_builder.setDoubleVal((Double)value);
    }
    else if (value instanceof Boolean) {
      entry_builder.setType(Proto.MapEntry.Type.BOOL);
      entry_builder.setBoolVal((Boolean)value);
    }
    else if (value instanceof String) {
      entry_builder.setType(Proto.MapEntry.Type.STRING);
      entry_builder.setStringVal((String)value);
    }
    else if (value instanceof ByteBuffer) {
      entry_builder.setType(Proto.MapEntry.Type.BYTES);
      entry_builder.setBytesVal(
        com.google.protobuf.ByteString.copyFrom((ByteBuffer)value)
      );
    }
    else {
      entry_builder.setType(Proto.MapEntry.Type.VOID);
    }

    return entry_builder.build();
  }


  /***************************************************************************/
  private void reconnectSession() {
    local_handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (connection == null) {
          retries++;
          doConnect();
        }
      }
    }, reconnect_timeout);
  }

  private long getMonotonicTime() {
    return SystemClock.uptimeMillis();
  }

  private long getMmTime() {
    return (getMonotonicTime() - mm_clock_last_update + mm_time);
  }

  private boolean isSessionConnected() {
    return (connection_status == ConnectionState.CONNECTED);
  }


  /***************************************************************************/
  private static class InMessageBuilder {
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.Connect message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.CONNECT)
        .setConnect(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.Disconnect message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.DISCONNECT)
        .setDisconnect(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.Ping message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.PING)
        .setPing(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.Pong message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.PONG)
        .setPong(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.CreateRoom message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.CREATE_ROOM)
        .setCreateRoom(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.JoinRoom message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.JOIN_ROOM)
        .setJoinRoom(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.LeaveRoom message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.LEAVE_ROOM)
        .setLeaveRoom(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.StartGame message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.START_GAME)
        .setStartGame(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.StopGame message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.STOP_GAME)
        .setStopGame(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.ResetGame message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.RESET_GAME)
        .setResetGame(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.SetRoomProperty message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.SET_ROOM_PROPERTY)
        .setSetRoomProperty(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.SetPlayerProperty message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.SET_PLAYER_PROPERTY)
        .setSetPlayerProperty(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.RoomMessage message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.ROOM_MESSAGE)
        .setRoomMessage(message)
        .build();
    }
  }
}
