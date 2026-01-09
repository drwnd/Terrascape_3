package core.settings;

import core.settings.optionSettings.Option;
import core.utils.FileManager;

import java.io.*;

public final class Settings {

    public static void update(FloatSetting setting, float value) {
        setting.setValue(value);
    }

    public static void update(KeyBound setting, int value) {
        setting.setKeybind(value);
    }

    public static void update(ToggleSetting setting, boolean value) {
        setting.setValue(value);
    }

    public static void update(OptionSetting setting, Option value) {
        setting.setValue(value);
    }

    public static void loadFromFile() {
        File file = new File(SETTINGS_FILE_LOCATION);
        String[] lines = FileManager.readAllLines(file);

        for (String line : lines) {
            String[] tokens = line.split(":");
            if (tokens.length != 2) continue;
            String name = tokens[0], value = tokens[1];

            FloatSetting.setIfPresent(name, value);
            KeySetting.setIfPresent(name, value);
            ToggleSetting.setIfPresent(name, value);
            OptionSetting.setIfPresent(name, value);
        }
    }

    public static void writeToFile() {
        File file = FileManager.loadAndCreateFile(SETTINGS_FILE_LOCATION);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath()));

            for (FloatSetting setting : FloatSetting.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));
            for (KeySetting setting : KeySetting.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.keybind()));
            for (ToggleSetting setting : ToggleSetting.values()) writer.write("%s:%s_%s%n".formatted(setting.name(), setting.value(), setting.keybind()));
            for (OptionSetting setting : OptionSetting.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));

            writer.close();

        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Failed to save Settings to File");
        }
    }

    public static Enum<?> getSettingWithName(String name) {
        for (Enum<?> setting : FloatSetting.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        for (Enum<?> setting : KeySetting.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        for (Enum<?> setting : ToggleSetting.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        for (Enum<?> setting : OptionSetting.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        return null;
    }

    private static final String SETTINGS_FILE_LOCATION = "assets/textData/Settings";
}
