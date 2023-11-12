package com.loadoutsaver.interfaces;

/**
 * Subscribers can be updated when an object is modified.
 * @param <T> The type of the object over which the subscriber is interested.
 */
public interface ISubscriber<T> {

    void Update(T updatedObject);
}
