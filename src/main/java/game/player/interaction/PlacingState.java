package game.player.interaction;

import core.rendering_api.Input;
import game.server.Game;
import game.settings.KeySettings;

public enum PlacingState {

    NONE,
    REPEAT, REPEAT_LOCKED,
    SHAPE, SHAPE_LOCKED,

    STRUCTURE_PLACE,
    STRUCTURE_PLACE_LOCKED,

    STRUCTURE_SELECT,
    STRUCTURE_SELECT_LOCKED;

    public boolean isLocked() {
        return switch (this) {
            case REPEAT_LOCKED, SHAPE_LOCKED, STRUCTURE_PLACE_LOCKED, STRUCTURE_SELECT_LOCKED -> true;
            default -> false;
        };
    }

    public boolean shouldRender() {
        return switch (this) {
            case NONE, SHAPE_LOCKED -> false;
            case REPEAT, REPEAT_LOCKED, STRUCTURE_PLACE_LOCKED -> true;
            case SHAPE, STRUCTURE_PLACE -> Input.isKeyPressed(KeySettings.SHOW_PLACEABLE_PREVIEW);
            case STRUCTURE_SELECT, STRUCTURE_SELECT_LOCKED -> Game.getPlayer().getInteractionHandler().getStartTarget() != null;
        };
    }
}
