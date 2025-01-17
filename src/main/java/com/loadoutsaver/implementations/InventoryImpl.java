package com.loadoutsaver.implementations;

import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.IItemStack;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Represents the player's inventory in a loadout.
 */
public class InventoryImpl implements IInventory {
    public static IInventory Deserializer = new InventoryImpl();

    private InventoryImpl() {

    }

    private InventoryImpl(IItemStack[] items) {
        if (items.length != 28) {
            throw new IllegalArgumentException("Expected inventories to have 28 slots.");
        }
        this.items = items;
    }

    public InventoryImpl(ItemContainer inventory) {
        this(ParseInventory(inventory));
    }

    private static IItemStack[] ParseInventory(ItemContainer inventory) {
        // I expected this to return 28, but it sometimes returns smaller values (27, or lower)
        // when the last k slots of the inventory are empty.
        // Rendering fails if that's the case, and this should always be 28 in size anyways.
        int inventorySize = 28; // inventory.size();
        IItemStack[] parsed = new IItemStack[inventorySize];

        if (inventory == null) {
            for (int i = 0; i < inventorySize; i++) {
                parsed[i] = new ItemStackImpl(-1, 0);
            }
            return parsed;
        }

        for (int i = 0; i < inventorySize; i++) {
            Item item = inventory.getItem(i);
            if (item == null) {
                parsed[i] = new ItemStackImpl(-1, 0);
            }
            else {
                parsed[i] = new ItemStackImpl(item.getId(), item.getQuantity());
            }
        }

        return parsed;
    }

    private IItemStack[] items;

    @Override
    public IItemStack[] GetItems() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        if (items.length != 28) {
            System.err.println("Inventory had " + items.length + "items");
        }
        return items;
    }

    /**
     * Serialization format: colon-delimited item stacks, b64 encoded.
     * @return The serialized string following the above format.
     */
    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return Arrays.stream(items).map(
                IItemStack::SerializeString
        ).map(
                String::getBytes
        ).map(
                Base64.getEncoder()::encodeToString
        ).collect(
                Collectors.joining(":")
        );
    }

    /**
     * Serialization format: colon-delimited item stacks, b64 encoded.
     * @return The inventory object deserialized from a string in the above format.
     */
    @Override
    public IInventory DeserializeString(String serialized) {
        String[] encodedItems = serialized.split(":", -1);
        IItemStack[] items = Arrays.stream(encodedItems).map(
                Base64.getDecoder()::decode
        ).map(
                String::new
        ).map(
                ItemStackImpl.Deserializer::DeserializeString
        ).toArray(IItemStack[]::new);

        return new InventoryImpl(items);
    }
}
