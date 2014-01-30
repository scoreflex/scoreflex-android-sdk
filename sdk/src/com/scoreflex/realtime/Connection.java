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
import java.net.Socket;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.io.IOException;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * @hide
 */
public class Connection
  extends AsyncTask<Proto.InMessage, Void, Void> {

  private Session session;
  private Socket          socket;
  private String          host;
  private int             port;

  public Connection(Session session, String host, int port) {
    this.session = session;
    this.host    = host;
    this.port    = port;
  }

  public void connect(Proto.InMessage message) {
    execute(message);
  }

  public void disconnect(Proto.InMessage message) {
    try {
      cancel(true);
      sendInMessage(message);
      socket.close();
    }
    catch (IOException e) {
    }
  }

  public boolean sendMessage(Proto.InMessage message) {
    try {
      sendInMessage(message);
      return true;
    }
    catch (IOException e) {
      Log.e("Scoreflex", "Failed to send message: "+e);
      return false;
    }
  }

  private void sendInMessage(Proto.InMessage message) throws IOException {
    message.writeDelimitedTo(socket.getOutputStream());
    socket.getOutputStream().flush();
  }

  @Override
  protected Void doInBackground(Proto.InMessage... inmessages) {
    Proto.ConnectionFailed.StatusCode status;

    try {
      InetAddress server_addr = InetAddress.getByName(host);
      socket = new Socket(server_addr, port);
      socket.setTcpNoDelay(true);

      try {
        for (int i = 0; i < inmessages.length; ++i)
          sendInMessage(inmessages[i]);

        InputStream      stream     = socket.getInputStream();
        Proto.OutMessage outmessage = null;
        while ((outmessage = Proto.OutMessage.parseDelimitedFrom(stream)) != null) {
          onMessageReceived(outmessage);

          if (outmessage.getType() == Proto.OutMessage.Type.CONNECTION_FAILED ||
              outmessage.getType() == Proto.OutMessage.Type.CONNECTION_CLOSED) {
            return null;
          }
        }
        status = Proto.ConnectionFailed.StatusCode.NETWORK_ERROR;
      }
      catch (InvalidProtocolBufferException e) {
        status = Proto.ConnectionFailed.StatusCode.PROTOCOL_ERROR;
      }
      catch (IOException e) {
        status = Proto.ConnectionFailed.StatusCode.NETWORK_ERROR;
      }

      socket.close();
    }
    catch (UnknownHostException e) {
      status = Proto.ConnectionFailed.StatusCode.NETWORK_ERROR;
    }
    catch (IOException e) {
      status = Proto.ConnectionFailed.StatusCode.NETWORK_ERROR;
    }
    catch (Exception e) {
      status = Proto.ConnectionFailed.StatusCode.INTERNAL_ERROR;
    }

    if (!isCancelled()) {
      Proto.ConnectionFailed msg = Proto.ConnectionFailed.newBuilder()
        .setStatus(status)
        .build();
      Proto.OutMessage outmessage = Proto.OutMessage.newBuilder()
        .setMsgid(0)
        .setAckid(0)
        .setType(Proto.OutMessage.Type.CONNECTION_FAILED)
        .setConnectionFailed(msg)
        .build();
      onMessageReceived(outmessage);
    }
    return null;
  }

  private void onMessageReceived(final Proto.OutMessage outmessage) {
    session.getHandler().post(new Runnable() {
      @Override
      public void run() {
        session.onMessageReceived(Connection.this, outmessage);
      }
    });
  }
}
