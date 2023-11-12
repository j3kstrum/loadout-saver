package com.loadoutsaver.interfaces;

/**
 * Represents the player's inventory.
 */
public interface IInventory extends ISerializable<IInventory> {

    IItemStack[] GetItems();

}
