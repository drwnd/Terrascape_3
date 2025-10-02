package game.player;

import game.assets.Textures;
import core.languages.Language;
import core.renderables.TextElement;
import core.settings.FloatSetting;
import core.settings.KeySetting;
import core.renderables.UiElement;
import core.rendering_api.Window;

import game.player.rendering.StructureDisplay;
import game.server.Game;
import game.server.generation.Structure;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static game.utils.Constants.AIR;
import static game.utils.Constants.AMOUNT_OF_MATERIALS;

public final class Inventory extends UiElement {
    public Inventory() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f), Textures.INVENTORY_OVERLAY);
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

            displays.add(new Display(display, index));
            addRenderable(display);
        }
        updateDisplayPositions();
        addRenderable(itemNameDisplay);
    }

    public void handleInput(int button, int action, Vector2i pixelCoordinate) {
        if (action != GLFW.GLFW_PRESS || !isVisible()) return;
        Hotbar hotbar = Game.getPlayer().getHotbar();

        if (button == KeySetting.HOTBAR_SLOT_1.value()) hotbar.setContent(0, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_2.value()) hotbar.setContent(1, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_3.value()) hotbar.setContent(2, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_4.value()) hotbar.setContent(3, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_5.value()) hotbar.setContent(4, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_6.value()) hotbar.setContent(5, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_7.value()) hotbar.setContent(6, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_8.value()) hotbar.setContent(7, getHoveredOverMaterial(pixelCoordinate));
        if (button == KeySetting.HOTBAR_SLOT_9.value()) hotbar.setContent(8, getHoveredOverMaterial(pixelCoordinate));
    }


    public void updateDisplayPositions() {
        float itemSize = FloatSetting.GUI_SIZE.value() * FloatSetting.INVENTORY_ITEM_SIZE.value();
        int itemsPerRow = (int) FloatSetting.INVENTORY_ITEMS_PER_ROW.value();

        Vector2f sizeToParent = new Vector2f(itemSize, itemSize * Window.getAspectRatio());

        for (Display display : displays) {
            int row = display.index / itemsPerRow, column = display.index % itemsPerRow;
            float x = 1.0f - itemSize * (column + 1);
            float y = 1.0f - itemSize * 2 * (row + 1);

            display.structureDisplay.setOffsetToParent(x, y);
            display.structureDisplay.setSizeToParent(sizeToParent.x, sizeToParent.y);
        }
    }


    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        if (isFocused()) return;

        itemNameDisplay.setVisible(false);
        Display selectedDisplay = null;

        for (Display display : displays) {
            StructureDisplay structureDisplay = display.structureDisplay;
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


    private byte getHoveredOverMaterial(Vector2i pixelCoordinate) {
        for (Display display : displays)
            if (display.structureDisplay.containsPixelCoordinate(pixelCoordinate)) return (byte) display.index;
        return AIR;
    }

    private final ArrayList<Display> displays = new ArrayList<>();
    private final TextElement itemNameDisplay = new TextElement(new Vector2f());

    private record Display(StructureDisplay structureDisplay, int index) {

    }
}
