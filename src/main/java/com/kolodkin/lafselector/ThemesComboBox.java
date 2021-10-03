package com.kolodkin.lafselector;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThemesComboBox extends JComboBox<ThemeInfo> {

    private static final Logger logger = LogManager.getLogger();
    private final PropertyChangeListener lafListener = this::lafChanged;
    public static final String THEMES_PACKAGE = "/com/formdev/flatlaf/intellijthemes/themes/";

    public ThemesComboBox() {
        setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof ThemeInfo) {
                    String[] parts = ((ThemeInfo) value).name.split("/");
                    return super.getListCellRendererComponent(list, parts[parts.length - 1].trim(), index, isSelected, cellHasFocus);
                } else {
                    return super.getListCellRendererComponent(list, UIManager.getLookAndFeel().getName(), index, isSelected, cellHasFocus);
                }
            }
        });

        addActionListener((ActionEvent e) -> {
            Object obj = getSelectedItem();
            if (obj instanceof ThemeInfo) {
                ((ThemeInfo) obj).apply();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        selectedCurrentLookAndFeel();
        UIManager.addPropertyChangeListener(lafListener);
    }

    void lafChanged(PropertyChangeEvent e) {
        if ("lookAndFeel".equals(e.getPropertyName())) {
            selectedCurrentLookAndFeel();
        }
    }

    private void selectedCurrentLookAndFeel() {
        setSelectedLookAndFeel(UIManager.getLookAndFeel().getClass().getName());
    }

    private void setSelectedLookAndFeel(String className) {
        setSelectedIndex(getIndexOfLookAndFeel(className));
    }

    private int getIndexOfLookAndFeel(String className) {
        ComboBoxModel<ThemeInfo> model = getModel();
        int size = model.getSize();
        for (int i = 0; i < size; i++) {
            if (className.equals(model.getElementAt(i).ñlassName)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        UIManager.removePropertyChangeListener(lafListener);
    }
}
