package game.player;

import core.assets.AssetManager;
import core.assets.CoreTextures;
import core.languages.Language;
import core.renderables.TextElement;
import core.settings.FloatSetting;
import core.settings.KeySetting;
import core.renderables.UiElement;
import core.rendering_api.Window;

import core.utils.FileManager;
import game.player.interaction.CubePlaceable;
import game.player.interaction.Placeable;
import game.player.interaction.StructurePlaceable;
import game.player.rendering.StructureDisplay;
import game.player.rendering.StructureSelectionButton;
import game.server.Game;
import game.server.generation.Structure;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;

import static game.utils.Constants.*;

public final class Inventory extends UiElement {
    public Inventory() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f), CoreTextures.OVERLAY);
        setVisible(false);
        setAllowFocusScaling(false);
        setScaleWithGuiSize(false);
        itemNameDisplay.setAddTransparentBackground(true);

        for (int index = 0; index < AMOUNT_OF_MATERIALS; index++) {
            Vector2f sizeToParent = new Vector2f();
            Vector2f offsetToParent = new Vector2f();
            Structure structure = new Structure((byte) index);

            StructureDisplay display = new StructureDisplay(sizeToParent, offsetToParent, structure);
            display.setScalingFactor(FloatSetting.INVENTORY_ITEM_SCALING.value());

            cubeDisplays.add(new CubeDisplay(display, index));
            addRenderable(display);
        }
        updateDisplayPositions();
        addRenderable(itemNameDisplay);
    }

    public void handleInput(int button, int action, Vector2i pixelCoordinate) {
        if (action != GLFW.GLFW_PRESS || !isVisible()) return;
        Hotbar hotbar = Game.getPlayer().getHotbar();

        if (button == KeySetting.HOTBAR_SLOT_1.value()) hotbar.setContent(0, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_2.value()) hotbar.setContent(1, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_3.value()) hotbar.setContent(2, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_4.value()) hotbar.setContent(3, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_5.value()) hotbar.setContent(4, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_6.value()) hotbar.setContent(5, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_7.value()) hotbar.setContent(6, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_8.value()) hotbar.setContent(7, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_9.value()) hotbar.setContent(8, getSelectedPlaceable(pixelCoordinate));
    }

    public void updateDisplayPositions() {
        float itemSize = FloatSetting.GUI_SIZE.value() * FloatSetting.INVENTORY_ITEM_SIZE.value();
        int itemsPerRow = (int) FloatSetting.INVENTORY_ITEMS_PER_ROW.value();

        Vector2f sizeToParent = new Vector2f(itemSize, itemSize * Window.getAspectRatio());

        for (CubeDisplay display : cubeDisplays) {
            int row = display.index / itemsPerRow, column = display.index % itemsPerRow;
            float x = 1.0f - itemSize * (column + 1);
            float y = 1.0f - itemSize * 2 * (row + 1);

            display.display.setOffsetToParent(x, y);
            display.display.setSizeToParent(sizeToParent.x, sizeToParent.y);
        }
    }

    public void moveStructureButtons(float movement) {
        Vector2f offset = new Vector2f(0.0f, movement);
        for (StructureSelectionButton button : structureButtons) button.move(offset);
    }


    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        if (isFocused()) return;

        for (StructureSelectionButton button : structureButtons) {
            button.setFocused(button.containsPixelCoordinate(pixelCoordinate));
        }

        itemNameDisplay.setVisible(false);
        CubeDisplay selectedDisplay = null;

        for (CubeDisplay display : cubeDisplays) {
            StructureDisplay structureDisplay = display.display;
            structureDisplay.setFocused(false);
            if (!structureDisplay.isVisible() || !structureDisplay.containsPixelCoordinate(pixelCoordinate)) continue;

            structureDisplay.setFocused(true);
            selectedDisplay = display;
        }

        if (selectedDisplay != null) {
            itemNameDisplay.setText(Language.getMaterialName((byte) selectedDisplay.index));
            itemNameDisplay.setOffsetToParent(
                    (float) pixelCoordinate.x / Window.getWidth() - itemNameDisplay.getLength(),
                    (float) pixelCoordinate.y / Window.getHeight());
            itemNameDisplay.setVisible(true);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        for (StructureSelectionButton button : structureButtons) AssetManager.reload(button);
        getChildren().removeAll(structureButtons);
        structureButtons.clear();

        int structureCount = 0;
        Vector2f sizeToParent = new Vector2f(0.25f, 0.05f);
        File[] structureFiles = FileManager.getChildren(new File("assets/structures"));

        for (File structureFile : structureFiles) {
            if (structureFile == null) continue;
            String structureName = structureFile.getName();
            Vector2f offsetToParent = new Vector2f(0.05f, 1.0f - ++structureCount * 0.065f);

            StructureSelectionButton button = new StructureSelectionButton(sizeToParent, offsetToParent, structureName);
            structureButtons.add(button);
            addRenderable(button);
        }
    }


    private Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (CubeDisplay display : cubeDisplays)
            if (display.display.containsPixelCoordinate(pixelCoordinate)) return new CubePlaceable((byte) display.index);
        for (StructureSelectionButton button : structureButtons)
            if (button.containsPixelCoordinate(pixelCoordinate)) return new StructurePlaceable(button.getStructure());
        return null;
    }

    private final ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>();
    private final ArrayList<StructureSelectionButton> structureButtons = new ArrayList<>();
    private final TextElement itemNameDisplay = new TextElement(new Vector2f());

    private record CubeDisplay(StructureDisplay display, int index) {

    }
}
