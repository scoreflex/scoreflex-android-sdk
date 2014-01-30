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

import java.util.Map;
import java.util.Collections;

/**
 * A realtime room with its configuration, properties and participants. Such
 * rooms can be created by calling {@link Session#createRoom(String, RoomConfig,
 * Map, Map)}.
 */
public class Room {
  private final String                   id;
  private final Session                  session;
  private       MatchState               state;
  private       Map<String, Object>      config;
  private       Map<String, Object>      properties;
  private       Map<String, Participant> participants;
  private final Map<String, Object>      configView;
  private final Map<String, Object>      propertiesView;
  private final Map<String, Participant> participantsView;

  protected static class Builder {
    private String                   id;
    private Session                  session;
    private MatchState                state;
    private Map<String, Object>      config;
    private Map<String, Object>      properties;
    private Map<String, Participant> participants;

    protected Builder() {
    }

    protected Builder setId(String id) {
      this.id = id;
      return this;
    }

    protected Builder setSession(Session session) {
      this.session = session;
      return this;
    }

    protected Builder setMatchState(MatchState state) {
      this.state = state;
      return this;
    }

    protected Builder setConfig(Map<String, Object> config) {
      this.config = config;
      return this;
    }

    protected Builder setProperties(Map<String, Object> properties) {
      this.properties = properties;
      return this;
    }

    protected Builder setParticipants(Map<String, Participant> participants) {
      this.participants = participants;
      return this;
    }

    protected Room build() {
      return new Room(this);
    }
  }

  protected static Builder builder() {
    return new Builder();
  }

  private Room(Builder builder) {
    this.id               = builder.id;
    this.session          = builder.session;
    this.state            = builder.state;
    this.config           = builder.config;
    this.properties       = builder.properties;
    this.participants     = builder.participants;
    this.configView       = Collections.unmodifiableMap(this.config);
    this.propertiesView   = Collections.unmodifiableMap(this.properties);
    this.participantsView = Collections.unmodifiableMap(this.participants);
  }

  /**
   * Retrieves the room's ID.
   *
   * @return The room's ID.
   */
  public String getId() {
    return id;
  }

  /**
   * Retrieves the room's configuration.
   *
   * @return The room's configuration.
   */
  public Map<String, Object> getConfig() {
    return configView;
  }

  /**
   * Retrieves a specific parameter's value in the room's configuration, given
   * its key.
   *
   * @return The parameter's value or <code>null</code> if the parameter does
   * not exists.
   */
  public Object getConfigValue(String key) {
    return config.get(key);
  }

  /**
   * Retrieves the room's properties.
   *
   * @return The room's properties.
   */
  public Map<String, Object> getProperties() {
    return propertiesView;
  }

  /**
   * Retrieves a specific room's property, given its key.
   *
   * @return The property value or <code>null</code> if the property does not
   * exists.
   */
  public Object getProperty(String key) {
    return properties.get(key);
  }

  /**
   * Retrieves the room's participants.
   *
   * @return The room's participants.
   */
  public Map<String, Participant> getParticipants() {
    return participantsView;
  }

  /**
   * Retrieves a specific participant inside the room, given his ID.
   *
   * @see Participant
   *
   * @return the participant or <code>null</code> if the participant is not
   * found in the room.
   */
  public Participant getParticipant(String id) {
    return participants.get(id);
  }

  /**
   * Retrieves the match's state of the room.
   *
   * @see MatchState
   *
   * @return the match's state
   */
  public MatchState getMatchState() {
    return state;
  }

  protected boolean isSameRoom(String id) {
    return id.equals(id);
  }

  protected void addParticipant(Participant p) {
    participants.put(p.getId(), p);
  }

  protected void removeParticipant(String id) {
    participants.remove(id);
  }

  protected void setMatchState(MatchState state) {
    this.state = state;
  }

  protected void addProperty(String key, Object value) {
    properties.put(key, value);
  }

  protected void removeProperty(String key) {
    properties.remove(key);
  }

  protected void addParticipantProperty(String id, String key, Object value) {
    Participant participant = participants.get(id);
    if (participant != null)
      participant.addProperty(key, value);
  }

  protected void removeParticipantProperty(String id, String key) {
    Participant participant = participants.get(id);
    if (participant != null)
      participant.removeProperty(key);
  }

}
