package game.player;

import core.assets.CoreTextures;
import core.languages.Language;
import core.languages.UiMessage;
import core.renderables.TextElement;
import core.renderables.TextField;
import core.rendering_api.Input;
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
        input = new InventoryInput(this);

        setVisible(false);
        setAllowFocusScaling(false);
        setScaleWithGuiSize(false);
        itemNameDisplay.setAddTransparentBackground(true);
        itemNameDisplay.setAllowFocusScaling(false);
        itemNameDisplay.setScaleWithGuiSize(false);
        filterTextField = new TextField(new Vector2f(0.25f, 0.1f), new Vector2f(0.375f, 0.9f), UiMessage.STRUCTURE_NAME, this::reloadStructureButtons);

        long start = System.nanoTime();
        for (int index = 0; index < AMOUNT_OF_MATERIALS; index++) {
            Vector2f sizeToParent = new Vector2f();
            Vector2f offsetToParent = new Vector2f();
            Structure structure = new Structure((byte) index);

            StructureDisplay display = new StructureDisplay(sizeToParent, offsetToParent, structure);
            display.setScalingFactor(FloatSetting.INVENTORY_ITEM_SCALING.value());
            display.setScaleWithGuiSize(false);

            cubeDisplays.add(new CubeDisplay(display, (byte) index));
            addRenderable(display);
        }
        System.out.printf("Build cube displays. Took %sms%n", (System.nanoTime() - start) / 1_000_000);
        addRenderable(itemNameDisplay);
        addRenderable(filterTextField);
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

    public void moveStructureButtons(float movement) {
        Vector2f offset = new Vector2f(0.0f, movement);
        for (StructureSelectionButton button : structureButtons) button.move(offset);
    }

    public Input getInput() {
        return input;
    }


    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        super.hoverOver(pixelCoordinate);
        itemNameDisplay.setVisible(false);

        for (CubeDisplay display : cubeDisplays) {
            StructureDisplay structureDisplay = display.display;
            if (!structureDisplay.containsPixelCoordinate(pixelCoordinate)) continue;

            itemNameDisplay.setText(Language.getMaterialName(display.material));
            itemNameDisplay.setOffsetToParent((float) pixelCoordinate.x / Window.getWidth() - itemNameDisplay.getLength(), (float) pixelCoordinate.y / Window.getHeight());
            itemNameDisplay.setVisible(true);
            break;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!isVisible()) return;

        reloadStructureButtons();
        updateDisplayPositions();
    }


    private void updateDisplayPositions() {
        float itemSize = FloatSetting.INVENTORY_ITEM_SIZE.value();
        int itemsPerRow = (int) FloatSetting.INVENTORY_ITEMS_PER_ROW.value();

        Vector2f sizeToParent = new Vector2f(itemSize, itemSize * Window.getAspectRatio());

        for (CubeDisplay display : cubeDisplays) {
            int index = display.material & 0xFF;
            int row = index / itemsPerRow, column = index % itemsPerRow;
            float x = 1.0f - itemSize * (column + 1);
            float y = 1.0f - itemSize * 2 * (row + 1);

            display.display.setOffsetToParent(x, y);
            display.display.setSizeToParent(sizeToParent.x, sizeToParent.y);
        }
    }

    private void reloadStructureButtons() {
        getChildren().removeAll(structureButtons);
        structureButtons.clear();

        int structureCount = 0;
        Vector2f sizeToParent = new Vector2f(0.25f, 0.05f);
        File[] structureFiles = FileManager.getChildren(new File("assets/structures"));
        String filterText = filterTextField.getText().toLowerCase();

        for (File structureFile : structureFiles) {
            if (structureFile == null || !structureFile.getName().toLowerCase().contains(filterText)) continue;
            String structureName = structureFile.getName();
            Vector2f offsetToParent = new Vector2f(0.05f, 1.0f - ++structureCount * 0.065f + input.getScroll());

            StructureSelectionButton button = new StructureSelectionButton(sizeToParent, offsetToParent, structureName);
            button.setRimThicknessMultiplier(0.5f);
            structureButtons.add(button);
            addRenderable(button);
        }
    }

    private Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (CubeDisplay display : cubeDisplays)
            if (display.display.containsPixelCoordinate(pixelCoordinate)) return new CubePlaceable(display.material);
        for (StructureSelectionButton button : structureButtons)
            if (button.containsPixelCoordinate(pixelCoordinate)) return new StructurePlaceable(button.getStructure());
        return null;
    }

    private final ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>();
    private final ArrayList<StructureSelectionButton> structureButtons = new ArrayList<>();
    private final TextElement itemNameDisplay = new TextElement(new Vector2f());
    private final TextField filterTextField;
    private final InventoryInput input;

    private record CubeDisplay(StructureDisplay display, byte material) {

    }
}
