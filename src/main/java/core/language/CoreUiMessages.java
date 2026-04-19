package core.language;

public enum CoreUiMessages implements Translatable {

    SETTINGS,
    BACK,
    APPLY_SETTINGS,
    RESET_ALL_SETTINGS,
    RESET_SETTING,
    LANGUAGE,
    FONT,
    GUI_SIZE,
    TEXT_SIZE,
    MASTER_AUDIO,
    UI_AUDIO,
    RIM_THICKNESS,
    RESIZE_WINDOW,
    RAW_MOUSE_INPUT,
    TEXTURE_PACK,
    KEYBIND;

    @Override
    public String translationFileName() {
        return "coreUIMessages";
    }
}
