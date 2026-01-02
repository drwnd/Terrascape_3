package core.settings;

public enum ToggleSetting {
    SCROLL_HOTBAR(true),
    RAW_MOUSE_INPUT(true),
    USE_SHADOW_MAPPING(false),
    USE_AMBIENT_OCCLUSION(true),
    SHOW_BREAK_PARTICLES(true),
    SHOW_CUBE_PLACE_PARTICLES(true),
    SHOW_STRUCTURE_PLACE_PARTICLES(true),
    SHOW_SPLASH_PARTICLES(true),
    X_RAY(false),
    V_SYNC(true),
    FAKE_COORDINATES(true),
    RENDER_OCCLUDERS(false),
    RENDER_OCCLUDEES(false),
    RENDER_OCCLUDER_DEPTH_MAP(false),
    USE_OCCLUSION_CULLING(true);

    public static void setIfPresent(String name, String value) {
        try {
            valueOf(name).value = Boolean.parseBoolean(value);
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
