package core.settings;

import core.settings.optionSettings.Option;

public interface OptionSetting extends KeySetting {

    default boolean setIfPresent(String name, String value) {
        if (!name().equalsIgnoreCase(name)) return false;

        String[] values = value.split("#");

        Option savedValue = defaultValue().value(values[0]);
        if (savedValue == null) return false;
        setValue(savedValue);

        setKeybind(Integer.parseInt(values[1]));
        return true;
    }

    default String toSaveValue() {
        return "%s#%d".formatted(String.valueOf(value()), keybind());
    }

    void setValue(Option value);

    Option value();

    Option defaultValue();
}
