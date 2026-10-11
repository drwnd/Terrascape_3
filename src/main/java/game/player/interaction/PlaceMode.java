package game.player.interaction;

import core.language.Translatable;
import core.settings.optionSettings.Option;
import game.server.Game;
import game.server.materials_data.MaterialsData;

import static game.utils.Constants.AIR;

public enum PlaceMode implements Option, Translatable {

    PAINT, REPLACE, REPLACE_AIR, BREAK_HELD_ONLY;

    @Override
    public String translationFileName() {
        return "PlaceModes";
    }

    public MaterialsData.OverrideFunction getOverrideFunction(boolean isBreak) {
        byte heldMaterial = ((ShapePlaceable) Game.getPlayer().getHeldPlaceable()).getMaterial();

        if (isBreak) return switch (this) {
            case PAINT -> ((previousMaterial, material) -> material != AIR && previousMaterial != AIR ? AIR : previousMaterial);
            case REPLACE -> ((previousMaterial, material) -> material != AIR ? AIR : previousMaterial);
            case REPLACE_AIR -> ((previousMaterial, material) -> material != AIR && previousMaterial == AIR ? AIR : previousMaterial);
            case BREAK_HELD_ONLY -> ((previousMaterial, material) -> material != AIR && previousMaterial == heldMaterial ? AIR : previousMaterial);
        };

        return switch (this) {
            case PAINT -> ((previousMaterial, material) -> previousMaterial != AIR && material != AIR ? material : previousMaterial);
            case REPLACE -> ((previousMaterial, material) -> material != AIR ? material : previousMaterial);
            case REPLACE_AIR -> ((previousMaterial, material) -> previousMaterial == AIR ? material : previousMaterial);
            case BREAK_HELD_ONLY -> ((previousMaterial, material) -> previousMaterial == heldMaterial ? material : previousMaterial);
        };
    }
}
