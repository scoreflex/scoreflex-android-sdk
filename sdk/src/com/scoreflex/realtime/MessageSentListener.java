package com.scoreflex.realtime;

/**
 * An interface that contains a callback used by the {@link Session realtime
 * session} to acknowledged the sent reliable messages.
 */
public interface MessageSentListener {
  /**
   * This method was called when a reliable message acknowledgement is received.
   * <br>
   * Possible status codes are:
   *
   * <ul>
   *   <li>{@link Session#STATUS_SUCCESS} The message was successfully sent.</li>
   *   <li>{@link Session#STATUS_ROOM_NOT_JOINED} The attempt to send message
   *   failed because the player has not joined the room.</li>
   *   <li>{@link Session#STATUS_PEER_NOT_FOUND} The attempt to send message
   *   failed because the recipient is not a room's participant.</li>
   *   <li>{@link Session#STATUS_INVALID_MESSAGE} The attempt to send message
   *   failed because the message is malformed.</li>
   *   <li>{@link Session#STATUS_INTERNAL_ERROR} The attempt to send message
   *   failed due to an unexpected error.</li>
   * </ul>
   * <br>
   * This callback is called on the main thread.
   *
   * @see Session#sendReliableMessage(MessageSentListener, String, int, Map)
   *
   * @param status_code A status code indicating the result of the operation.
   * @param msg_id The ID of the reliable message that was sent.
   */
  public void onMessageSent(int status_code, int msg_id);
}
