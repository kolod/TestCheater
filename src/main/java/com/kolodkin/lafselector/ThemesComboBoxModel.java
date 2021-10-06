// This file is part of TestCheater.
// Copyright (C) 2021 - ... Oleksandr Kolodkin <alexandr.kolodkin@gmail.com>
//
// TestCheater is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Foobar is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
package com.kolodkin.lafselector;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.file.*;
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

        ThemeInfo() {
        }
    }

    private static final String THEMES_PACKAGE = "/com/formdev/flatlaf/intellijthemes/themes/";
    private static final Logger logger = LogManager.getLogger();
    private List<ThemeInfo> themes = new ArrayList<>();
    private Integer selected = null;

    public ThemesComboBoxModel() {
        Type listType = new TypeToken<ArrayList<ThemeInfo>>() {
        }.getType();
        Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("themes.json"));
        themes = new Gson().fromJson(reader, listType);
        selected = themes.size() > 0 ? 0 : null;
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
            logger.info(String.format("Set theme: %s.", theme.name));
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
            apply();
        } else if (anItem instanceof String) {
            for (Integer index = 0; index < themes.size(); index++) {
                if (themes.get(index).name.equals((String) anItem)) {
                    selected = index;
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
