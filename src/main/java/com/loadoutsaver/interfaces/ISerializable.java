package com.loadoutsaver.interfaces;

import java.util.Dictionary;
import java.util.List;

/**
 * Serializable objects can convert themselves to a string and can produce a new object from a serialized form.
 * @param <T> The type to which deserialization should occur. This is expected to be equal to the object's type.
 */
public interface ISerializable<T> {
    String SerializeString();

    T DeserializeString(String serialized);
}
