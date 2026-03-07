package core.settings;

public interface ToggleSetting extends KeySetting {

    default boolean setIfPresent(String name, String value) {
        if (!name().equalsIgnoreCase(name)) return false;

        String[] values = value.split("_");
        setValue(Boolean.parseBoolean(values[0]));
        setKeybind(Integer.parseInt(values[1]));

        return true;
    }

    default String toSaveValue() {
        return "%s_%s".formatted(String.valueOf(value()), String.valueOf(keybind()));
    }

    void setValue(boolean value);

    boolean value();

    boolean defaultValue();
}
