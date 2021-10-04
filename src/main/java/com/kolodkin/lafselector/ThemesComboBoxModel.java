/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kolodkin.lafselector;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.json.Json;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author alexa
 */
public class ThemesComboBoxModel extends AbstractListModel<String> implements ComboBoxModel<String> {

    private class ThemeInfo {

        String name;
        String className;
        String resource;

        ThemeInfo(String name, String className, String resource) {
            this.name = name;
            this.className = className;
            this.resource = resource;
        }
    }

    private static final String THEMES_PACKAGE = "/com/formdev/flatlaf/intellijthemes/themes/";
    private static final Logger logger = LogManager.getLogger();
    private final List<ThemeInfo> themes = new ArrayList<>();
    private Integer selected = null;

    public ThemesComboBoxModel() {

        // load themes.json
        List<Map<String, String>> json = null;
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("themes.json"), StandardCharsets.UTF_8)) {
            json = (List<Map<String, String>>) Json.parse(reader);
        } catch (IOException ex) {
            logger.catching(ex);
        }

        // add info about bundled themes
        if (json != null) {
            json.forEach(theme -> {
                String name = theme.get("name");
                String resource = theme.get("resource");
                String className = theme.get("class");

                if (name != null) {
                    themes.add(new ThemeInfo(name, className, resource));
                }
            });
        }

        if (themes.size() > 0) {
            selected = 0;
        }

        logger.info(String.format("%d bundled themes loaded.", themes.size()));
    }

    public void apply() {
        ThemeInfo theme = themes.get(selected);
        if (theme != null) {
            if (theme.className != null) {
                FlatAnimatedLafChange.showSnapshot();
                try {
                    UIManager.setLookAndFeel(theme.className);
                } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
                    logger.error("Failed to create '" + theme.className + "'.", ex);
                }
            } else {
                FlatAnimatedLafChange.showSnapshot();
                IntelliJTheme.setup(getClass().getResourceAsStream(THEMES_PACKAGE + theme.resource));
            }

            // update all components
            FlatLaf.updateUI();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }

    @Override
    public int getSize() {
        return themes.size();
    }

    @Override
    public String getElementAt(int index) {
        ThemeInfo theme = themes.get(index);
        return theme != null ? theme.name : "";
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (anItem instanceof Integer) {
            selected = (Integer) anItem;
            fireContentsChanged(this, -1, -1);
            apply();
        } else if (anItem instanceof String) {
            for (Integer index = 0; index < themes.size(); index++) {
                if (themes.get(index).name.equals((String) anItem)) {
                    selected = index;
                    fireContentsChanged(this, -1, -1);
                    apply();
                    break;
                }
            }
        }
    }

    @Override
    public Object getSelectedItem() {
        return selected == null ? null : themes.get(selected).name;
    }
}
