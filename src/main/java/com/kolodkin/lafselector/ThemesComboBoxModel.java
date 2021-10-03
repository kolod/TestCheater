/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kolodkin.lafselector;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.json.Json;
import com.formdev.flatlaf.util.StringUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author alexa
 */
public class ThemesComboBoxModel extends AbstractListModel<ThemeInfo> implements ComboBoxModel<ThemeInfo> {

    private static final Logger logger = LogManager.getLogger();
    private static final Comparator<? super ThemeInfo> comparator = (t1, t2) -> t1.name.compareToIgnoreCase(t2.name);
    private final Map<File, Long> lastModifiedMap;
    private final List<ThemeInfo> themes;
    private ThemeInfo selected = null;

    public ThemesComboBoxModel() {
        selected = null;
        themes = new ArrayList<>();
        lastModifiedMap = new HashMap<>();        
        themes.addAll(loadCoreThemes());
        themes.addAll(loadBundledThemes());
    }

    private List<ThemeInfo> loadCoreThemes() {
        List<ThemeInfo> coreThemes = new ArrayList<>();
        coreThemes.add(new ThemeInfo("Flat Light", null, FlatLightLaf.class.getName()));
        coreThemes.add(new ThemeInfo("Flat Dark", null, FlatDarkLaf.class.getName()));
        coreThemes.add(new ThemeInfo("Flat IntelliJ", null, FlatIntelliJLaf.class.getName()));
        coreThemes.add(new ThemeInfo("Flat Darcula", null, FlatDarculaLaf.class.getName()));
        coreThemes.sort(comparator);
        return coreThemes;
    }

    private List<ThemeInfo> loadBundledThemes() {
        List<ThemeInfo> bundledThemes = new ArrayList<>();

        // load themes.json
        Map<String, Object> json;
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("themes.json"), StandardCharsets.UTF_8)) {
            json = (Map<String, Object>) Json.parse(reader);
        } catch (IOException ex) {
            logger.catching(ex);
            return bundledThemes;
        }

        // add info about bundled themes
        json.entrySet().forEach(e -> {
            String resourceName = e.getKey();
            Map<String, String> value = (Map<String, String>) e.getValue();
            String name = value.get("name");
            bundledThemes.add(new ThemeInfo(name, resourceName, null));
        });

        logger.info("" + bundledThemes.size() + " bundled themes loaded.");
        bundledThemes.sort(comparator);
        return bundledThemes;
    }

    boolean hasThemesFromDirectoryChanged() {
        return lastModifiedMap.entrySet().stream().anyMatch(e -> (e.getKey().lastModified() != e.getValue()));
    }

    @Override
    public int getSize() {
        return themes.size();
    }

    @Override
    public ThemeInfo getElementAt(int index) {
        return themes.get(index);
    }

    @Override
    public void setSelectedItem(Object anItem) {
        if (anItem instanceof ThemeInfo) {
            selected = (ThemeInfo) anItem;
        }
    }

    @Override
    public Object getSelectedItem() {
        return selected;
    }
}
