package core.settings;

import core.assets.AssetManager;
import core.rendering_api.Debug;
import core.utils.FileManager;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Settings {

    static {
        settings = new ArrayList<>();
        settingTokens = loadSettingsFile();
        registerSettingsEnums(CoreFloatSettings.class, CoreKeySettings.class, CoreToggleSettings.class, CoreOptionSettings.class);
        registerSettings(List.of(new AssetPackSetting()));
    }

    @SafeVarargs
    public static void registerSettingsEnums(Class<? extends Setting>... settings) {
        for (Class<? extends Setting> setting : settings) registerSettingsEnum(setting);
    }

    @SafeVarargs
    public static void registerSettings(List<Setting>... settings) {
        for (List<Setting> settingList : settings) {
            Settings.settings.addAll(settingList);
            initSettings(settingList);
        }
    }

    public static void loadFromFile() {
        settingTokens = loadSettingsFile();
        initSettings(settings);
    }

    public static void writeToFile() {
        File file = FileManager.loadAndCreateFile(Path.of("Settings"));
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath()))) {
            for (Setting setting : settings) writer.write("%s:%s%n".formatted(setting.name(), setting.toSaveValue()));
        } catch (Exception exception) {
            exception.printStackTrace();
            Debug.err("Failed to save Settings to File");
        }
    }

    public static Setting getSettingWithName(String name) {
        for (Setting setting : settings) if (setting.name().equalsIgnoreCase(name)) return setting;
        return null;
    }

    public static ArrayList<Setting> getSettings() {
        return settings;
    }

    private static String[][] loadSettingsFile() {
        String[] settingsFileContents = FileManager.readAllLines(Path.of("Settings"));
        String[][] settings = new String[settingsFileContents.length][0];
        for (int index = 0; index < settingsFileContents.length; index++) settings[index] = settingsFileContents[index].split(":");
        return settings;
    }

    private static void registerSettingsEnum(Class<? extends Setting> settings) {
        if (!settings.isEnum()) throw new IllegalArgumentException("Argument must be an Enum");
        List<Setting> settingList = List.of(settings.getEnumConstants());
        Settings.settings.addAll(settingList);
        initSettings(settingList);
    }

    private static void initSettings(Iterable<Setting> settings) {
        for (String[] tokens : settingTokens)
            for (Setting setting : settings) {
                if (tokens.length != 2) continue;
                try {
                    if (setting.setIfPresent(tokens[0], tokens[1])) break;
                } catch (Exception _) {

                }
            }
    }

    private static final ArrayList<Setting> settings;
    private static String[][] settingTokens;

    private static class AssetPackSetting implements Setting {

        @Override
        public String name() {
            return "ASSET_PACKS";
        }

        @Override
        public String toSaveValue() {
            ArrayList<String> activePacks = AssetManager.getActiveAssetPackNames();
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < activePacks.size(); index++) {
                builder.append(activePacks.get(index));
                if (index != activePacks.size() - 1) builder.append('#');
            }
            return builder.toString();
        }

        @Override
        public boolean setIfPresent(String name, String value) {
            if (!name().equalsIgnoreCase(name)) return false;
            String[] packsArray = value.split("#");
            ArrayList<String> packsList = new ArrayList<>(packsArray.length);
            packsList.addAll(Arrays.asList(packsArray));
            packsList.removeIf(packName -> !Path.of("assetPacks", packName).toFile().exists());
            AssetManager.setActiveAssetPackNames(packsList);
            return true;
        }

        @Override
        public String translationFileName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int ordinal() {
            throw new UnsupportedOperationException();
        }
    }
}
