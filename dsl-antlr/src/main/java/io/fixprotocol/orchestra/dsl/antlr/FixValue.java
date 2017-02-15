/**
 * Copyright 2017 FIX Protocol Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package io.fixprotocol.orchestra.dsl.antlr;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents an mutable value of a DSL expression 
 * <br/>
 * Access control, immutable usage and synchronization of {@link #assign(FixValue)}
 * must be guarded externally.
 * 
 * @param T storage type for value
 * 
 * @author Don Mendelson
 */
public class FixValue<T> implements FixNode {

  /**
   * Creates a new object with the specified name and values of the operand
   * 
   * @param name identifier of the new object
   * @param operand value to copy
   * @return a new FixValue instance
   * @throws ScoreException if the data type is not handled
   */
  public static FixValue<?> copy(String name, FixValue<?> operand) throws ScoreException {
    String valueType = operand.getValue().getClass().getName();
    switch (valueType) {
      case "java.lang.Integer":
        return new FixValue<Integer>(name, operand.getType(), (Integer) operand.getValue());
      case "java.lang.Boolean":
        return new FixValue<Boolean>(name, operand.getType(), (Boolean) operand.getValue());
      case "java.lang.Character":
        return new FixValue<Character>(name, operand.getType(), (Character) operand.getValue());
      case "java.lang.String":
        return new FixValue<String>(name, operand.getType(), (String) operand.getValue());
      case "java.math.BigDecimal":
        return new FixValue<BigDecimal>(name, operand.getType(), (BigDecimal) operand.getValue());
      default:
        throw new ScoreException("Unable to copy type " + valueType);
    }
  }

  private final String name;
  private final FixType type;
  private T value;

  /**
   * Construct an unnamed expression value
   * 
   * @param type a FIX data type
   * @param value an immutable value
   */
  public FixValue(FixType type, T value) {
    this(null, type, value);
  }

  /**
   * Construct with a name only for search
   * 
   * @param name message component name
   */
  public FixValue(String name) {
    this(name, null, null);
  }

  /**
   * Construct a named expression without value
   * 
   * @param name named value
   * @param type a FIX data type
   */
  public FixValue(String name, FixType type) {
    this(name, type, null);
  }

  /**
   * Construct a named expression value
   * 
   * @param name named value
   * @param type a FIX data type
   * @param value an immutable value
   */
  public FixValue(String name, FixType type, T value) {
    this.name = name;
    this.type = type;
    this.value = value;
  }

  /**
   * Assigns a value to this FixValue
   * 
   * @param operand other FixValue
   * @throws ScoreException if a type conflict occurs
   */
  @SuppressWarnings("unchecked")
  public void assign(FixValue<?> operand) throws ScoreException {
    Objects.requireNonNull(operand, "Missing operand");
    if (this.type.getBaseType() != operand.getType().getBaseType()) {
      throw new ScoreException(
          String.format("Data type mismatch between %s and %s", this.type, operand.getType()));
    }
    this.value = (T) operand.getValue();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FixValue other = (FixValue) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the type
   */
  public FixType getType() {
    return type;
  }

  /**
   * @return the value
   */
  public T getValue() {
    return value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  /**
   * @param value the value to set
   */
  public void setValue(T value) {
    this.value = value;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "FixValue [" + (name != null ? "name=" + name + ", " : "")
        + (type != null ? "type=" + type + ", " : "") + (value != null ? "value=" + value : "")
        + "]";
  }

}
