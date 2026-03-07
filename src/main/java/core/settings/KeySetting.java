package core.settings;

public interface KeySetting extends Setting {

    default boolean setIfPresent(String name, String value) {
        if (!name().equalsIgnoreCase(name)) return false;
        setKeybind(Integer.parseInt(value));
        return true;
    }

    default String toSaveValue() {
        return String.valueOf(keybind());
    }

    int keybind();

    int defaultKeybind();

    void setKeybind(int keybind);
}
