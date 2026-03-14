package core.settings;

import core.assets.AssetManager;
import core.assets.SettingsFile;
import core.assets.identifiers.AssetIdentifier;
import core.settings.optionSettings.Option;
import core.utils.FileManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class Settings {

    static {
        fileIdentifier = () -> Settings::loadSettingsFile;
        settings = new ArrayList<>();
        registerSettingsEnums(CoreFloatSettings.class, CoreKeySettings.class, CoreToggleSettings.class, CoreOptionSettings.class);
    }

    @SafeVarargs
    public static void registerSettingsEnums(Class<? extends Setting>... settings) {
        for (Class<? extends Setting> setting : settings) registerSettingsEnum(setting);
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
        AssetManager.reload(fileIdentifier);
        initSettings(settings);
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

    private static SettingsFile loadSettingsFile() {
        String[] settingsFileContents = FileManager.readAllLines(new File(SETTINGS_FILE_LOCATION));
        String[][] settings = new String[settingsFileContents.length][0];
        for (int index = 0; index < settingsFileContents.length; index++) settings[index] = settingsFileContents[index].split(":");
        return new SettingsFile(settings);
    }

    private static void registerSettingsEnum(Class<? extends Setting> settings) {
        if (!settings.isEnum()) throw new IllegalArgumentException("Argument must be an Enum");
        List<Setting> settingList = List.of(settings.getEnumConstants());
        Settings.settings.addAll(settingList);
        initSettings(settingList);
    }

    private static void initSettings(Iterable<Setting> settings) {
        for (String[] tokens : AssetManager.get(fileIdentifier).getTokens())
            for (Setting setting : settings) {
                if (tokens.length != 2) continue;
                if (setting.setIfPresent(tokens[0], tokens[1])) break;
            }
    }

    private static final ArrayList<Setting> settings;
    private static final String SETTINGS_FILE_LOCATION = "assets/textData/Settings";
    private static final AssetIdentifier<SettingsFile> fileIdentifier;
}
