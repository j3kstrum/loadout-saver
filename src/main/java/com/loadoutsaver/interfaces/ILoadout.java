package com.loadoutsaver.interfaces;

/**
 * A loadout is the top-level interface for the plugin.
 * Loadouts have a name and are composed of an inventory setup and an equipment setup.
 */
public interface ILoadout extends ISerializable<ILoadout> {

    String GetName();

    IInventory GetInventory();

    IEquipment GetEquipment();

}
