package com.loadoutsaver.ui;

import com.loadoutsaver.interfaces.IEquipment;
import com.loadoutsaver.interfaces.IItemStack;
import net.runelite.api.EquipmentInventorySlot;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Map;

public class TotalEquipmentPanel extends JPanel {

    private final EquipmentPanel headPanel = new EquipmentPanel("helmetdefault");
    private final EquipmentPanel capePanel = new EquipmentPanel("capedefault");
    private final EquipmentPanel amuletPanel = new EquipmentPanel("amuletdefault");
    private final EquipmentPanel ammoPanel = new EquipmentPanel("ammodefault");
    private final EquipmentPanel weaponPanel = new EquipmentPanel("weapondefault");
    private final EquipmentPanel bodyPanel = new EquipmentPanel("bodydefault");
    private final EquipmentPanel shieldPanel = new EquipmentPanel("shielddefault");
    private final EquipmentPanel legsPanel = new EquipmentPanel("legsdefault");
    private final EquipmentPanel glovesPanel = new EquipmentPanel("glovesdefault");
    private final EquipmentPanel bootsPanel = new EquipmentPanel("bootsdefault");
    private final EquipmentPanel ringPanel = new EquipmentPanel("ringdefault");

    private JComponent[] GetEquipmentGrid() {
        return new JComponent[] {
                new JLabel(), headPanel, new JLabel(),
                capePanel, amuletPanel, ammoPanel,
                weaponPanel, bodyPanel, shieldPanel,
                new JLabel(), legsPanel, new JLabel(),
                glovesPanel, bootsPanel, ringPanel
        };
    }

    private final JPanel equipmentPanel;

    public TotalEquipmentPanel(LoadoutSaverPanel panel, IEquipment equipment) {
        JPanel result = panel.PanelWithBackground("equipmentbackgroundgrid.png");
        // This is based on the properties of the equipment grid.
        result.setBorder(new EmptyBorder(4, 23, 8, 23)); // 16 left looks good
        result.setLayout(new GridLayout(5, 3));

        Map<EquipmentInventorySlot, IItemStack> equipmentMap = equipment.GetEquipment();
        for (EquipmentInventorySlot slot : equipmentMap.keySet()) {
            IItemStack itemStack = equipmentMap.get(slot);
            EquipmentPanel p;
            switch (slot) {
                case HEAD:
                    p = headPanel;
                    break;
                case CAPE:
                    p = capePanel;
                    break;
                case AMULET:
                    p = amuletPanel;
                    break;
                case AMMO:
                    p = ammoPanel;
                    break;
                case WEAPON:
                    p = weaponPanel;
                    break;
                case BODY:
                    p = bodyPanel;
                    break;
                case SHIELD:
                    p = shieldPanel;
                    break;
                case LEGS:
                    p = legsPanel;
                    break;
                case GLOVES:
                    p = glovesPanel;
                    break;
                case BOOTS:
                    p = bootsPanel;
                    break;
                case RING:
                    p = ringPanel;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown equipment: " + slot);
            }

            JLabel label = p.GetNewLabel(false);

            panel.AddItemImageToLabel(label, itemStack);
        }

        for (Component c : GetEquipmentGrid()) {
            result.add(c);
        }

        result.setAlignmentX(CENTER_ALIGNMENT);
        equipmentPanel = result;
    }

    JPanel GetPanel() {
        return equipmentPanel;
    }
}
