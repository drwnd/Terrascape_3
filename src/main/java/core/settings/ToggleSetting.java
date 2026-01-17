package core.settings;

import static org.lwjgl.glfw.GLFW.*;

public enum ToggleSetting implements KeyBound {
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
    RENDER_OCCLUDERS(false),
    RENDER_OCCLUDEES(false),
    RENDER_OCCLUDER_DEPTH_MAP(false),
    RENDER_SHADOW_MAP(false),
    RENDER_SHADOW_COLORS(false),
    CHUNKS_CAST_SHADOWS(true),
    PARTICLES_CAST_SHADOWS(true),
    GLASS_CASTS_SHADOWS(true),
    USE_OCCLUSION_CULLING(true),
    NO_CLIP(false, GLFW_KEY_P),
    CULLING_COMPUTATION(true, GLFW_KEY_L),
    DEBUG_MENU(false, GLFW_KEY_F3);

    public static void setIfPresent(String name, String value) {
        try {
            String[] values = value.split("_");
            valueOf(name).value = Boolean.parseBoolean(values[0]);
            valueOf(name).keybind = Integer.parseInt(values[1]);
        } catch (IllegalArgumentException | IndexOutOfBoundsException ignore) {

        }
    }

    ToggleSetting(boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = GLFW_KEY_UNKNOWN;
        this.keybind = defaultKeybind;
    }

    ToggleSetting(boolean defaultValue, int defaultKeybind) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = defaultKeybind;
        this.keybind = defaultKeybind;
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

    @Override
    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    @Override
    public int keybind() {
        return keybind;
    }

    @Override
    public int defaultKeybind() {
        return defaultKeybind;
    }

    private final boolean defaultValue;
    private boolean value;

    private final int defaultKeybind;
    private int keybind;
}
