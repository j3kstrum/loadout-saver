package com.loadoutsaver.interfaces;

/**
 * Represents a stack of items.
 */
public interface IItemStack extends ISerializable<IItemStack> {

    int ItemID();

    int Quantity();

}
