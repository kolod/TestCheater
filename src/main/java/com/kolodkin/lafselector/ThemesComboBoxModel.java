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
        themes.addAll(loadThemesFromDirectory());
    }

    private List<ThemeInfo> loadCoreThemes() {
        List<ThemeInfo> coreThemes = new ArrayList<>();
        coreThemes.add(new ThemeInfo("Flat Light", null, false, null, FlatLightLaf.class.getName()));
        coreThemes.add(new ThemeInfo("Flat Dark", null, true, null, FlatDarkLaf.class.getName()));
        coreThemes.add(new ThemeInfo("Flat IntelliJ", null, false, null, FlatIntelliJLaf.class.getName()));
        coreThemes.add(new ThemeInfo("Flat Darcula", null, true, null, FlatDarculaLaf.class.getName()));
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
            boolean dark = Boolean.parseBoolean(value.get("dark"));
            bundledThemes.add(new ThemeInfo(name, resourceName, dark, null, null));
        });

        bundledThemes.sort(comparator);
        return bundledThemes;
    }

    private List<ThemeInfo> loadThemesFromDirectory() {
        List<ThemeInfo> moreThemes = new ArrayList<>();

        // get current working directory
        File directory = new File("").getAbsoluteFile();

        File[] themeFiles = directory.listFiles((dir, name) -> {
            return name.endsWith(".theme.json") || name.endsWith(".properties");
        });

        if (themeFiles == null) {
            return moreThemes;
        }

        lastModifiedMap.clear();
        lastModifiedMap.put(directory, directory.lastModified());

        moreThemes.clear();
        for (File f : themeFiles) {
            String fname = f.getName();
            String name = fname.endsWith(".properties")
                    ? StringUtils.removeTrailing(fname, ".properties")
                    : StringUtils.removeTrailing(fname, ".theme.json");
            moreThemes.add(new ThemeInfo(name, null, false, f, null));
            lastModifiedMap.put(f, f.lastModified());
        }

        moreThemes.sort(comparator);
        return moreThemes;
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
