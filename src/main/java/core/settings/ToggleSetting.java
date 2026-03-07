package core.settings;

public interface ToggleSetting extends KeySetting {

    void setValue(boolean value);

    boolean value();

    boolean defaultValue();
}
