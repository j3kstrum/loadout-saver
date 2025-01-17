package com.loadoutsaver.implementations;

import net.runelite.api.EquipmentInventorySlot;

import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IItemStack;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EquipmentImpl implements IEquipment {

    public static IEquipment Deserializer = new EquipmentImpl();

    private EquipmentImpl() {

    }

    private EquipmentImpl(Map<EquipmentInventorySlot, IItemStack> equipment) {
        this.equipment = equipment;
    }

    public EquipmentImpl(ItemContainer equipment) {
        this(ParseEquipment(equipment));
    }

    /**
     * Parses the user's equipment from the equipment item container.
     * @param equipment Runelite's item container containing user equipment.
     * @return A map from equipment slots to the item stacks in that slot.
     */
    private static Map<EquipmentInventorySlot, IItemStack> ParseEquipment(ItemContainer equipment) {
        Map<EquipmentInventorySlot, IItemStack> mapping = new HashMap<>();

        if (equipment == null) {
            return mapping;
        }

        int equipmentSize = equipment.size();

        // For each equipment object in the item container, build an item stack based on the item properties.
        for (int i = 0; i < equipmentSize; i++) {
            Item item = equipment.getItem(i);
            EquipmentInventorySlot slot = ID_TO_SLOT.get(i);
            if (item != null) {
                IItemStack itemStack = new ItemStackImpl(item.getId(), item.getQuantity());
                mapping.put(slot, itemStack);
            }
        }

        return mapping;
    }

    /**
     * Creates a map of Runelite's equipment inventory slot index to the enum value.
     */
    private static final Map<Integer, EquipmentInventorySlot> ID_TO_SLOT = Arrays.stream(
            EquipmentInventorySlot.values()
    ).collect(
            Collectors.toMap(EquipmentInventorySlot::getSlotIdx, e -> e)
    );

    private Map<EquipmentInventorySlot, IItemStack> equipment;

    @Override
    public Map<EquipmentInventorySlot, IItemStack> GetEquipment() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return equipment;
    }

    /**
     * Serialization format: semicolon-delimited key-value pairs. keys are ints, values are b64-encoded item stacks.
     * keys and values are separated by a colon.
     * 1:{stack};2:{stack2}
     * @return The serialized string in the above format.
     */
    @Override
    public String SerializeString() {
        if (this == Deserializer) {
            throw new IllegalArgumentException("Attempted to access property on deserializer singleton.");
        }
        return equipment.entrySet().stream().map(
                entry -> (
                        entry.getKey().getSlotIdx()
                                + ":"
                                + Base64.getEncoder().encodeToString(entry.getValue().SerializeString().getBytes())
                )
        ).collect(Collectors.joining(";"));
    }

    /**
     * Serialization format: semicolon-delimited key-value pairs. keys are ints, values are b64-encoded item stacks.
     * keys and values are separated by a colon.
     * 1:{stack};2:{stack2}
     * @return The deserialized object of the string that was provided in the above format.
     */
    @Override
    public IEquipment DeserializeString(String serialized) {

        List<String> items = Arrays.stream(serialized.split(";", -1)).filter(s -> !s.isBlank()).collect(Collectors.toList());
        Map<EquipmentInventorySlot, IItemStack> itemMap = new HashMap<>();

        for (String item : items) {
            String[] kvp = item.split(":", -1);
            if (kvp.length != 2) {
                System.err.println("Equipment was not a key-value pair: " + item);
                throw new IllegalArgumentException("Corrupted equipment: " + serialized);
            }
            EquipmentInventorySlot key = ID_TO_SLOT.get(Integer.parseInt(kvp[0]));
            String value = new String(Base64.getDecoder().decode(kvp[1]));
            IItemStack deserializedValue = ItemStackImpl.Deserializer.DeserializeString(value);
            itemMap.put(key, deserializedValue);
        }

        return new EquipmentImpl(itemMap);
    }
}
