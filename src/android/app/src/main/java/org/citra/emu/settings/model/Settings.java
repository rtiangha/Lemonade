package org.citra.emu.settings.model;

import android.text.TextUtils;

import org.citra.emu.CitraApplication;
import org.citra.emu.R;
import org.citra.emu.settings.SettingsActivity;
import org.citra.emu.settings.SettingsFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Settings {
    public static final String SECTION_INTERFACE = "Interface";
    public static final String SECTION_CORE = "Core";
    public static final String SECTION_SYSTEM = "System";
    public static final String SECTION_CAMERA = "Camera";
    public static final String SECTION_CONTROLS = "Controls";
    public static final String SECTION_RENDERER = "Renderer";
    public static final String SECTION_LAYOUT = "Layout";
    public static final String SECTION_UTILITY = "Utility";
    public static final String SECTION_AUDIO = "Audio";
    public static final String SECTION_DEBUG = "Debug";

    private String gameId;

    private static final Map<String, List<String>> configFileSectionsMap = new HashMap<>();

    static {
        configFileSectionsMap.put(SettingsFile.FILE_NAME_CONFIG, Arrays.asList(SECTION_INTERFACE, SECTION_CORE, SECTION_SYSTEM, SECTION_CAMERA, SECTION_CONTROLS, SECTION_RENDERER, SECTION_LAYOUT, SECTION_UTILITY, SECTION_AUDIO, SECTION_DEBUG));
    }

    /**
     * A HashMap<String, SettingSection> that constructs a new SettingSection instead of returning null
     * when getting a key not already in the map
     */
    public static final class SettingsSectionMap extends HashMap<String, SettingSection> {
        @Override
        public SettingSection get(Object key) {
            if (!(key instanceof String)) {
                return null;
            }

            String stringKey = (String) key;

            if (!super.containsKey(stringKey)) {
                SettingSection section = new SettingSection(stringKey);
                super.put(stringKey, section);
                return section;
            }
            return super.get(key);
        }
    }

    private HashMap<String, SettingSection> sections = new Settings.SettingsSectionMap();

    public SettingSection getSection(String sectionName) {
        return sections.get(sectionName);
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }

    public HashMap<String, SettingSection> getSections() {
        return sections;
    }

    public void loadSettings(SettingsActivity activity) {
        sections = new Settings.SettingsSectionMap();
        loadCitraSettings(activity);

        if (!TextUtils.isEmpty(gameId)) {
            loadCustomGameSettings(gameId, activity);
        }
    }

    private void loadCitraSettings(SettingsActivity activity) {
        for (Map.Entry<String, List<String>> entry : configFileSectionsMap.entrySet()) {
            String fileName = entry.getKey();
            sections.putAll(SettingsFile.readFile(fileName, activity));
        }
    }

    private void loadCustomGameSettings(String gameId, SettingsActivity activity) {
        // custom game settings
        mergeSections(SettingsFile.readCustomGameSettings(gameId, activity));
    }

    private void mergeSections(HashMap<String, SettingSection> updatedSections) {
        for (Map.Entry<String, SettingSection> entry : updatedSections.entrySet()) {
            if (sections.containsKey(entry.getKey())) {
                SettingSection originalSection = sections.get(entry.getKey());
                SettingSection updatedSection = entry.getValue();
                originalSection.mergeSection(updatedSection);
            } else {
                sections.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void loadSettings(String gameId, SettingsActivity activity) {
        this.gameId = gameId;
        loadSettings(activity);
    }

    public void saveSettings(SettingsActivity activity) {
        if (TextUtils.isEmpty(gameId)) {
            activity.showToastMessage(CitraApplication.getAppContext().getString(R.string.ini_saved), false);

            for (Map.Entry<String, List<String>> entry : configFileSectionsMap.entrySet()) {
                String fileName = entry.getKey();
                List<String> sectionNames = entry.getValue();
                TreeMap<String, SettingSection> iniSections = new TreeMap<>();
                for (String section : sectionNames) {
                    iniSections.put(section, sections.get(section));
                }

                SettingsFile.saveFile(fileName, iniSections, activity);
            }
        } else {
            // custom game settings
            activity.showToastMessage(CitraApplication.getAppContext().getString(R.string.gameid_saved, gameId), false);

            SettingsFile.saveCustomGameSettings(gameId, sections);
        }
    }
}