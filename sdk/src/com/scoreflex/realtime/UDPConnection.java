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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.io.IOException;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @hide
 */
public class UDPConnection extends AsyncTask<Void, Void, Void> {
  private Session           session;
  private InetSocketAddress srvaddr;
  private DatagramSocket    socket;
  private String            host;
  private int               port;
  private boolean           is_connected;

  private final byte MESSAGE   = (byte)0x00;
  private final byte HEARTBEAT = (byte)0x80;
  private final byte START_SEQ = (byte)0x40;
  private final byte END_SEQ   = (byte)0x20;

  private final byte[] buffer    = new byte[8192];
  private final byte[] heartbeat = new byte[] { (byte)(HEARTBEAT | START_SEQ | END_SEQ) };


  protected UDPConnection(Session session, String host, int port) {
    this.session      = session;
    this.host         = host;
    this.port         = port;
    this.is_connected = false;
  }

  protected void connect() throws UnknownHostException, SocketException {
    try {
      srvaddr = new InetSocketAddress(InetAddress.getByName(host), port);
      socket = new DatagramSocket();
      socket.connect(srvaddr);
      socket.setSoTimeout(Session.getUdpHeartbeatTimeout());
      execute((Void)null);
    }
    catch (UnknownHostException e) {
      Log.e("Scoreflex", "Failed to create UDP socket: "+e);
      throw e;
    }
    catch (SocketException e) {
      Log.e("Scoreflex", "Failed to create UDP socket: "+e);
      throw e;
    }
  }

  protected void disconnect() {
    cancel(true);
    socket.disconnect();
    socket.close();
  }

  protected boolean isConnected() {
    return is_connected;
  }

  protected boolean sendMessage(Proto.InMessage message) {
    try {
      byte[] data   = message.toByteArray();
      byte[] buffer = new byte[1 + data.length];
      buffer[0] = (byte)(MESSAGE | START_SEQ | END_SEQ); // Header
      System.arraycopy(data, 0, buffer, 1, data.length); // Body

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
                                                 srvaddr);
      socket.send(packet);
      return true;
    }
    catch (IOException e) {
      Log.e("Scoreflex", "Failed to send message on UDP socket: "+e);
      synchronized (this) { is_connected = false; }
      return false;
    }
  }

  @Override
  protected Void doInBackground(Void... messages) {
    try {
      DatagramPacket rcvpacket = new DatagramPacket(buffer, buffer.length);
      DatagramPacket sndpacket = new DatagramPacket(heartbeat, heartbeat.length,
                                                    srvaddr);

      socket.send(sndpacket);
      while (true) {
        if (isCancelled())
          return null;

        try {
          rcvpacket.setLength(8192);
          socket.receive(rcvpacket);
          synchronized (this) { is_connected = true; }

          byte Hdrs = buffer[0];
          if ((Hdrs & HEARTBEAT) == HEARTBEAT) {
            continue;
          }

          ByteString s = ByteString.copyFrom(buffer, 1, rcvpacket.getLength()-1);
          Proto.OutMessage outmessage = Proto.OutMessage.parseFrom(s);
          onMessageReceived(outmessage);
        }
        catch (SocketTimeoutException e) {
          socket.send(sndpacket);
        }
        catch (IOException e) {
          if (!socket.isConnected()) {
            Log.e("Scoreflex", "Failed to read message on UDP socket: "+e);
            break;
          }
        }
      }
    }
    catch (SocketException e) {
      if (!isCancelled())
        Log.e("Scoreflex", "Failed to read message on UDP socket: "+e);
    }
    catch (IOException e) {
      if (!isCancelled())
        Log.e("Scoreflex", "Failed to read message on UDP socket: "+e);
    }

    socket.close();
    return null;
  }

  private void onMessageReceived(final Proto.OutMessage outmessage) {
    session.getHandler().post(new Runnable() {
      @Override
      public void run() {
        session.onMessageReceived(UDPConnection.this, outmessage);
      }
    });
  }
}
