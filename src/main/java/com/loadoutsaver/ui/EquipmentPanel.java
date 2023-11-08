package com.loadoutsaver.ui;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class EquipmentPanel extends JPanel {

    private BufferedImage image;
    private final BufferedImage defaultImage;
    private final BufferedImage plainImage;

    private JLabel label = new JLabel();

    public EquipmentPanel(String defaultBackground) {
        super();
        this.setLayout(new GridLayout(1, 1));
        BufferedImage plainImage1;
        this.setOpaque(false);

        if (!defaultBackground.contains(".")) {
            defaultBackground = defaultBackground + ".png";
        }

        synchronized (ImageIO.class) {
            try {
                image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(defaultBackground)));
            }
            catch (IOException | NullPointerException ignored) {
                System.out.println("Could not find equipment background: " + defaultBackground);
                image = new BufferedImage(54, 54, BufferedImage.TYPE_INT_ARGB);
            }
            try {
                plainImage1 = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("emptyequipment.png")));
            }
            catch (IOException | NullPointerException ignored) {
                System.out.println("Could not find equipment background: " + defaultBackground);
                plainImage1 = new BufferedImage(54, 54, BufferedImage.TYPE_INT_ARGB);
            }
        }

        plainImage = plainImage1;
        defaultImage = image;
        this.add(label);

        this.setBorder(new EmptyBorder(3, 3, 3, 3));
    }

    public JLabel GetNewLabel(boolean reset) {

        this.removeAll();
        label = new JLabel();
        if (reset) {
            image = defaultImage;
        }
        else {
            image = plainImage;
        }
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setAlignmentY(CENTER_ALIGNMENT);
        this.add(label);

        this.revalidate();
        this.repaint();

        return label;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null);
    }

}
