package com.loadoutsaver;

import com.loadoutsaver.implementations.LoadoutImpl;
import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.IItemStack;
import com.loadoutsaver.interfaces.ILoadout;
import com.loadoutsaver.interfaces.ISubscriber;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private void AlignAdd(JComponent c) {
        c.setAlignmentX(CENTER_ALIGNMENT);
        this.add(c);
    }

    private JTextField loadoutName = null;

    private static final int PADDING = 10;

    @Override
    public void Update(Stream<ILoadout> updatedObject) {

        this.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        String storedName = "My Custom Loadout";
        if (loadoutName != null) {
            storedName = loadoutName.getText();
        }

        for (Component c : components) {
            this.remove(c);
        }
        this.components.clear();

        this.AlignAdd(new JLabel("Custom loadout name:"));

        loadoutName = new JTextField(storedName);
        this.AlignAdd(loadoutName);

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
        this.AlignAdd(addButton);

        JButton saveButton = new JButton("Manually Save Loadouts");
        saveButton.addActionListener(
                al -> {
                    manager.save();
                    saveButton.setText(
                        "Saved at "
                        + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime())
                    );
                }
        );
        this.AlignAdd(saveButton);

        JTextField loadExternalField = new JTextField("Paste share code here to load");
        this.AlignAdd(loadExternalField);

        JButton loadButton = new JButton("Load from share code");
        loadButton.addActionListener(
                al -> {
                    try {
                        ILoadout loaded = LoadoutImpl.Deserializer.DeserializeString(loadExternalField.getText());
                        manager.AddLoadout(loaded);
                        loadExternalField.setText("Successfully loaded: " + loaded.GetName());
                    }
                    catch (IllegalArgumentException iae) {
                        loadExternalField.setText("[Invalid code] Paste share code here");
                    }
                }
        );
        this.AlignAdd(loadButton);

        updatedObject.map(this::AsComponent).forEach(this::AlignAdd);

        this.revalidate();
        this.repaint();
    }

    private JComponent AsComponent(ILoadout loadout) {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(PADDING, 0, PADDING, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        // Show the loadout's name.
        JLabel nameLabel = new JLabel(loadout.GetName());
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(nameLabel);

        // Display the loadout images.
        panel.add(AsComponent(loadout.GetEquipment()));
        JPanel padding = new JPanel();
        padding.setBorder(new EmptyBorder(PADDING, 0, 0, 0));
        padding.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(padding);
        panel.add(AsComponent(loadout.GetInventory()));

        // "Remove" button for removing a loadout.
        JButton removeButton = new JButton("Remove");
        removeButton.setAlignmentX(CENTER_ALIGNMENT);
        removeButton.addActionListener(
                ae -> {
                    if (Objects.equals(removeButton.getText(), "Click again to confirm delete")) {
                        manager.RemoveLoadout(loadout);
                    }
                    else {
                        removeButton.setText("Click again to confirm delete");
                    }
                }
        );
        panel.add(removeButton);

        // Share code label for displaying the share code.
        JLabel shareCode = new JLabel("");
        shareCode.setAlignmentX(CENTER_ALIGNMENT);
        // Temporarily disabled due to text being truncated.
        // panel.add(shareCode);

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
        showShareCode.setAlignmentX(CENTER_ALIGNMENT);
        // Temporarily disabled due to text being truncated.
        // panel.add(showShareCode);

        JButton copyButton = new JButton("Copy share code to clipboard");
        copyButton.addActionListener(
            ae -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(loadout.SerializeString()),
                    null
                );
                copyButton.setText(
                    "Copy code (Copied at "
                    + new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime())
                    + ")"
                );
            }
        );
        copyButton.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(copyButton);

        return panel;
    }

    private JPanel PanelWithBackground(String backgroundReference) {

        BufferedImage image;
        synchronized (ImageIO.class) {
            try {
                image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(backgroundReference)));
            }
            catch (IOException ioe) {
                return new JPanel();
            }
        }
        if (image == null) {
            return new JPanel();
        }

        Image resized;
        int targetWidth = PANEL_WIDTH - 2 * PADDING;
        int newHeight = (targetWidth * image.getHeight()) / image.getWidth();
        resized = image.getScaledInstance(targetWidth, newHeight, Image.SCALE_DEFAULT);

        JPanel result = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                g.drawImage(resized, 0, 0, null);
            }
        };
        result.setPreferredSize(new Dimension(targetWidth, newHeight));
        result.setAlignmentX(CENTER_ALIGNMENT);
        return result;
    }

    private JComponent AsComponent(IEquipment equipment) {
        JPanel result = PanelWithBackground("equipmentbackgroundgrid.png");
        // This is based on the properties of the equipment grid.
        result.setBorder(new EmptyBorder(0, 26, 0, 16));
        result.setLayout(new GridLayout(5, 3));
        Component[] components = new Component[15];
        for (int i = 0; i < 15; i++) {
            components[i] = new JLabel();
        }

        Map<EquipmentInventorySlot, IItemStack> equipmentMap = equipment.GetEquipment();
        for (EquipmentInventorySlot slot : equipmentMap.keySet()) {
            IItemStack itemStack = equipmentMap.get(slot);
            Component c = AsComponent(itemStack);
            switch (slot) {
                case HEAD:
                    components[1] = c;
                    break;
                case CAPE:
                    components[3] = c;
                    break;
                case AMULET:
                    components[4] = c;
                    break;
                case AMMO:
                    components[5] = c;
                    break;
                case WEAPON:
                    components[6] = c;
                    break;
                case BODY:
                    components[7] = c;
                    break;
                case SHIELD:
                    components[8] = c;
                    break;
                case LEGS:
                    components[10] = c;
                    break;
                case GLOVES:
                    components[12] = c;
                    break;
                case BOOTS:
                    components[13] = c;
                    break;
                case RING:
                    components[14] = c;
                    break;
            }
        }

        for (Component c : components) {
            result.add(c);
        }

        result.setAlignmentX(CENTER_ALIGNMENT);

        return result;
    }

    private JComponent AsComponent(IInventory inventory) {
        int columns = 4;
        JPanel result = PanelWithBackground("inventorybackground.png");
        int rows = (int) Math.ceil(((double) inventory.GetItems().length) / columns);
        result.setLayout(new GridLayout(rows, columns));
        result.setBorder(new EmptyBorder(PADDING, PADDING * 2, PADDING, PADDING));

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

    private JComponent AsComponent(IItemStack itemStack) {

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
