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

import java.util.Set;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;

/**
 * A {@link RealtimeMap} is an implementation of a <code>Map</code> where the
 * keys are <code>String</code>s whereas the values can be:
 * <ul>
 *   <li><code>Integer</code></li>
 *   <li><code>Long</code></li>
 *   <li><code>Double</code></li>
 *   <li><code>Boolean</code></li>
 *   <li><code>String</code></li>
 *   <li><code>byte[]</code></li>
 * </ul>
 *
 * As a standard <code>Map</code>, a {@link RealtimeMap} provides helper methods
 * to iterate through all of the keys contained in it, as well as various
 * methods to access and update the key/value pairs.
 * <br>
 * It also provides a method to know the number of bytes required to encode the
 * data.
 */
public class RealtimeMap extends AbstractMap<String,Object> {
  private Map<String, Object> map;
  private int                 serialized_size;

  /**
   * Constructs a new empty {@link RealtimeMap} instance.
   */
  public RealtimeMap() {
    map             = new HashMap<String, Object>();
    serialized_size = 0;
  }

  /**
   * Get the number of bytes required to encode this map.
   *
   * @return The number of bytes.
   */
  public int getSerializedSize() {
    // Add 4 bytes per element:
    //  - 1 byte to reference it in the InMessage/OutMessage
    //  - 1 byte for the element "Type" in the MapEntry
    //  - 1 byte for the element "name" in the MapEntry
    //  - 1 byte for the element "..._val" in the MapEntry
    return (serialized_size + 4*map.size());
  }

  /**
   * Returns the value of the mapping with the specified key, without casting
   * the value.
   *
   * @param key The key.
   *
   * @return The value of the mapping with the specified key, or
   * <code>null</code> if no mapping for the specified key is found.
   */
  public Object get(String key) {
    return map.get(key);
  }

  /**
   * Returns the <code>Integer</code> instance mapped with the specified key.
   *
   * @param key The key.
   *
   * @return The <code>Integer</code> instance mapped with the specified key, or
   * <code>null</code> if no mapping for the specified key is found.
   *
   * @throws ClassCastException if the value is not an <code>Integer</code>
   * instance.
   */
  public Integer getAsInteger(String key) {
    Object value = map.get(key);
    if (value == null || value instanceof Integer)
      return (Integer)value;
    throw new ClassCastException();
  }

  /**
   * Returns the <code>Long</code> instance mapped with the specified key.
   *
   * @param key The key.
   *
   * @return The <code>Long</code> instance mapped with the specified key, or
   * <code>null</code> if no mapping for the specified key is found.
   *
   * @throws ClassCastException if the value is not a <code>Long</code>
   * instance.
   */
  public Long getAsLong(String key) {
    Object value = map.get(key);
    if (value == null || value instanceof Long)
      return (Long)value;
    throw new ClassCastException();
  }

  /**
   * Returns the <code>Double</code> instance mapped with the specified key.
   *
   * @param key The key.
   *
   * @return The <code>Double</code> instance mapped with the specified key, or
   * <code>null</code> if no mapping for the specified key is found.
   *
   * @throws ClassCastException if the value is not a <code>Double</code>
   * instance.
   */
  public Double getAsDouble(String key) {
    Object value = map.get(key);
    if (value == null || value instanceof Double)
      return (Double)value;
    throw new ClassCastException();
  }

  /**
   * Returns the <code>Boolean</code> instance mapped with the specified key.
   *
   * @param key The key.
   *
   * @return The <code>Boolean</code> instance mapped with the specified key, or
   * <code>null</code> if no mapping for the specified key is found.
   *
   * @throws ClassCastException if the value is not a <code>Boolean</code>
   * instance.
   */
  public Boolean getAsBoolean(String key) {
    Object value = map.get(key);
    if (value == null || value instanceof Boolean)
      return (Boolean)value;
    throw new ClassCastException();
  }

  /**
   * Returns the <code>String</code> instance mapped with the specified key.
   *
   * @param key The key.
   *
   * @return The <code>String</code> instance mapped with the specified key, or
   * <code>null</code> if no mapping for the specified key is found.
   *
   * @throws ClassCastException if the value is not a <code>String</code>
   * instance.
   */
  public String getAsString(String key) {
    Object value = map.get(key);
    if (value == null || value instanceof String)
      return (String)value;
    throw new ClassCastException();
  }

  /**
   * Returns the <code>byte[]</code> instance mapped with the specified key.
   *
   * @param key The key.
   *
   * @return The <code>byte[]</code> instance mapped with the specified key, or
   * <code>null</code> if no mapping for the specified key is found.
   *
   * @throws ClassCastException if the value is not a <code>byte[]</code>
   * instance.
   */
  public byte[] getAsByteArray(String key) {
    Object value = map.get(key);
    if (value == null || value instanceof byte[])
      return (byte[])value;
    throw new ClassCastException();
  }

