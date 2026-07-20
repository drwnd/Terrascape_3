package game.player.interaction;

import core.language.Translatable;
import core.settings.optionSettings.Option;

public enum PlaceMode implements Option, Translatable {

    PAINT, REPLACE, REPLACE_AIR, BREAK_HELD_ONLY;

    @Override
    public String translationFileName() {
        return "PlaceModes";
    }
}
