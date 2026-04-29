package game.settings;

import core.settings.ToggleSetting;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F3;

public enum ToggleSettings implements ToggleSetting {
    SCROLL_HOTBAR(true),
    USE_SHADOW_MAPPING(false),
    USE_AMBIENT_OCCLUSION(true),
    SHOW_BREAK_PARTICLES(true),
    SHOW_CUBE_PLACE_PARTICLES(true),
    SHOW_STRUCTURE_PLACE_PARTICLES(true),
    SHOW_SPLASH_PARTICLES(true),
    X_RAY(false),
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
    DEBUG_MENU(false, GLFW_KEY_F3),
    OFFSET_FROM_GROUND(true),
    USE_RUST_FUNCTIONS(false);

    ToggleSettings(boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = GLFW_KEY_UNKNOWN;
        this.keybind = defaultKeybind;
    }

    ToggleSettings(boolean defaultValue, int defaultKeybind) {
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.defaultKeybind = defaultKeybind;
        this.keybind = defaultKeybind;
    }

    @Override
    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean value() {
        return value;
    }

    @Override
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
