package game.player.inventory;

import core.language.Language;
import core.renderables.OptionToggle;
import core.renderables.Renderable;
import core.renderables.TextElement;
import core.renderables.Toggle;
import core.rendering_api.Input;
import core.rendering_api.Window;

import game.player.interaction.Placeable;
import game.server.Game;
import game.server.material.Materials;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public final class CustomShapeTab extends Renderable implements InventoryTab {

    public CustomShapeTab(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);
    }

    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (CubeDisplay display : cubeDisplays)
            if (display.display().containsPixelCoordinate(pixelCoordinate)) {
                // TODO
            }
        return null;
    }

    @Override
    public void handleScroll(Vector2i pixelCoordinate, double yScroll) {
        if (shapePreview != null && shapePreview.containsPixelCoordinate(pixelCoordinate)) {
            shapePreview.changeZoom(yScroll > 0 ? 1.05F : 1 / 1.05F);
            return;
        }

        InventoryInput input = Game.getPlayer().getInventory().getInput();
        float newScroll = Math.max((float) (input.materialScroll - yScroll * 0.05), 0.0F);
        moveMaterialButtons(newScroll - input.materialScroll);
        input.materialScroll = newScroll;
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);
        if (!refreshShapePreview) return;

        refreshShapePreview = false;
        Vector3f rotation = shapePreview != null ? shapePreview.getRotation() : null;
        float zoom = shapePreview != null ? shapePreview.getZoom() : 1.0F;
        removeRenderable(shapePreview).delete();

        Vector2f sizeToParent = new Vector2f(0.325F, 0.325F * Window.getAspectRatio() * getAspectRatio());
        Vector2f offsetToParent = new Vector2f(0.0F, 0.0F);
//        selectedDisplay.getPlaceable().updateBitMap();
//        Structure structure = selectedDisplay.getPlaceable().getStructure();
//
//        shapePreview = new StructureDisplay(sizeToParent, offsetToParent, structure);
//        shapePreview.setRotation(rotation);
//        shapePreview.changeZoom(zoom);
//        shapePreview.setDoAutoFocusScaling(false);
//        shapePreview.setScaleWithGuiSize(false);
//
//        addRenderable(shapePreview);
    }

    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        if (!Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON)) lastCursorPos.set(pixelCoordinate);
        itemNameDisplay.setVisible(false);
        for (Renderable renderable : getChildren()) renderable.setFocused(renderable.containsPixelCoordinate(pixelCoordinate));
        for (CubeDisplay display : cubeDisplays) {
            StructureDisplay structureDisplay = display.display();
            if (!structureDisplay.containsPixelCoordinate(pixelCoordinate)) continue;

            Vector2f size = Window.toPixelSize(getSize(), scalesWithGuiSize());
            Vector2f position = Window.toPixelCoordinate(getPosition(), scalesWithGuiSize());
            itemNameDisplay.setText(Language.getTranslation(Materials.getTranslatable(display.material())));
            itemNameDisplay.setOffsetToParent(
                    (pixelCoordinate.x - position.x) / size.x - itemNameDisplay.getLength(),
                    (pixelCoordinate.y - position.y) / size.y);
            itemNameDisplay.setVisible(true);
            break;
        }
    }

    @Override
    public void dragOver(Vector2i pixelCoordinate) {
        super.dragOver(pixelCoordinate);
        if (shapePreview == null || pixelCoordinate.x > Window.getWidth() * (shapePreview.getPosition().x + shapePreview.getSize().x)) return;

        shapePreview.rotate(new Vector2i(pixelCoordinate).sub(lastCursorPos));
        lastCursorPos.set(pixelCoordinate);
    }


    void addContents(ArrayList<CubeDisplay> cubeDisplays, OptionToggle placeModeToggle, Toggle offsetToggle) {
        for (CubeDisplay display : cubeDisplays) {
            this.cubeDisplays.add(display);
            addRenderable(display.display());
        }
        addRenderable(placeModeToggle);
        addRenderable(offsetToggle);
    }

    private void moveMaterialButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (CubeDisplay button : cubeDisplays) button.display().move(offset);
    }

    private final TextElement itemNameDisplay = new TextElement(new Vector2f());
    private final Vector2i lastCursorPos = new Vector2i();

    private StructureDisplay shapePreview;
    private final ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>();
    private boolean refreshShapePreview = true;
}
