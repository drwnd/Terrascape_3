package core.languages;

import core.utils.StringGetter;

public enum UiMessage implements StringGetter {
    QUIT_GAME,
    NEW_WORLD,
    SETTINGS,
    CONFIRM_DELETE_WORLD,
    KEEP_WORLD,
    PLAY_WORLD,
    DELETE_WORLD,
    QUIT_WORLD,
    CONTINUE_PLAYING,
    BACK,
    EVERYTHING_SECTION,
    CONTROLS_SECTION,
    RENDERING_SECTION,
    UI_CUSTOMIZATION_SECTION,
    SOUND_SECTION,
    DEBUG_SECTION,
    DEBUG_SCREEN_SECTION,
    WORLD_NAME,
    WORLD_SEED,
    CREATE_WORLD,
    APPLY_SETTINGS,
    RESET_ALL_SETTINGS,
    RESET_SETTING,
    LANGUAGE,
    FONT;

    @Override
    public String get() {
        return Language.getUiMessage(this);
    }
}
