package core.language;

public enum CoreUiMessages implements Translatable {

    SETTINGS,
    BACK,
    APPLY_SETTINGS,
    RESET_ALL_SETTINGS,
    RESET_SETTING,
    KEYBIND;

    @Override
    public String translationFileName() {
        return "coreUIMessages";
    }
}
