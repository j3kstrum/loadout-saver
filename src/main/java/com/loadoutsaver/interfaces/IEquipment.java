package com.loadoutsaver.interfaces;

import net.runelite.api.EquipmentInventorySlot;

import java.util.Map;

/**
 * Represents the player's worn equipment.
 */
public interface IEquipment extends ISerializable<IEquipment> {

    Map<EquipmentInventorySlot, IItemStack> GetEquipment();

}
