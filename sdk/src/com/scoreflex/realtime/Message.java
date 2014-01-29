package com.scoreflex.realtime;

import java.util.Map;
import java.util.Collections;

/**
 * Message received in a realtime room. The room's participants receive it in
 * the callback {@link MessageReceivedListener#onMessageReceived(Message)}.
 */
public class Message {
  private final String              room_id;
  private final String              sender_id;
  private final int                 tag;
  private final Map<String, Object> payload;
  private final Map<String, Object> payloadView;

  protected static class Builder {
    private String              room_id;
    private String              sender_id;
    private int                 tag;
    private Map<String, Object> payload;

    protected Builder() {
    }

    protected Builder setRoomId(String id) {
      this.room_id = id;
      return this;
    }

    protected Builder setSenderId(String id) {
      this.sender_id = id;
      return this;
    }

    protected Builder setTag(int tag) {
      this.tag = tag;
      return this;
    }

    protected Builder setPayload(Map<String, Object> payload) {
      this.payload = payload;
      return this;
    }

    protected Message build() {
      return new Message(this);
    }
  }

  protected static Builder builder() {
    return new Builder();
  }

  private Message(Builder builder) {
    this.room_id     = builder.room_id;
    this.sender_id   = builder.sender_id;
    this.tag         = builder.tag;
    this.payload     = builder.payload;
    this.payloadView = Collections.unmodifiableMap(this.payload);
  }

  /**
   * Retrieves the room's ID in which the message was received.
   *
   * @return The room's ID.
   */
  public String getRoomId() {
    return room_id;
  }

  /**
   * Retrieves the sender's ID.
   *
   * @return The sender's ID.
   */
  public String getSenderId() {
    return sender_id;
  }

  /**
   * Retrieves the message's tag.
   *
   * @return The message's tag.
   */
  public int getTag() {
    return tag;
  }

  /**
   * Retrieves the message's payload.
   *
   * @return The message's payload.
   */
  public Map<String, Object> getPayload() {
    return payloadView;
  }
}
