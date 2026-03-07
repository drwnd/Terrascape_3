package core.settings;

import core.settings.optionSettings.Option;

public interface OptionSetting {

    void setValue(Option value);

    Option value();

    Option defaultValue();
}
