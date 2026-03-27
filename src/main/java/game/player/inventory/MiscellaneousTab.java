package game.player.inventory;

import core.renderables.Renderable;

import game.player.interaction.Placeable;

import org.joml.Vector2f;
import org.joml.Vector2i;

public final class MiscellaneousTab extends Renderable implements InventoryTab {

    public MiscellaneousTab(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);
    }

    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        return null;
    }

    @Override
    public void handleScroll(Vector2i pixelCoordinate, double yScroll) {

    }
}
