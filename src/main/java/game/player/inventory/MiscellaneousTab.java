package game.player.inventory;

import core.renderables.Renderable;

import core.rendering_api.Window;
import game.player.interaction.ChunkRebuildPlaceable;
import game.player.interaction.Placeable;
import game.player.interaction.StructureSelector;

import org.joml.Vector2f;
import org.joml.Vector2i;

public final class MiscellaneousTab extends Renderable implements InventoryTab {

    /**
     * Initializes the Miscellaneous tab with displays for special placeables.
     * @param sizeToParent the size relative to the parent element
     * @param offsetToParent the offset relative to the parent element
     */
    public MiscellaneousTab(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        float width = 0.2F, height = width * getAspectRatio() * Window.getAspectRatio();
        Vector2f size = new Vector2f(width, height);

        chunkRebuildDisplay = new StructureDisplay(size, new Vector2f(0.05F, 0.95F - height), new ChunkRebuildPlaceable().getStructure());
        structureSelectDisplay = new StructureDisplay(size, new Vector2f(0.05F, 0.95F - height * 2), new StructureSelector().getStructure());

        addRenderable(chunkRebuildDisplay);
        addRenderable(structureSelectDisplay);
    }

    /**
     * Retrieves the placeable selected at the given pixel coordinate.
     * @param pixelCoordinate the current mouse position in screen pixels
     * @return a {@link Placeable} corresponding to the selection, or null
     */
    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        if (chunkRebuildDisplay.containsPixelCoordinate(pixelCoordinate)) return new ChunkRebuildPlaceable();
        if (structureSelectDisplay.containsPixelCoordinate(pixelCoordinate)) return new StructureSelector();
        return null;
    }

    @Override
    public void handleScroll(Vector2i pixelCoordinate, double yScroll) {

    }

    private final StructureDisplay chunkRebuildDisplay;
    private final StructureDisplay structureSelectDisplay;
}
