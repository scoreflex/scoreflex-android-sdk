package com.scoreflex.realtime;

import java.util.Map;
import java.util.Collections;

/**
 * A participant is a player inside a realtime room. When a player joins a room,
 * he defines his set of properties and he can update them by calling {@link
 * Session#setCurrentParticipantProperty(String, Object)}.
 * <br>
 * These properties are public, all room's participants can see them and they
 * are notfified of any changes with the callback {@link
 * RoomListener#onParticipantPropertyChanged(int, Room, String, String)}.
 */
public class Participant {
  private final String              id;
  private final String              room_id;
  private final Map<String, Object> properties;
  private final Map<String, Object> propertiesView;

  protected Participant(String id, String room_id,
                        Map<String, Object> properties) {
    this.id             = id;
    this.room_id        = room_id;
    this.properties     = Collections.unmodifiableMap(properties);
    this.propertiesView = Collections.unmodifiableMap(this.properties);
  }

  /**
   * Retrieves the participant's ID.
   *
   * @return The participant's ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Retrieves the room's ID of the participant.
   *
   * @return The room's ID.
   */
  public String getRoomId() {
    return room_id;
  }

  /**
   * Retrieves the participant's properties.
   *
   * @return The participant's properties.
   */
  public Map<String, Object> getProperties() {
    return propertiesView;
  }

  /**
   * Retrieves a specific participant's property, given its key.
   *
   * @return The property value or <code>null</code> if the property does not
   * exists.
   */
  public Object getProperty(String key) {
    return properties.get(key);
  }

  protected void addProperty(String key, Object value) {
    properties.put(key, value);
  }

  protected void removeProperty(String key) {
    properties.remove(key);
  }
}
