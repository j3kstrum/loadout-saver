package com.loadoutsaver;

import com.loadoutsaver.implementations.LoadoutImpl;
import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.IItemStack;
import com.loadoutsaver.interfaces.ILoadout;
import com.loadoutsaver.interfaces.ISubscriber;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.AsyncBufferedImage;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class LoadoutSaverPanel extends PluginPanel implements ISubscriber<Stream<ILoadout>> {

    private LoadoutManager manager;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ItemManager itemManager;

    public void setManager(LoadoutManager manager) {
        if (this.manager != null) {
            this.manager.UnSubscribe(this);
        }
        this.manager = manager;
        this.manager.Subscribe(this);
    }

    private final List<Component> components = new ArrayList<>();

    @Override
    public Component add(Component c) {
        this.components.add(c);
        return super.add(c);
    }

    private JTextField loadoutName = null;

    @Override
    public void Update(Stream<ILoadout> updatedObject) {

        String storedName = "My Custom Loadout";
        if (loadoutName != null) {
            storedName = loadoutName.getText();
        }

        for (Component c : components) {
            this.remove(c);
        }
        this.components.clear();

        this.add(new JLabel("Custom loadout name:"));

        loadoutName = new JTextField(storedName);
        this.add(loadoutName);

        JButton addButton = new JButton("Add current loadout");
        addButton.addActionListener(
                ae -> {
                    if (client.getGameState() == GameState.LOGGED_IN) {
                        try {
                            this.manager.AddLoadout(new LoadoutImpl(loadoutName.getText(), client));
                            addButton.setText("Add current loadout");
                        }
                        catch (IllegalArgumentException iae) {
                            if (iae.getMessage().contains("Client state was unexpected")) {
                                return;
                            }
                            if (iae.getMessage().contains("Name contained illegal characters")) {
                                addButton.setText("Add current loadout. [!] : not allowed");
                                return;
                            }
                            throw iae;
                        }
                    }
                    else {
                        addButton.setText("Add current loadout. [!] Must log in");
                    }
                });
        this.add(addButton);

        JButton saveButton = new JButton("Manually Save Loadouts");
        saveButton.addActionListener(al -> manager.save());
        this.add(saveButton);

        updatedObject.map(this::AsComponent).forEach(this::add);

        this.revalidate();
        this.repaint();
    }

    private Component AsComponent(ILoadout loadout) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        // Show the loadout's name.
        panel.add(new JLabel(loadout.GetName()));

        // Display the loadout images.
        panel.add(AsComponent(loadout.GetEquipment()));
        panel.add(AsComponent(loadout.GetInventory()));

        // "Remove" button for removing a loadout.
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(ae -> manager.RemoveLoadout(loadout));
        panel.add(removeButton);

        // Share code label for displaying the share code.
        JLabel shareCode = new JLabel("");
        panel.add(shareCode);

        // Button to show/hide the share code.
        JButton showShareCode = new JButton("Show share code");
        showShareCode.addActionListener(
                ae -> {
                    if (shareCode.getText().isEmpty()) {
                        shareCode.setText("Share code: " + loadout.SerializeString());
                        showShareCode.setText("Hide share code");
                    }
                    else {
                        shareCode.setText("");
                        showShareCode.setText("Show share code");
                    }
                }
        );
        panel.add(showShareCode);

        JButton copyButton = new JButton("Copy share code to clipboard");
        copyButton.addActionListener(
                ae -> {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                            new StringSelection(loadout.SerializeString()),
                            null
                    );
                    copyButton.setText("Copy share code to clipboard (Copied at " + LocalDateTime.now() + ")");
                }
        );
        panel.add(copyButton);

        return panel;
    }

    private Component AsComponent(IEquipment equipment) {
        JPanel result = new JPanel();

        Map<EquipmentInventorySlot, IItemStack> equipmentMap = equipment.GetEquipment();
        for (EquipmentInventorySlot slot : equipmentMap.keySet()) {
            // TODO: Put in the right place.
            IItemStack itemStack = equipmentMap.get(slot);
            result.add(AsComponent(itemStack));
        }

        return result;
    }

    private Component AsComponent(IInventory inventory) {
        int columns = 4;
        JPanel result = new JPanel(new GridLayout(0, columns));
        // TODO: This could be drawn nicer, with the border and all.

        for (IItemStack itemStack : inventory.GetItems()) {

            // For our inventory, location is important.
            if (itemStack.ItemID() < 0) {
                // Empty slot.
                result.add(new JLabel());
            }
            else {
                result.add(AsComponent(itemStack));
            }
        }

        return result;
    }

    private Component AsComponent(IItemStack itemStack) {

        JLabel label = new JLabel();

        // We can't pull the item information without going to the client thread.
        // So we do this asynchronously, whenever available, and we add the final image later on.
        // Prevents the client from lagging - but quantities on stackable images might take some time to show up.
        AsyncBufferedImage image;
        if (itemStack.Quantity() > 1) {
            // We trust our data (which claims it is naturally stackable), and then verify later.
            image = itemManager.getImage(itemStack.ItemID(), itemStack.Quantity(), true);
        }
        else {
            // It looks less weird to have an air rune with no quantity than a sword with a 1 quantity.
            // So we avoid quantities for now, and verify later.
            image = itemManager.getImage(itemStack.ItemID(), itemStack.Quantity(), false);
        }
        image.addTo(label);

        // Finally, cross-verify later on when the client thread is available.
        clientThread.invokeLater(() -> AddItemImageToLabel(label, itemStack));

        return label;
    }

    private void AddItemImageToLabel(JLabel label, IItemStack item) {
        ItemComposition composition;
        FutureTask<ItemComposition> getComposition = new FutureTask<>(() -> client.getItemDefinition(item.ItemID()));
        clientThread.invoke(getComposition);
        try {
            composition = getComposition.get();
        }
        catch (ExecutionException | InterruptedException e) {
            // Couldn't get the composition.
            return;
        }
        AsyncBufferedImage image = itemManager.getImage(item.ItemID(), item.Quantity(), composition.isStackable());
        image.addTo(label);
    }
}
