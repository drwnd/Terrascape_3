package core.settings;

import core.settings.optionSettings.Option;

public interface OptionSetting extends Setting {

    default boolean setIfPresent(String name, String value) {
        if (!name().equalsIgnoreCase(name)) return false;
        Option savedValue = defaultValue().value(value);
        if (savedValue == null) return false;
        setValue(savedValue);
        return true;
    }

    default String toSaveValue() {
        return String.valueOf(value());
    }

    void setValue(Option value);

    Option value();

    Option defaultValue();
}
