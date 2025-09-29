package core.settings;

import core.settings.optionSettings.Option;

import java.io.*;
import java.util.ArrayList;

public final class Settings {

    public static void update(FloatSetting setting, float value) {
        setting.setValue(value);
    }

    public static void update(KeySetting setting, int value) {
        setting.setValue(value);
    }

    public static void update(ToggleSetting setting, boolean value) {
        setting.setValue(value);
    }

    public static void update(OptionSetting setting, Option value) {
        setting.setValue(value);
    }

    public static void loadFromFile() {
        try {
            File file = new File(SETTINGS_FILE_LOCATION);
            if (!file.exists()) file.createNewFile();

            BufferedReader reader = new BufferedReader(new FileReader(file.getPath()));
            ArrayList<String> lines = new ArrayList<>();
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                lines.add(line);
            }
            reader.close();

            for (String line : lines) {
                String[] tokens = line.split(":");
                if (tokens.length != 2) continue;
                String name = tokens[0], value = tokens[1];

                FloatSetting.setIfPresent(name, value);
                KeySetting.setIfPresent(name, value);
                ToggleSetting.setIfPresent(name, value);
                OptionSetting.setIfPresent(name, value);

            }
        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Failed to load core.settings from file, proceeding with default core.settings set.");
        }
    }

    public static void writeToFile() {
        try {
            File file = new File(SETTINGS_FILE_LOCATION);
            if (!file.exists()) file.createNewFile();

            BufferedWriter writer = new BufferedWriter(new FileWriter(file.getPath()));

            for (FloatSetting setting : FloatSetting.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));
            for (KeySetting setting : KeySetting.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));
            for (ToggleSetting setting : ToggleSetting.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));
            for (OptionSetting setting : OptionSetting.values()) writer.write("%s:%s%n".formatted(setting.name(), setting.value()));

            writer.close();

        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Failed to save Settings to File");
        }
    }

    private static final String SETTINGS_FILE_LOCATION = "assets/textData/Settings";
}
