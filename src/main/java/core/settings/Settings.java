package core.settings;

import core.settings.optionSettings.Option;
import core.utils.FileManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public final class Settings {

    static {
        settings = new ArrayList<>();
        registerSettingsEnums(CoreFloatSettings.class, CoreKeySettings.class, CoreToggleSettings.class, CoreOptionSettings.class);
    }

    public static void registerSettingsEnums(Class<?>... settings) {
        for (Class<?> setting : settings) registerSettingsEnum(setting);
    }

    public static void update(FloatSetting setting, float value) {
        setting.setValue(value);
    }

    public static void update(IntSetting setting, int value) {
        setting.setValue(value);
    }

    public static void update(KeySetting setting, int value) {
        setting.setKeybind(value);
    }

    public static void update(ToggleSetting setting, boolean value) {
        setting.setValue(value);
    }

    public static void update(OptionSetting setting, Option value) {
        setting.setValue(value);
    }

    public static <T extends Number> void update(NumberSetting<T> setting, Number value) {
        switch (value) {
            case Integer intValue -> update((IntSetting) setting, (int) intValue);
            case Float floatValue -> update((FloatSetting) setting, (float) floatValue);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    public static void loadFromFile() {
        File file = new File(SETTINGS_FILE_LOCATION);
        String[] lines = FileManager.readAllLines(file);

        lines:
        for (String line : lines) {
            String[] tokens = line.split(":");
            if (tokens.length != 2) continue;
            String name = tokens[0], value = tokens[1];

            for (Setting setting : settings) if (setting.setIfPresent(name, value)) continue lines;
        }
    }

    public static void writeToFile() {
        File file = FileManager.loadAndCreateFile(SETTINGS_FILE_LOCATION);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath()));
            for (Setting setting : settings) writer.write("%s:%s%n".formatted(setting.name(), setting.toSaveValue()));
            writer.close();

        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Failed to save Settings to File");
        }
    }

    public static Setting getSettingWithName(String name) {
        for (Setting setting : settings) if (setting.name().equalsIgnoreCase(name)) return setting;
        return null;
    }

    public static ArrayList<Setting> getSettings() {
        return settings;
    }

    private static void registerSettingsEnum(Class<?> settings) {
        if (!settings.isEnum() || !(settings.getEnumConstants()[0] instanceof Setting))
            throw new IllegalArgumentException("Argument must be an Enum implementing Setting");
        Collections.addAll(Settings.settings, (Setting[]) settings.getEnumConstants());
    }

    private static final ArrayList<Setting> settings;
    private static final String SETTINGS_FILE_LOCATION = "assets/textData/Settings";
}
