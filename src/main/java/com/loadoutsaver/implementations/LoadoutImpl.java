package com.loadoutsaver.implementations;

import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.ILoadout;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;

import java.util.Base64;

public class LoadoutImpl implements ILoadout {

    public static ILoadout Deserializer = new LoadoutImpl();

    private LoadoutImpl() {

    }

    public LoadoutImpl(String name, Client client) {
        this(name, ParseInventory(client), ParseEquipment(client));
        if (
                client.getItemContainer(InventoryID.INVENTORY) == null
                && client.getItemContainer(InventoryID.EQUIPMENT) == null
        ) {
            throw new IllegalArgumentException("Client state was unexpected in loadout parser.");
        }
    }

    private static IInventory ParseInventory(Client client) {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        return new InventoryImpl(inventory);
    }

    private static IEquipment ParseEquipment(Client client) {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        return new EquipmentImpl(equipment);
    }

    private LoadoutImpl(String name, IInventory inventory, IEquipment equipment) {
        this(name, inventory, equipment, false);
    }

    private LoadoutImpl(String name, IInventory inventory, IEquipment equipment, boolean replaceName) {
        if (name.contains(":")) {
            // Some high-level sanitization. The ":" is the one that will break the (de)-serialization here.
            if (replaceName) {
                String replacement = name.replace(":", "[COLON]");
                System.out.println("WARNING: Replacing name " + name + " with " + replacement);
                name = replacement;
            }
            else {
                throw new IllegalArgumentException("Name contained illegal characters: " + name);
            }
        }
        this.name = name;
        this.inventory = inventory;
        this.equipment = equipment;
    }

    private String name;
    private IInventory inventory;
    private IEquipment equipment;

    @Override
    public String GetName() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return name;
    }

    @Override
    public IInventory GetInventory() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return inventory;
    }

    @Override
    public IEquipment GetEquipment() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return equipment;
    }

    // Serialization format:
    // {name}:{b64 inventory}:{b64 equipment}

    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        String encodedInventory = Base64.getEncoder().encodeToString(inventory.SerializeString().getBytes());
        String encodedEquipment = Base64.getEncoder().encodeToString(equipment.SerializeString().getBytes());

        return this.name + ":" + encodedInventory + ":" + encodedEquipment;
    }

    @Override
    public ILoadout DeserializeString(String serialized) {
        String[] components = serialized.strip().split(":");
        if (components.length != 3) {
            // Violation of format.
            throw new IllegalArgumentException("Corrupted loadout: " + serialized);
        }
        String loadoutName = components[0];
        String decodedInventory = new String(Base64.getDecoder().decode(components[1]));
        String decodedEquipment = new String(Base64.getDecoder().decode(components[2]));

        IInventory inventory = InventoryImpl.Deserializer.DeserializeString(decodedInventory);
        IEquipment equipment = EquipmentImpl.Deserializer.DeserializeString(decodedEquipment);

        return new LoadoutImpl(loadoutName, inventory, equipment, true);
    }
}
