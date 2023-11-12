package com.loadoutsaver.implementations;

import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.ILoadout;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

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
            // I'm seeing that the inventory or equipment are null in a couple of cases:
            //  1. When we're not actually in-game (this is expected)
            //  2. When we're just starting to be in-game and haven't accessed the inventory or equipment (this is weird)
            //  3. When the equipment or inventory are empty. This needs to be supported but because of the other cases,
            //      we won't support saving completely empty loadouts (which should be fine...)
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



    /**
     * Serialization format:
     * {name};{b64 inventory}:{b64 equipment}:{version}
     * This allows us to assume there are no semicolons after the name, so all name chars are supported.
     * @return The serialized loadout following the above format.
     */
    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        String encodedInventory = Base64.getEncoder().encodeToString(inventory.SerializeString().getBytes());
        String encodedEquipment = Base64.getEncoder().encodeToString(equipment.SerializeString().getBytes());
        String version = "1";

        return this.name + ";" + encodedInventory + ":" + encodedEquipment + ":" + version;
    }

    @Override
    public ILoadout DeserializeString(String serialized) {
        String[] topComponents = serialized.strip().split(";", -1);
        if (topComponents.length < 2) {
            // Violation of format.
            System.err.println("Corrupted loadout: wrong number of top components: " + Arrays.toString(topComponents));
            throw new IllegalArgumentException("Corrupted loadout: " + serialized);
        }

        String loadoutName = String.join(
                ";",
                Arrays.copyOfRange(topComponents, 0, topComponents.length - 1)
        );

        String[] components = topComponents[topComponents.length - 1].split(":", -1);
        if (components.length != 3) {
            // Violation of format.
            System.err.println("Corrupted loadout: wrong number of components: " + Arrays.toString(components));
            throw new IllegalArgumentException("Corrupted loadout: " + serialized);
        }
        String decodedInventory = new String(Base64.getDecoder().decode(components[0]));
        String decodedEquipment = new String(Base64.getDecoder().decode(components[1]));
        String version = components[2];

        if (!Objects.equals(version, "1")) {
            System.err.println("Corrupted loadout: unknown version: " + version);
            throw new IllegalArgumentException("Unknown loadout version: " + version);
        }

        IInventory inventory = InventoryImpl.Deserializer.DeserializeString(decodedInventory);
        IEquipment equipment = EquipmentImpl.Deserializer.DeserializeString(decodedEquipment);

        return new LoadoutImpl(loadoutName, inventory, equipment);
    }
}
