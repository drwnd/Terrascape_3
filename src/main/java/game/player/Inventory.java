package game.player;

import core.assets.CoreTextures;
import core.language.Language;
import core.renderables.*;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.utils.FileManager;

import game.language.UiMessages;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.placeable_shapes.*;
import game.player.interaction.Placeable;
import game.player.interaction.StructurePlaceable;
import game.player.rendering.StructureDisplay;
import game.player.rendering.StructureSelectionButton;
import game.server.Game;
import game.server.generation.Structure;
import game.server.material.Materials;
import game.settings.FloatSettings;
import game.settings.KeySettings;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.io.File;
import java.util.ArrayList;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class Inventory extends UiElement {

    public Inventory() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F), CoreTextures.OVERLAY);
        input = new InventoryInput(this);

        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);
        itemNameDisplay.setAddTransparentBackground(true);
        itemNameDisplay.setDoAutoFocusScaling(false);
        itemNameDisplay.setScaleWithGuiSize(false);
        filterTextField = new TextField(new Vector2f(0.25F, 0.1F), new Vector2f(0.375F, 0.9F), UiMessages.STRUCTURE_NAME, this::reloadStructureButtons);

        long start = System.nanoTime();
        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            Vector2f sizeToParent = new Vector2f();
            Vector2f offsetToParent = new Vector2f();
            Structure structure = new Structure((byte) material);

            StructureDisplay display = new StructureDisplay(sizeToParent, offsetToParent, structure);
            display.setScalingFactor(FloatSettings.INVENTORY_ITEM_SCALING.value());
            display.setScaleWithGuiSize(false);

            cubeDisplays.add(new CubeDisplay(display, (byte) material));
            addRenderable(display);
        }
        System.out.printf("Build cube displays. Took %sms%n", (System.nanoTime() - start) / 1_000_000);
        loadShapeDisplays();
        addRenderable(itemNameDisplay);
        addRenderable(filterTextField);
    }

    public void handleInput(int button, int action, Vector2i pixelCoordinate) {
        if (action != GLFW_PRESS || !isVisible()) return;
        Hotbar hotbar = Game.getPlayer().getHotbar();

        if (button == KeySettings.HOTBAR_SLOT_1.keybind()) hotbar.setContent(0, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_2.keybind()) hotbar.setContent(1, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_3.keybind()) hotbar.setContent(2, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_4.keybind()) hotbar.setContent(3, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_5.keybind()) hotbar.setContent(4, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_6.keybind()) hotbar.setContent(5, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_7.keybind()) hotbar.setContent(6, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_8.keybind()) hotbar.setContent(7, getSelectedPlaceable(pixelCoordinate));
        if (button == KeySettings.HOTBAR_SLOT_9.keybind()) hotbar.setContent(8, getSelectedPlaceable(pixelCoordinate));
    }

    public void moveStructureButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (StructureSelectionButton button : structureButtons) button.move(offset);
    }

    public void moveMaterialButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (CubeDisplay button : cubeDisplays) button.display.move(offset);
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

            itemNameDisplay.setText(Language.getTranslation(Materials.getTranslatable(display.material)));
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

    private void loadShapeDisplays() {
        getChildren().removeAll(shapeDisplays);
        getChildren().removeAll(shapePlaceableSettingSliders);
        shapeDisplays.clear();
        shapePlaceableSettingSliders.clear();
        Vector2f sizeToParent = new Vector2f(0.0475F, 0.0475F * Window.getAspectRatio());

        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.35F, 0.8F), new CubePlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.4F, 0.8F), new SpherePlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.45F, 0.8F), new CylinderPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.5F, 0.8F), new StairPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.55F, 0.8F), new SlopedStairPlaceable(STONE), this));

        for (Renderable renderable : shapeDisplays) addRenderable(renderable);
        selectedDisplay = shapeDisplays.getFirst();
        for (Renderable renderable : selectedDisplay.settingElements) renderable.setVisible(true);
    }

    private void updateDisplayPositions() {
        float itemSize = FloatSettings.INVENTORY_ITEM_SIZE.value();
        int itemsPerRow = Math.max(1, (int) Math.floor(1.0 / (3 * itemSize)));

        Vector2f sizeToParent = new Vector2f(itemSize, itemSize * Window.getAspectRatio());

        for (CubeDisplay display : cubeDisplays) {
            int index = display.material & 0xFF;
            int row = index / itemsPerRow, column = index % itemsPerRow;
            float x = 1.0F - itemSize * (column + 1);
            float y = 1.0F - itemSize * 2 * (row + 1);

            display.display.setOffsetToParent(x, y + input.getMaterialScroll());
            display.display.setSizeToParent(sizeToParent.x, sizeToParent.y);
        }
    }

    private void reloadStructureButtons() {
        getChildren().removeAll(structureButtons);
        structureButtons.clear();

        int structureCount = 0;
        Vector2f sizeToParent = new Vector2f(0.25F, 0.05F);
        File[] structureFiles = FileManager.getChildren(new File("assets/structures"));
        String filterText = filterTextField.getText().toLowerCase();

        for (File structureFile : structureFiles) {
            if (structureFile == null || !structureFile.getName().toLowerCase().contains(filterText)) continue;
            String structureName = structureFile.getName();
            Vector2f offsetToParent = new Vector2f(0.05F, 1.0F - ++structureCount * 0.065F + input.getStructureScroll());

            StructureSelectionButton button = new StructureSelectionButton(sizeToParent, offsetToParent, structureName);
            button.setRimThicknessMultiplier(0.5F);
            structureButtons.add(button);
            addRenderable(button);
        }
    }

    private Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (CubeDisplay display : cubeDisplays)
            if (display.display.containsPixelCoordinate(pixelCoordinate)) {
                if (selectedDisplay == null) return new CubePlaceable(display.material);
                return selectedDisplay.placeable.copyWithMaterial(display.material);
            }
        for (StructureSelectionButton button : structureButtons)
            if (button.containsPixelCoordinate(pixelCoordinate)) return new StructurePlaceable(button.getStructure());
        return null;
    }

    private final ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>();
    private final ArrayList<StructureSelectionButton> structureButtons = new ArrayList<>();
    private final ArrayList<ShapeDisplay> shapeDisplays = new ArrayList<>();
    private final ArrayList<UiBackgroundElement> shapePlaceableSettingSliders = new ArrayList<>();
    private final TextElement itemNameDisplay = new TextElement(new Vector2f());
    private final TextField filterTextField;
    private final InventoryInput input;

    private ShapeDisplay selectedDisplay;

    private record CubeDisplay(StructureDisplay display, byte material) {

    }

    private static class ShapeDisplay extends UiButton {

        private ShapeDisplay(Vector2f sizeToParent, Vector2f offsetToParent, ShapePlaceable placeable, Inventory inventory) {
            super(sizeToParent, offsetToParent);
            setAction(getAction());
            setRimThicknessMultiplier(0.5F);
            setDoAutoFocusScaling(false);
            setScalingFactor(1.2F);
            this.placeable = placeable;

            int index = 0;
            for (UiBackgroundElement settingElement : placeable.settings()) {
                settingElement.setSizeToParent(0.3F, 0.075F);
                settingElement.setOffsetToParent(0.35F, 0.7F - index++ * 0.08F);
                settingElement.setVisible(false);
                settingElement.setRimThicknessMultiplier(0.5F);
                settingElements.add(settingElement);
            }

            inventory.shapePlaceableSettingSliders.addAll(settingElements);
            for (UiBackgroundElement settingElement : settingElements) inventory.addRenderable(settingElement);

            addRenderable(new StructureDisplay(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F), placeable.getStructure()));
        }

        @Override
        public void renderSelf(Vector2f position, Vector2f size) {
            if (Game.getPlayer().getInventory().selectedDisplay == this) scaleForFocused(position, size);
            super.renderSelf(position, size);
        }

        private Clickable getAction() {
            return (Vector2i _, int _, int action) -> {
                if (action != GLFW_PRESS) return;
                Inventory inventory = Game.getPlayer().getInventory();
                inventory.selectedDisplay = this;
                for (UiBackgroundElement settingElement : inventory.shapePlaceableSettingSliders) settingElement.setVisible(false);
                for (UiBackgroundElement settingElement : settingElements) settingElement.setVisible(true);
            };
        }

        private final ArrayList<UiBackgroundElement> settingElements = new ArrayList<>();
        private final ShapePlaceable placeable;
    }
}