  /**
   * Maps the specified key to the specified value. Valid types for the value
   * object are:
   * <ul>
   *   <li><code>Integer</code></li>
   *   <li><code>Long</code></li>
   *   <li><code>Double</code></li>
   *   <li><code>Boolean</code></li>
   *   <li><code>String</code></li>
   *   <li><code>byte[]</code></li>
   * </ul>
   *
   * @param key The key.
   * @param value The value.
   *
   * @return The value of any previous mapping with the specified key or
   * <code>null</code> if there was no mapping.
   *
   * @throws ClassCastException if the value's type is inappropriate for this map.
   */
  public Object put(String key, Object value) {
    Object old_value = remove(key);
    if (value != null) {
      serialized_size += getSerializedSize(key, value);
      map.put(key, value);
    }
    return old_value;
  }

  /**
   * Removes a mapping with the specified key from this map.
   *
   * @param key The of the mapping to remove.
   *
   * @return The value of any previous mapping with the specified key or
   * <code>null</code> if there was no mapping.
   */
  public Object remove(String key) {
    Object old_value = map.remove(key);
    if (old_value == null)
      return old_value;
    serialized_size -= getSerializedSize(key, old_value);
    return old_value;
  }

  /**
   * Removes all elements from this map, leaving it empty.
   */
  public void clear() {
    serialized_size = 0;
    map.clear();
  }

  /**
   * Returns the number of mappings in this map.
   *
   * @return The number of mappings in this map.
   */
  public int size() {
    return map.size();
  }

  /**
   * Return whether this map is empty.
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * Returns a <code>Set</code> containing all of the mappings in this map. Each
   * mapping is an instance of <code>Map.Entry</code>. This <code>Set</code> is
   * unmodifiable.
   *
   * @return An unmodifiable set of the mappings.
   */
  public Set<Entry<String,Object>> entrySet() {
    return Collections.unmodifiableSet(map.entrySet());
  }

  /**
   * Returns a <code>Set</code> of the keys contained in this map. This
   * <code>Set</code> is unmodifiable.
   *
   * @return The unmodifiable set of keys.
   */
  public Set<String> keySet() {
    return Collections.unmodifiableSet(map.keySet());
  }

  /**
   * Returns a <code>Collection</code> of the values contained in this map. This
   * <code>Collection</code> is unmodifiable.
   *
   * @return an unmodifiable collection of the values contained in this map.
   */
  public Collection<Object> values() {
    return Collections.unmodifiableCollection(map.values());
  }

  /**
   * Returns whether this map contains the specified key.
   *
   * @param key The key to search for.
   *
   * @return <code>true</code> if this map contains the specified key,
   * <code>false</code> otherwise.
   */
  public boolean containsKey(String key) {
    return map.containsKey(key);
  }

  /**
   * Returns whether this map contains the specified value.
   *
   * @param value The value to search for.
   *
   * @return <code>true</code> if this map contains the specified value,
   * <code>false</code> otherwise.
   */
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  /**
   * Compares this instance with the specified object and indicates if they are
   * equal. In order to be equal, <code>object</code> must represent the same
   * object as this instance using a class-specific comparison.
   * <br><br>
   * This implementation returns <code>true</code> if <code>this ==
   * object</code>. If not, it checks the structure of <code>object</code>. If
   * it is not a {@link RealtimeMap} or of a different size, this returns
   * <code>false</code>. Otherwise, it iterates its own entry set: looking up
   * each entry's key in <code>object</code>. If any value does not equal the
   * other map's value for the same key, this returns false. Otherwise it
   * returns true.
   *
   * @param object The object to compare this instance with.
   *
   * @return <code>true</code> if the specified object is equal to this
   * map. <code>false</code> otherwise.
   */
  public boolean equals(Object object) {
    if (object == this)
      return true;
    if (!(object instanceof RealtimeMap))
      return false;

    return (((RealtimeMap)object).serialized_size == serialized_size &&
            ((RealtimeMap)object).map.equals(map));
  }

  /**
   * Returns a wrapper on the specified map which throws
   * UnsupportedOperationException whenever an attempt is made to modify the
   * list.
   *
   * @param r The map to wrap in an unmodifiable map.
   *
   * @return An unmodifiable map.
   */
  public static UnmodifiableRealtimeMap unmodifiableRealtimeMap(RealtimeMap r) {
    return new UnmodifiableRealtimeMap(r);
  }

