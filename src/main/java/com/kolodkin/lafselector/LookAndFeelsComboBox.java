package com.kolodkin.lafselector;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.*;
import java.awt.*;
import java.beans.*;
import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LookAndFeelsComboBox extends JComboBox<ThemeInfo> {

    private static final Logger logger = LogManager.getLogger();
    private final PropertyChangeListener lafListener = this::lafChanged;
    public static final String THEMES_PACKAGE = "/com/formdev/flatlaf/intellijthemes/themes/";

    public LookAndFeelsComboBox() {
        setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof ThemeInfo) {
                    return super.getListCellRendererComponent(list, ((ThemeInfo) value).name, index, isSelected, cellHasFocus);
                } else {
                    return super.getListCellRendererComponent(list, UIManager.getLookAndFeel().getName(), index, isSelected, cellHasFocus);
                }
            }
        });

        addActionListener(e -> {

            Object obj = getSelectedItem();
            if (obj instanceof ThemeInfo) {
                ThemeInfo themeInfo = (ThemeInfo) obj;
                if (themeInfo.ñlassName != null) {
                    if (!themeInfo.ñlassName.equals(UIManager.getLookAndFeel().getClass().getName())) {
                        FlatAnimatedLafChange.showSnapshot();
                        try {
                            UIManager.setLookAndFeel(themeInfo.ñlassName);
                        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
                            logger.error("Failed to create '" + themeInfo.ñlassName + "'.", ex);
                        }
                    }
                } else if (themeInfo.themeFile != null) {
                    FlatAnimatedLafChange.showSnapshot();
                    try {
                        if (themeInfo.themeFile.getName().endsWith(".properties")) {
                            FlatLaf.setup(new FlatPropertiesLaf(themeInfo.name, themeInfo.themeFile));
                        } else {
                            FlatLaf.setup(IntelliJTheme.createLaf(new FileInputStream(themeInfo.themeFile)));
                        }

                        //DemoPrefs.getState().put(DemoPrefs.KEY_LAF_THEME, DemoPrefs.FILE_PREFIX + themeInfo.themeFile);
                    } catch (IOException ex) {
                        logger.error("Failed to load '" + themeInfo.themeFile + "'.", ex);
                    }

                } else {
                    FlatAnimatedLafChange.showSnapshot();

                    IntelliJTheme.setup(getClass().getResourceAsStream(THEMES_PACKAGE + themeInfo.resourceName));
                    //DemoPrefs.getState().put( DemoPrefs.KEY_LAF_THEME, DemoPrefs.RESOURCE_PREFIX + themeInfo.resourceName );

                }
            }

            // update all components
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
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
            if (className.equals(model.getElementAt(i).getClassName())) {
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
