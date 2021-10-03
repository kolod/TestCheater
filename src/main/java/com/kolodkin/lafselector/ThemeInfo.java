/*
 * Copyright 2019 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kolodkin.lafselector;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import com.formdev.flatlaf.IntelliJTheme;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import static com.kolodkin.lafselector.ThemesComboBox.THEMES_PACKAGE;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThemeInfo {

    final String name;
    final String resourceName;
    final String ñlassName;

    private static final Logger logger = LogManager.getLogger();

    /**
     *
     * @param name
     * @param resourceName
     * @param ñlassName
     */
    public ThemeInfo(String name, String resourceName, String ñlassName) {
        this.name = name;
        this.resourceName = resourceName;
        this.ñlassName = ñlassName;
    }

    /**
     *
     * @param s
     */
    public ThemeInfo(String s) {
        String[] array = s.split(":");
        if (array.length == 3) {
            switch (array[0]) {
                case "class":
                    name = array[1];
                    resourceName = null;
                    ñlassName = array[2];
                    break;
                case "resource":
                    name = array[1];
                    resourceName = array[2];
                    ñlassName = null;
                    break;
                default:
                    name = null;
                    resourceName = null;
                    ñlassName = null;
                    throw new IllegalArgumentException("String must start with 'class:' or 'resource:'");
            }
        } else {
            name = null;
            resourceName = null;
            ñlassName = null;
            throw new IllegalArgumentException("The string must have three parts separated by ':'.");
        }
    }

    public void apply() {
        if (ñlassName != null) {
            FlatAnimatedLafChange.showSnapshot();
            try {
                UIManager.setLookAndFeel(ñlassName);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
                logger.error("Failed to create '" + ñlassName + "'.", ex);
            }
        } else {
            FlatAnimatedLafChange.showSnapshot();
            IntelliJTheme.setup(getClass().getResourceAsStream(THEMES_PACKAGE + resourceName));
        }

        // update all components
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    @Override
    public String toString() {
        if (ñlassName != null) {
            return String.format("class:%s:%s", name, ñlassName);
        } else if (resourceName != null) {
            return String.format("resource:%s:%s", name, resourceName);
        } else {
            return "";
        }
    }
}