  private static class UnmodifiableRealtimeMap extends RealtimeMap {
    private final RealtimeMap m;

    protected UnmodifiableRealtimeMap(RealtimeMap m) {
      if (m == null)
        throw new NullPointerException();
      this.m = m;
    }

    public int     size()                     { return m.size(); }
    public boolean isEmpty()                  { return m.isEmpty(); }
    public boolean containsKey(String key)    { return m.containsKey(key); }
    public boolean containsValue(String val)  { return m.containsValue(val); }
    public Object  get(String key)            { return m.get(key); }
    public Integer getAsInteger(String key)   { return m.getAsInteger(key); }
    public Long    getAsLong(String key)      { return m.getAsLong(key); }
    public Double  getAsDouble(String key)    { return m.getAsDouble(key); }
    public Boolean getAsBoolean(String key)   { return m.getAsBoolean(key); }
    public String  getAsString(String key)    { return m.getAsString(key); }
    public byte[]  getAsByteArray(String key) { return m.getAsByteArray(key); }

    public Object put(String key, Object value) {
      throw new UnsupportedOperationException();
    }
    public Object remove(String key) {
      throw new UnsupportedOperationException();
    }
    public void clear() {
      throw new UnsupportedOperationException();
    }

    private transient Set<String>                   keySet   = null;
    private transient Set<Map.Entry<String,Object>> entrySet = null;
    private transient Collection<Object>            values = null;

    public Set<String> keySet() {
      if (keySet==null)
        keySet = m.keySet();
      return keySet;
    }

    public Set<Map.Entry<String,Object>> entrySet() {
      if (entrySet==null)
        entrySet = m.entrySet();
      return entrySet;
    }

    public Collection<Object> values() {
      if (values==null)
        values = m.values();
      return values;
    }

    public boolean equals(Object o) { return m.equals(o); }
    public int     hashCode()       { return m.hashCode(); }
    public String  toString()       { return m.toString(); }
  }


  protected static int getSerializedSize(String key, Object value) {
    int sz = key.length() + ((key.length() < 128) ? 1: 2);

    if (value instanceof Integer) {
      return (sz + getNumberOfBytes((Integer)value));
    }
    if (value instanceof Long) {
      return (sz + getNumberOfBytes((Long)value));
    }
    if (value instanceof Double) {
      return (sz + 8);
    }
    if (value instanceof Boolean) {
      return (sz + 1);
    }
    if (value instanceof String) {
      int len = ((String)value).length();
      return (sz + len + (len<128 ? 1 : 2));
    }
    if (value instanceof byte[]) {
      int len = ((byte[])value).length;
      return (sz + len + (len<128 ? 1 : 2));
    }

    throw new ClassCastException();
  }


  private static int getNumberOfBytes(Integer value) {
    if (value < 0) {
      if (value >= -0x00000040)
        return 1;
      if (value >= -0x00002000)
        return 2;
      if (value >= -0x00100000)
        return 3;
      if (value >= -0x08000000)
        return 4;
      return 5;
    }
    else {
      if (value < 0x00000080)
        return 1;
      if (value < 0x00004000)
        return 2;
      if (value < 0x00200000)
        return 3;
      if (value < 0x10000000)
        return 4;
      return 5;
    }
    /*
    int  n = 1;
    long i;
    if (value < 0)
      i = -2 * value.longValue() + 1;
    else
      i = value.longValue();

    while (i >= 128) {
      i >>= 7;
      n++;
    }
    return n;
    */
  }

  private static int getNumberOfBytes(Long value) {
    if (value < 0) {
      if (value >= -0x00000040)
        return 1;
      if (value >= -0x00002000)
        return 2;
      if (value >= -0x00100000)
        return 3;
      if (value >= -0x08000000)
        return 4;
      if (value >= -0x0000000400000000L)
        return 5;
      if (value >= -0x0000020000000000L)
        return 6;
      if (value >= -0x0001000000000000L)
        return 7;
      if (value >= -0x0080000000000000L)
        return 8;
      if (value >= -0x4000000000000000L)
        return 9;
      return 10;
    }
    else {
      if (value < 0x00000080)
        return 1;
      if (value < 0x00004000)
        return 2;
      if (value < 0x00200000)
        return 3;
      if (value < 0x80000000)
        return 4;
      if (value < 0x0000000800000000L)
        return 5;
      if (value < 0x0000040000000000L)
        return 6;
      if (value < 0x0002000000000000L)
        return 7;
      if (value < 0x0100000000000000L)
        return 8;
      if (value < 0x8000000000000000L)
        return 9;
      return 10;
    }
  }
}
