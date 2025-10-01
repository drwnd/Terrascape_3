package core.settings;

public enum ToggleSetting {
    SCROLL_HOTBAR(true),
    RAW_MOUSE_INPUT(true),
    DO_SHADOW_MAPPING(false),
    X_RAY(false),
    V_SYNC(true);

    public static void setIfPresent(String name, String value) {
        try {
            valueOf(name).setValue(Boolean.parseBoolean(value));
        } catch (IllegalArgumentException ignore) {

        }
    }

    ToggleSetting(boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    void setValue(boolean value) {
        this.value = value;
    }

    public boolean value() {
        return value;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    private final boolean defaultValue;
    private boolean value;
}
