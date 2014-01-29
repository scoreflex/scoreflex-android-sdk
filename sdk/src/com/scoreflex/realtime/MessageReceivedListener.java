package com.scoreflex.realtime;

/**
 * An interface that contains a callback used by the {@link Session realtime
 * session} to notify the player of messages received from a participant.
 */
public interface MessageReceivedListener {
  /**
   * This method is called when the player receives a message from a participant
   * (reliable or not).
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#sendUnreliableMessage(String, int, Map)
   * @see Session#sendReliableMessage(MessageSentListener, String, int, Map)
   *
   * @param msg The message that was received.
   */
  public void onMessageReceived(Message msg);
}
