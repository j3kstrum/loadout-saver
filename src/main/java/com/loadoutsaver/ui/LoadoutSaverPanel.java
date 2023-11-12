package com.loadoutsaver.ui;

import com.loadoutsaver.LoadoutManager;
import com.loadoutsaver.implementations.LoadoutImpl;
import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IInventory;
import com.loadoutsaver.interfaces.IItemStack;
import com.loadoutsaver.interfaces.ILoadout;
import com.loadoutsaver.interfaces.ISubscriber;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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

    /**
     * Sets the active loadout manager. In practice this is only called once when loading completes.
     * @param manager The loadout manager to associate with this loadout panel.
     */
    public void setManager(LoadoutManager manager) {
        if (this.manager != null) {
            this.manager.UnSubscribe(this);
        }
        this.manager = manager;
        this.manager.Subscribe(this);
    }

    // Had some troubles with rendering and clearing components, so just created this array for tracking...
    private final List<Component> components = new ArrayList<>();

    @Override
    public Component add(Component c) {
        this.components.add(c);
        return super.add(c);
    }

    private void AlignAdd(JComponent c) {
        c.setAlignmentX(CENTER_ALIGNMENT);
        c.setPreferredSize(new Dimension(INNER_WIDTH, c.getPreferredSize().height));
        this.add(c);
    }

    private JTextField loadoutName = null;

    private static final int PADDING = 5;
    private static final int INNER_WIDTH = PANEL_WIDTH - (2 * PADDING);

    @Override
    public void Update(Stream<ILoadout> updatedObject) {

        this.getScrollPane().getVerticalScrollBar().setUnitIncrement(16);
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
                    // Adds the player's current loadout to the loadout list, plus some error handling.
                    if (client.getGameState() == GameState.LOGGED_IN) {
                        try {
                            this.manager.AddLoadout(new LoadoutImpl(loadoutName.getText(), client));
                            addButton.setText("Add current loadout");
                        }
                        catch (IllegalArgumentException iae) {
                            if (iae.getMessage().contains("Client state was unexpected")) {
                                System.out.println(iae.getMessage());
                                return;
                            }
                            if (iae.getMessage().contains("Name contained illegal characters")) {
                                addButton.setText("Add current loadout. [!] name not allowed");
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

        JButton saveButton = new JButton("Manually save loadouts");
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

        // Allow for loading loadouts based on a share code.
        // This is just the serialized loadout string (see LoadoutImpl::SerializeString).
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

        // Lastly, add all of the rendered loadouts underneath.
        updatedObject.map(this::AsComponent).forEach(this::AlignAdd);

        this.revalidate();
        this.repaint();
    }

    /**
     * Renders a loadout as a JComponent.
     * @param loadout The loadout to be rendered.
     * @return The JComponent rendering of the loadout.
     */
    private JComponent AsComponent(ILoadout loadout) {
        JPanel panel = new JPanel();
        // Pad top and bottom to have some space between other components; arrange everything in one column.
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
        removeButton.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(removeButton);

        // Copy button to place the share code (serialized string) for the given loadout on the player's clipboard.
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

    /**
     * Creates a JPanel that has the provided background image.
     * @param backgroundReference The string reference to the background image resource.
     * @return A JPanel with the loaded image as its background.
     */
    JPanel PanelWithBackground(String backgroundReference) {

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

    /**
     * Equipment was hard to render, so it gets its own class for the component.
     * @param equipment The equipment set to be drawn as a component.
     * @return A JComponent that draws the given equipment in a nice way on the screen.
     */
    private JComponent AsComponent(IEquipment equipment) {
        return new TotalEquipmentPanel(this, equipment).GetPanel();
    }

    /**
     * Renders the player's inventory into a nice grid on the screen.
     * @param inventory The player's inventory to draw on the screen.
     * @return A JComponent that renders the given inventory in a nice way on the screen.
     */
    private JComponent AsComponent(IInventory inventory) {
        int columns = 4;
        JPanel result = PanelWithBackground("inventorybackground.png");
        int rows = (int) Math.ceil(((double) inventory.GetItems().length) / columns);
        result.setLayout(new GridLayout(rows, columns));
        result.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // We remove padding to find out the remaining size.
        int componentWidth = (result.getWidth() - 2 * PADDING) / columns;
        int componentHeight = (result.getHeight() - 2 * PADDING) / rows;

        for (IItemStack itemStack : inventory.GetItems()) {
            JComponent c;

            // For our inventory, location is important.
            if (itemStack.ItemID() < 0) {
                // Empty slot.
                c = new JLabel();
            }
            else {
                c = AsComponent(itemStack);
            }
            c.setBorder(new EmptyBorder(3, 3, 3, 3));
            c.setPreferredSize(new Dimension(componentWidth - 2 * 3, componentHeight - 2 * 3));
            result.add(c);
        }

        return result;
    }

    private JComponent AsComponent(IItemStack itemStack) {
        JLabel label = new JLabel();
        AddItemImageToLabel(label, itemStack);
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setAlignmentY(CENTER_ALIGNMENT);
        return label;
    }

    void AddItemImageToLabel(JLabel label, IItemStack itemStack) {
        // We can't pull the item information without going to the client thread.
        // So we do this asynchronously, whenever available, and we add the final image later on.
        // Prevents the client from lagging - but quantities on stackable images might take some time to show up.
        AsyncBufferedImage image;
        // We can make a quick inference and then correct ourselves later.
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
        clientThread.invokeLater(() -> AddItemImageToLabelClientThread(label, itemStack));
    }

    /**
     * This function must run on the client thread. It reads from the item definition for the item
     * in order to determine if the item is actually stackable or not.
     * This allows us to correct our initially rendered image, which is our best guess.
     * @param label The label onto which to draw the final image as an icon.
     * @param item The item stack for which we would like to obtain the image to draw.
     */
    private void AddItemImageToLabelClientThread(JLabel label, IItemStack item) {
        ItemComposition composition;
        FutureTask<ItemComposition> getComposition = new FutureTask<>(() -> client.getItemDefinition(item.ItemID()));
        clientThread.invoke(getComposition);
        try {
            composition = getComposition.get();
        }
        catch (ExecutionException | InterruptedException e) {
            // Couldn't get the composition.
            // Just take our initial image; return.
            return;
        }
        AsyncBufferedImage image = itemManager.getImage(item.ItemID(), item.Quantity(), composition.isStackable());
        image.addTo(label);
    }
}
