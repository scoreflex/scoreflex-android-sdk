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
  private static Session session = new Session();

  private Handler               main_handler;
  private Handler               local_handler;
  private Boolean               is_initialized = false;
  private Boolean               is_started     = false;

  private ConnectionListener    connection_listener;
  private ConnectionState       connection_status;
  private TCPConnection         connection;
  private UDPConnection         udp_connection;
  private String                host;
  private int                   port;
  private boolean               reconnect_flag;
  private int                   reconnect_timeout;
  private int                   max_retries;
  private int                   retries;

  private String                session_id;
  private RealtimeMap           session_info;
  private Room                  current_room;
  private int                   mm_time;
  private int                   mm_latency;
  private long                  mm_clock_last_update;
  private int                   last_msgid;
  private int                   last_ackid;
  private int                   last_reliable_id;
  private Map<Integer, Integer> last_unreliable_ids;

  private int                   tcp_heartbeat_timeout = 200;
  private int                   udp_heartbeat_timeout = 200;

  private Map<Integer, Proto.InMessage>   inmsg_queue;
  private Map<Integer, Proto.OutMessage>  outmsg_queue;

  private Map<Integer, PingListener>             ping_listeners;
  private Map<String,  RoomListener>             room_listeners;
  private Map<String,  MessageReceivedListener>  rcv_message_listeners;
  private Map<Integer, MessageSentListener>      snd_message_listeners;

  /**
   * The maximum serialized size allowed for a payload in the unreliable
   * messages.
   *
   * @see RealtimeMap#getSerializedSize
   */
  public static final int MAX_UNRELIABLE_PAYLOAD_SIZE = 1300;

  /**
   * The maximum serialized size allowed for a payload in the reliable messages.
   *
   * @see RealtimeMap#getSerializedSize
   */
  public static final int MAX_RELIABLE_PAYLOAD_SIZE = 2048;

  /**
   * The maximum serliazed size allowed for the room's property list.
   *
   * @see RealtimeMap#getSerializedSize
   */
  public static final int MAX_ROOM_PROPERTIES_SIZE = 1500;

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
   * The status code used in {@link ConnectionListener#onConnectionClosed} when
   * the player's session is closed on the server side.
   */
  public static final int STATUS_SESSION_CLOSED             =  3;

  /**
   * The status code used used in {@link ConnectionListener#onConnectionClosed}
   * when the current connection is closed by a new one.
   */
  public static final int STATUS_REPLACED_BY_NEW_CONNECTION =  4;

  /**
   * The status code used in {@link ConnectionListener#onReconnecting} when the
   *   server requests the client to reconnect on a specific host.
   */
  public static final int STATUS_NEW_SERVER_LOCATION        =  5;

  /**
   * The status code used in {@link ConnectionListener#onConnectionFailed} and
   * {@link MessageSentListener#onMessageSent} when a malformed message is sent
   * by the player.
   */
  public static final int STATUS_INVALID_MESSAGE            =  6;

  /**
   * The status code used in {@link ConnectionListener#onConnectionFailed} when
   * an unknown message was sent to the server.
   */
  public static final int STATUS_PROTOCOL_ERROR             =  7;

  /**
   * The status code used in callbacks when the player does not have permissions
   * to perform an operation.
   */
  public static final int STATUS_PERMISSION_DENIED          =  8;

  /**
   * The status code used in {@link ConnectionListener#onConnectionFailed} when
   * the player has already a opened session on another device.
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
   * The status code used in {@link RoomListener#onRoomCreated} when the player
   * attempts to create a room with the same ID than an existing one.
   */
  public static final int STATUS_ROOM_ALREADY_CREATED       = 12;

  /**
   * The status code used {@link RoomListener#onRoomClosed} when the the room is
   * closed normally by an external way.
   */
  public static final int STATUS_ROOM_CLOSED                = 13;

  /**
   * The status code used {@link RoomListener#onRoomJoined} when the player
   * attempts to join a unknown room.
   */
  public static final int STATUS_ROOM_NOT_FOUND             = 14;

  /**
   * The status code used in {@link RoomListener#onRoomJoined} when the player
   * attempts to join a room which the maximum number of participants allowed
   * was reached.
   */
  public static final int STATUS_ROOM_FULL                  = 15;

  /**
   * The status code used in {@link RoomListener#onRoomJoined} when the player
   * attempts to join a room during a running match whereas the drpop-in-match
   * option is disabled.
   */
  public static final int STATUS_NO_DROP_IN_MATCH           = 16;

  /**
   * The status code used in {@link RoomListener#onRoomCreated} when the player
   * uses an invalid configuration to create a room.
   */
  public static final int STATUS_INVALID_DATA               = 17;

  /**
   * The status code used in {@link RoomListener#onMatchStateChanged} when the
   * player attempts to do an invalid change of the match's state.
   */
  public static final int STATUS_BAD_STATE                  = 18;

  /**
   * The status code used in {@link MessageSentListener#onMessageSent} when the
   * player attempts to send a reliable message to a unknown participant.
   */
  public static final int STATUS_PEER_NOT_FOUND             = 19;

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
      session.last_unreliable_ids = new HashMap<Integer, Integer>();;

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
   * Retrieves the server address currently used to connect.
   *
   * @return the server address  currently used.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static String getServerAddr() {
    checkInstance();
    return session.host;
  }

  /**
   * Retrieves the current port number on which the realtime service is listening for
   * incoming requests.
   *
   * @return the server port currently used.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static int getServerPort() {
    checkInstance();
    return session.port;
  }

  /**
   * Sets the value of the reconnect flag. By setting it to <code>true</code>,
   * the session will be automatically reconnected when an error occurs. The
   * reconnect attempts will be made with a configurable delay. After a number
   * of consecutive reconnection failures, an error is notified.
   * <br><br>
   * Default value: <code>true</code>.
   *
   * @see ConnectionListener#onReconnecting
   * @see ConnectionListener#onConnectionFailed
   * @see #setMaxRetries
   * @see #setReconnectTimeout
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
   * @see #setReconnectFlag
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
   * @see #setReconnectFlag
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
   * @return A {@link RealtimeMap} representing the session's information. If
   * the session is not connected, this method returns <code>null</code>.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   */
  public static RealtimeMap getSessionInfo() {
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
   * ConnectionListener#onConnected} or {@link
   * ConnectionListener#onConnectionFailed} depending of the operation's result.
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
   * @throws IllegalArgumentException if the listener is <code>null</code>
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
      .setPlayerId(Scoreflex.getPlayerId())
      .setGameId(Scoreflex.getClientId())
      .setAccessToken(Scoreflex.getAccessToken());
    if (session_id != null) {
      builder.setSessionId(session_id);
    }

    Proto.Connect msg = builder.build();
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
    if (udp_connection != null) {
      udp_connection.disconnect();
      udp_connection = null;
    }

    if (isSessionConnected()) {
      Proto.Disconnect message = Proto.Disconnect.newBuilder().build();
      connection.disconnect(InMessageBuilder.build(0, 0, true, message));
    }

    connection_status = ConnectionState.DISCONNECTED;
    connection        = null;
    udp_connection    = null;
    session_id        = null;
    session_info      = null;
    last_msgid        = 0;
    last_ackid        = 0;
    last_reliable_id  = 0;
    last_unreliable_ids.clear();
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
   * timeout is reached, {@link PingListener#onPong} is called. If the requests
   * timed out, {@link PingListener#onPang} is called.
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
      onPang(listener);
      return;
    }

    ping_listeners.put(id, listener);
    local_handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        onPang(ping_listeners.remove(id));
      }
    }, timeout);
  }


  /**
   * Same as {@link #createRoom(String, RoomConfig, RealtimeMap)
   * createRoom(id, config, null)}.
   */
  public static void createRoom(String id, RoomConfig config) {
    createRoom(id, config, null);
  }
  /**
   * Creates a realtime room. The result of this operation will be notified by
   * the callback {@link RoomListener#onRoomCreated} to the given {@link
   * RoomListener} in {@link RoomConfig}.
   *
   * @param id The room's ID.
   * @param config The room's configuration.
   * @param room_props The room's properties.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   * @throws IllegalArgumentException if the room's id or the room's
   * configuration are <code>null</code>
   * @throws IllegalArgumentException if the serialized size of the room's
   * property list exceeds {@link #MAX_ROOM_PROPERTIES_SIZE}.
   */
  public static void createRoom(final String id, final RoomConfig config,
                                final RealtimeMap room_props) {
    checkInstance();
    if (id == null)
      throw new IllegalArgumentException("Room id cannot be null");
    if (config == null)
      throw new IllegalArgumentException("Room configuration cannot be null");
    if (room_props != null && room_props.getSerializedSize() > MAX_ROOM_PROPERTIES_SIZE)
      throw new IllegalArgumentException("Serialized size of the room's properties exceeds MAX_ROOM_PROPERTIES_SIZE");

    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.sendCreateRoom(id, config, room_props);
      }
    });
  }
  private void sendCreateRoom(String id, RoomConfig config,
                              RealtimeMap room_props) {
    if (!isConnected()) {
      onRoomCreated(config.getRoomListener(), STATUS_SESSION_NOT_CONNECTED,
                    null);
      return;
    }

    room_listeners.put(id, config.getRoomListener());
    rcv_message_listeners.put(id, config.getMessageListener());

    Proto.CreateRoom msg = Proto.CreateRoom.newBuilder()
      .setRoomId(id)
      .addAllRoomConfig(realtimeMapToProtoMap(config.getRoomConfig()))
      .addAllRoomProperties(realtimeMapToProtoMap(room_props))
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      onRoomCreated(config.getRoomListener(), STATUS_NETWORK_ERROR, null);
      return;
    }

    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Joins a realtime room. The result of this operation will be notified by the
   * callback {@link RoomListener#onRoomJoined} to the given {@link
   * RoomListener}.
   *
   * @param id The room's ID.
   * @param room_listener The {@link RoomListener} used to notify the player of
   * room's state changes.
   * @param message_listener The {@link MessageReceivedListener} used to notify
   * the player when a message is received.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet.
   * @throws IllegalArgumentException if the one of listeners is
   * <code>null</code> or if the room's id is <code>null</code>.
   */
  public static void joinRoom(final String id, final RoomListener room_listener,
                              final MessageReceivedListener message_listener) {
    checkInstance();
    if (id == null)
      throw new IllegalArgumentException("Room id cannot be null");
    if (room_listener == null)
      throw new IllegalArgumentException("Room listener cannot be null");
    if (message_listener == null)
      throw new IllegalArgumentException("Message listener cannot be null");
    session.local_handler.post(new Runnable() {
      @Override
      public void run() {
        session.sendJoinRoom(id, room_listener, message_listener);
      }
    });
  }
  private void sendJoinRoom(String id, RoomListener room_listener,
                            MessageReceivedListener message_listener) {
    if (!isConnected()) {
      onRoomJoined(room_listener, STATUS_SESSION_NOT_CONNECTED,
                   null);
      return;
    }

    room_listeners.put(id, room_listener);
    rcv_message_listeners.put(id, message_listener);

    Proto.JoinRoom msg = Proto.JoinRoom.newBuilder()
      .setRoomId(id)
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      onRoomJoined(room_listener, STATUS_NETWORK_ERROR, null);
      return;
    }

    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Leaves the current room. The result of this operation will be notified by
   * the callback {@link RoomListener#onRoomLeft} to the given {@link
   * RoomListener} set when the player has created or joined the room.
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
      onRoomLeft(room_listener, STATUS_NETWORK_ERROR, room_id);
      return;
    }

    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets the match's state to {@link MatchState#RUNNING}. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onMatchStateChanged} to the given {@link RoomListener} set
   * when the player has created or joined the room.
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

    Proto.StartMatch msg = Proto.StartMatch.newBuilder()
      .setRoomId(room.getId())
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      onMatchStateChanged(room_listener, STATUS_NETWORK_ERROR, room,
                          MatchState.UNKNOWN);
      return;
    }

    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets the match's state to {@link MatchState#FINISHED}. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onMatchStateChanged} to the given {@link RoomListener} set
   * when the player has created or joined the room.
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

    Proto.StopMatch msg = Proto.StopMatch.newBuilder()
      .setRoomId(room.getId())
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      onMatchStateChanged(room_listener, STATUS_NETWORK_ERROR, room,
                          MatchState.UNKNOWN);
      return;
    }

    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets the match's state to {@link MatchState#PENDING}. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onMatchStateChanged} to the given {@link RoomListener} set
   * when the player has created or joined the room.
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

    Proto.ResetMatch msg = Proto.ResetMatch.newBuilder()
      .setRoomId(room.getId())
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      onMatchStateChanged(room_listener, STATUS_NETWORK_ERROR, room,
                          MatchState.UNKNOWN);
          return;
        }

        last_msgid++;
        inmsg_queue.put(last_msgid, inmsg);
  }


  /**
   * Sets or Updates a room's property given its key. The result of this
   * operation will be notified by the callback {@link
   * RoomListener#onRoomPropertyChanged} to the given {@link RoomListener} set
   * when the player has created or joined the room.
   * <br>
   * If the value is <code>null</code>, the property will be removed.
   *
   * @param key The property's key.
   * @param value The new property's value.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   * @throws IllegalArgumentException if the property's key is <code>null</code>
   * or if the value's type is inappropriate.
   * @throws IllegalArgumentException if the serialized size of the updated
   * room's properties exceeds {@link #MAX_ROOM_PROPERTIES_SIZE}.
   */
  public static void setRoomProperty(final String key, final Object value) {
    checkInstance();
    if (key == null)
      throw new IllegalArgumentException("Property key cannot be null");
    Runnable r;

    synchronized (session) {
      if (session.current_room == null)
        throw new IllegalStateException("No room is joined");

      if (value != null) {
        try {
          RealtimeMap props = session.current_room.getProperties();
          Object old_value  = props.get(key);
          int props_size    = props.getSerializedSize();
          if (old_value != null)
            props_size -= RealtimeMap.getSerializedSize(key,old_value);
          props_size += RealtimeMap.getSerializedSize(key,value);

          if (props_size > MAX_ROOM_PROPERTIES_SIZE)
            throw new IllegalArgumentException("Serialized size of the room's properties exceeds MAX_ROOM_PROPERTIES_SIZE");
        }
        catch (ClassCastException e) {
          throw new IllegalArgumentException("Invalid value type");
        }
      }


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
      onSetRoomPropertyFailed(room_listener, STATUS_SESSION_NOT_CONNECTED,
                              room, key);
      return;
    }

    Proto.SetRoomProperty msg = Proto.SetRoomProperty.newBuilder()
      .setRoomId(room.getId())
      .setProperty(objectToMapEntry(key, value))
      .build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      onSetRoomPropertyFailed(room_listener, STATUS_NETWORK_ERROR, room, key);
      return;
    }

    last_msgid++;
    inmsg_queue.put(last_msgid, inmsg);
  }

  /**
   * Same as {@link #sendUnreliableMessage(String, byte, RealtimeMap)
   * sendUnreliableMessage(null, 0, payload)}.
   */
  public static int sendUnreliableMessage(RealtimeMap payload) {
    return sendUnreliableMessage(null, (byte)0, payload);
  }
  /**
   * Same as {@link #sendUnreliableMessage(String, byte, RealtimeMap)
   * sendUnreliableMessage(peer_id, 0, payload)}.
   */
  public static int sendUnreliableMessage(String peer_id, RealtimeMap payload) {
    return sendUnreliableMessage(peer_id, (byte)0, payload);
  }
  /**
   * Same as {@link #sendUnreliableMessage(String, byte, RealtimeMap)
   * sendUnreliableMessage(null, tag, payload)}.
   */
  public static int sendUnreliableMessage(byte tag, RealtimeMap payload) {
    return sendUnreliableMessage(null, tag, payload);
  }
  /**
   * Sends a unreliable message to a participant in the room. If
   * <code>peer_id</code> is <code>null</code>, the message will be broadcasted
   * to all participants in the room. The maximum payload size supported, once
   * serialized, is {@link #MAX_UNRELIABLE_PAYLOAD_SIZE} bytes.
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
   * @throws IllegalArgumentException if the </code>peer_id</code> is the
   * current player.
   * @throws IllegalArgumentException if the serialized size of the payload
   * exceeds {@link #MAX_UNRELIABLE_PAYLOAD_SIZE}.
   */
  public static int sendUnreliableMessage(final String peer_id, final byte tag,
                                          final RealtimeMap payload) {
    checkInstance();
    FutureTask<Integer> t;

    if (peer_id != null && peer_id.equals(Scoreflex.getPlayerId()))
      throw new IllegalArgumentException("Invalid participant's id");
    if (payload.getSerializedSize() > MAX_UNRELIABLE_PAYLOAD_SIZE)
      throw new IllegalArgumentException("Serialized size of the payload exceeds MAX_UNRELIABLE_PAYLOAD_SIZE");

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
      return STATUS_INTERNAL_ERROR;
    }
  }
  private int sendUnreliableMessage(String room_id, String peer_id,
                                    byte tag, RealtimeMap payload) {
    if (!isSessionConnected()) {
      return STATUS_SESSION_NOT_CONNECTED;
    }

    int msgid = (int)getMmTime();
    Proto.RoomMessage.Builder builder = Proto.RoomMessage.newBuilder()
      .setRoomId(room_id)
      .setTimestamp(msgid)
      .setTag(tag)
      .setIsReliable(false)
      .addAllPayload(realtimeMapToProtoMap(payload));
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
      return STATUS_NETWORK_ERROR;
    }

    return STATUS_SUCCESS;
  }


  /**
   * Same as {@link #sendReliableMessage(MessageSentListener, String, byte,
   * RealtimeMap) sendReliableMessage(listener, peer_id, 0, payload)}.
   */
  public static int sendReliableMessage(MessageSentListener listener,
                                        String peer_id,
                                        RealtimeMap payload) {
    return sendReliableMessage(listener, peer_id, (byte)0, payload);
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
   * {@link MessageSentListener#onMessageSent} or {@link
   * #STATUS_SESSION_NOT_CONNECTED} if the player's session is not connected on
   * the service, {@link #STATUS_NETWORK_ERROR} if a network error occurs,
   * {@link #STATUS_INVALID_DATA} if an unexpected error occurs.
   *
   * @throws IllegalStateException if the realtime session is not initialized
   * yet or if no room is joined.
   * @throws IllegalArgumentException if the listener is <code>null</code>.
   * @throws IllegalArgumentException if the </code>peer_id</code> is
   * <code>null</code> or is the current player.
   * @throws IllegalArgumentException if the serialized size of the payload
   * exceeds {@link #MAX_RELIABLE_PAYLOAD_SIZE}.
   */
  public static int sendReliableMessage(final MessageSentListener listener,
                                        final String peer_id, final byte tag,
                                        final RealtimeMap payload) {
    checkInstance();
    if (listener == null)
      throw new IllegalArgumentException("Room listener cannot be null");
    if (peer_id == null || peer_id.equals(Scoreflex.getPlayerId()))
      throw new IllegalArgumentException("Invalid participant's id");
    if (payload.getSerializedSize() > MAX_RELIABLE_PAYLOAD_SIZE)
      throw new IllegalArgumentException("Serialized size of the payload exceeds MAX_RELIABLE_PAYLOAD_SIZE");

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
      return STATUS_INTERNAL_ERROR;
    }
  }
  private int sendReliableMessage(String room_id, MessageSentListener listener,
                                  String peer_id, byte tag,
                                  RealtimeMap payload) {
    if (!isSessionConnected()) {
      return STATUS_SESSION_NOT_CONNECTED;
    }

    Proto.RoomMessage.Builder builder = Proto.RoomMessage.newBuilder()
      .setRoomId(room_id)
      .setTimestamp((int)getMmTime())
      .setTag(tag)
      .setIsReliable(true)
      .addAllPayload(realtimeMapToProtoMap(payload));
    if (peer_id != null) {
      builder.setToId(peer_id);
    }

    Proto.RoomMessage msg = builder.build();
    Proto.InMessage inmsg =
      InMessageBuilder.build(last_msgid+1, last_reliable_id, true, msg);

    if (!connection.sendMessage(inmsg)) {
      return STATUS_NETWORK_ERROR;
    }

    last_msgid++;
    snd_message_listeners.put(last_msgid, listener);
    inmsg_queue.put(last_msgid, inmsg);
    return last_msgid;
  }


  /***************************************************************************/
  private void onConnected() {
    if (connection_listener == null)
      return;

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

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onMatchStateChanged(status_code, room, state);
      }
    });
  }

  private void onRoomPropertyChanged(final RoomListener listener,
                                     final Room room,
                                     final Participant from,
                                     final String name) {
    if (listener == null)
      return;

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onRoomPropertyChanged(room, from, name);
      }
    });
  }

  private void onSetRoomPropertyFailed(final RoomListener listener,
                                       final int status_code,
                                       final Room room,
                                       final String name) {
    if (listener == null)
      return;

    main_handler.post(new Runnable() {
      @Override
      public void run() {
        listener.onSetRoomPropertyFailed(status_code, room, name);
      }
    });
  }

  private void onRealTimeMessageReceived(final MessageReceivedListener listener,
                                         final Message msg) {
    if (listener == null)
      return;

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
      inmsg_queue.remove(last_ackid);
    }
  }

  private void onReliableMessageReceived(Proto.OutMessage msg) {
    int msgid = msg.getMsgid();

    if (msgid > last_reliable_id + 1) {
      outmsg_queue.put(msgid, msg);
    }
    else if (msgid == last_reliable_id + 1) {
      last_reliable_id = msgid;
      handleOutMessage(msg);

      while (!outmsg_queue.isEmpty()) {
        Proto.OutMessage nextmsg = outmsg_queue.remove(last_reliable_id + 1);
        if (nextmsg == null)
          break;
        last_reliable_id = nextmsg.getMsgid();
        handleOutMessage(nextmsg);
      }
    }
  }

  private void onUnreliableMessageReceived(Proto.OutMessage msg) {
    int                  idx;
    int                   msgid = msg.getMsgid();
    Proto.OutMessage.Type type  = msg.getType();

    if (type == Proto.OutMessage.Type.ROOM_MESSAGE) {
      Proto.RoomMessage room_msg = msg.getRoomMessage();
      idx = room_msg.getTag();
      if (idx == 0) {
        handleOutMessage(room_msg);
        return;
      }
    }
    else {
      idx = 256 + type.getNumber();
    }

    Integer last_id = last_unreliable_ids.get(idx);
    if (last_id == null || msgid > last_id) {
      last_unreliable_ids.put(idx, msgid);
      handleOutMessage(msg);
    }
  }

  private void handleOutMessage(Proto.OutMessage msg) {
    Log.i("Scoreflex", "msg "+msg.getType()+" received");
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
      case MATCH_STATE_CHANGED:
        handleOutMessage(msg.getMatchStateChanged());
        break;
      case ROOM_PROPERTY_UPDATED:
        handleOutMessage(msg.getRoomPropertyUpdated());
        break;
      case ROOM_MESSAGE:
        handleOutMessage(msg.getRoomMessage());
        break;
      case ACK:
        handleOutMessage(msg.getAck());
        break;
      default:
    }
  }
  private void handleOutMessage(Proto.Connected msg) {
    RealtimeMap info = protoMapToRealtimeMap(msg.getInfoList());

    connection_status    = ConnectionState.CONNECTED;
    retries              = 0;
    session_id           = msg.getSessionId();
    session_info         = RealtimeMap.unmodifiableRealtimeMap(info);
    mm_time              = msg.getMmTime();
    mm_clock_last_update = getMonotonicTime();

    onConnected();

    int udp_port = msg.getUdpPort();
    if (udp_port != 0) {
      try {
        udp_connection = new UDPConnection(this, host, udp_port);
        udp_connection.connect();
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
        connection        = null;
        connection_status = ConnectionState.DISCONNECTED;
        session_id        = null;
        session_info      = null;
        retries           = 0;
        last_msgid        = 0;
        last_ackid        = 0;
        last_reliable_id  = 0;
        last_unreliable_ids.clear();
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

        for (String playerId: r.getPlayersList()) {
          participants.put(playerId, new Participant(playerId, r.getRoomId()));
        }

        MatchState state;
        switch (r.getMatchState()) {
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
          .setConfig(protoMapToRealtimeMap(r.getConfigList()))
          .setProperties(protoMapToRealtimeMap(r.getPropertiesList()))
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

        for (String playerId: r.getPlayersList()) {
          participants.put(playerId, new Participant(playerId, r.getRoomId()));
        }

        MatchState state;
        switch (r.getMatchState()) {
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
            .setConfig(protoMapToRealtimeMap(r.getConfigList()))
            .setProperties(protoMapToRealtimeMap(r.getPropertiesList()))
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

      case NO_DROP_IN_MATCH:
        rcv_message_listeners.remove(r.getRoomId());
        onRoomJoined(room_listeners.remove(r.getRoomId()),
                     STATUS_NO_DROP_IN_MATCH, null);
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

    Participant peer = new Participant(msg.getPlayerId(), msg.getRoomId());
    current_room.addParticipant(peer);

    onPeerJoined(room_listeners.get(current_room.getId()), current_room, peer);
  }
  private void handleOutMessage(Proto.PeerLeftRoom msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    current_room.removeParticipant(msg.getPlayerId());
    onPeerLeft(room_listeners.get(current_room.getId()), current_room,
               msg.getPlayerId());
  }
  private void handleOutMessage(Proto.MatchStateChanged msg) {
    if (current_room == null || !current_room.isSameRoom(msg.getRoomId()))
      return;

    switch (msg.getStatus()) {
      case SUCCESS:
        MatchState state;
        switch (msg.getMatchState()) {
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
        Participant peer = current_room.getParticipant(msg.getPlayerId());
        onRoomPropertyChanged(room_listeners.get(current_room.getId()),
                              current_room, peer, name);
        break;
      case ROOM_NOT_JOINED:
        onSetRoomPropertyFailed(room_listeners.get(current_room.getId()),
                                STATUS_ROOM_NOT_JOINED, current_room, name);
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
      .setPayload(protoMapToRealtimeMap(msg.getPayloadList()))
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
  private static RealtimeMap protoMapToRealtimeMap(List<Proto.MapEntry> pmap) {
    RealtimeMap rtmap = new RealtimeMap();

    for (Proto.MapEntry entry: pmap) {
      Object value = mapEntrytoObject(entry);
      if (value != null)
        rtmap.put(entry.getName(), value);
    }
    return rtmap;
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
        return entry.getBytesVal().toByteArray();
      default:
        return null;
    }
  }

  private static List<Proto.MapEntry> realtimeMapToProtoMap(RealtimeMap rtmap) {
    List<Proto.MapEntry> pmap = new ArrayList<Proto.MapEntry>();
    if (rtmap == null)
      return pmap;

    for (Map.Entry<String, Object> entry: rtmap.entrySet()) {
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
    else if (value instanceof byte[]) {
      entry_builder.setType(Proto.MapEntry.Type.BYTES);
      entry_builder.setBytesVal(
        com.google.protobuf.ByteString.copyFrom((byte[])value)
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
                                        Proto.StartMatch message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.START_MATCH)
        .setStartMatch(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.StopMatch message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.STOP_MATCH)
        .setStopMatch(message)
        .build();
    }
    public static Proto.InMessage build(int msgid, int ackid, boolean reliable,
                                        Proto.ResetMatch message) {
      return Proto.InMessage.newBuilder()
        .setMsgid(msgid)
        .setAckid(ackid)
        .setIsReliable(reliable)
        .setType(Proto.InMessage.Type.RESET_MATCH)
        .setResetMatch(message)
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
