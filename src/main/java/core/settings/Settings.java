package core.settings;

import core.settings.optionSettings.Option;
import core.utils.FileManager;

import java.io.*;

public final class Settings {

    public static void update(CoreFloatSettings setting, float value) {
        setting.setValue(value);
    }

    public static void update(KeyBound setting, int value) {
        setting.setKeybind(value);
    }

    public static void update(CoreToggleSettings setting, boolean value) {
        setting.setValue(value);
    }

    public static void update(CoreOptionSettings setting, Option value) {
        setting.setValue(value);
    }

    public static void loadFromFile() {
        File file = new File(SETTINGS_FILE_LOCATION);
        String[] lines = FileManager.readAllLines(file);

        for (String line : lines) {
            String[] tokens = line.split(":");
            if (tokens.length != 2) continue;
            String name = tokens[0], value = tokens[1];

            CoreFloatSettings.setIfPresent(name, value);
            CoreKeySettings.setIfPresent(name, value);
            CoreToggleSettings.setIfPresent(name, value);
            CoreOptionSettings.setIfPresent(name, value);
        }
    }

    public static void writeToFile() {
        File file = FileManager.loadAndCreateFile(SETTINGS_FILE_LOCATION);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath()));

            for (CoreFloatSettings setting : CoreFloatSettings.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));
            for (CoreKeySettings setting : CoreKeySettings.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.keybind()));
            for (CoreToggleSettings setting : CoreToggleSettings.values()) writer.write("%s:%s_%s%n".formatted(setting.name(), setting.value(), setting.keybind()));
            for (CoreOptionSettings setting : CoreOptionSettings.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));

            writer.close();

        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Failed to save Settings to File");
        }
    }

    public static Enum<?> getSettingWithName(String name) {
        for (Enum<?> setting : CoreFloatSettings.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        for (Enum<?> setting : CoreKeySettings.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        for (Enum<?> setting : CoreToggleSettings.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        for (Enum<?> setting : CoreOptionSettings.values()) if (setting.name().equalsIgnoreCase(name)) return setting;
        return null;
    }

    private static final String SETTINGS_FILE_LOCATION = "assets/textData/Settings";
}
