package game.player.inventory;

import core.language.Language;
import core.renderables.*;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.utils.FileManager;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.placeable_shapes.CustomShape;
import game.server.Game;
import game.server.generation.Structure;
import game.server.material.Materials;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class CustomShapeTab extends Renderable implements InventoryTab {

    public CustomShapeTab(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        UiButton loadButton = new UiButton(new Vector2f(0.125F, 0.1F), new Vector2f(0.025F, 0.775F), getLoadButtonClickable());
        loadButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessages.LOAD_SHADER_CODE, Color.WHITE));
        addRenderable(loadButton);

        int index = 0;
        for (UiButton settingElement : shape.getSettingButtons()) {
            if (settingElement instanceof CallbackSlider<?> slider) slider.setSlidingCallback(() -> refreshShapePreview = true);
            if (settingElement instanceof UiButton settingButton) {
                Clickable clickable = settingButton.getClickable();
                settingButton.setAction((Vector2i pixelCoordinate, int button, int action) -> {
                    clickable.clickOn(pixelCoordinate, button, action);
                    if (action == GLFW_PRESS) refreshShapePreview = true;
                });
            }
            settingElement.setSizeToParent(0.3F, 0.075F);
            settingElement.setOffsetToParent(0.35F, 1.0F - 0.05F * Window.getAspectRatio() - index++ * 0.08F);
            settingElement.setRimThicknessMultiplier(0.5F);
            addRenderable(settingElement);
        }
    }

    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (CubeDisplay display : cubeDisplays)
            if (display.display().containsPixelCoordinate(pixelCoordinate)) return shape.copyWithMaterial(display.material());
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

        Vector2f sizeToParent = new Vector2f(0.45F, 0.45F * Window.getAspectRatio() * getAspectRatio());
        Vector2f offsetToParent = new Vector2f(0.0F, 0.0F);
        Structure structure;
        try {
            structure = shape.updateBitMap().getStructure();
        } catch (Exception exception) {
            exception.printStackTrace();
            shape.setShaderCode("bool isInside(int x, int y, int z) {return true;}");
            return;
        }

        shapePreview = new StructureDisplay(sizeToParent, offsetToParent, structure);
        shapePreview.setRotation(rotation);
        shapePreview.changeZoom(zoom);
        shapePreview.setDoAutoFocusScaling(false);
        shapePreview.setScaleWithGuiSize(false);

        addRenderable(shapePreview);
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

    private Clickable getLoadButtonClickable() {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;
            if (Window.isMaximized()) Window.toggleFullScreen();

            JFileChooser fileChooser = new JFileChooser();
            int option = fileChooser.showOpenDialog(null);
            if (option != JFileChooser.APPROVE_OPTION) return;

            File file = fileChooser.getSelectedFile();
            String shaderCode = FileManager.loadFileContents(file.getPath());
            shape.setShaderCode(shaderCode);
            refreshShapePreview = true;
        };
    }

    private final TextElement itemNameDisplay = new TextElement(new Vector2f());
    private final Vector2i lastCursorPos = new Vector2i();

    private final CustomShape shape = new CustomShape(STONE, "bool isInside(int x, int y, int z) {return true;}");
    private StructureDisplay shapePreview;
    private final ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>();
    private boolean refreshShapePreview = true;
}
