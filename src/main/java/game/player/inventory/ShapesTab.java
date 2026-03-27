package game.player.inventory;

import core.language.Language;
import core.renderables.*;
import core.rendering_api.Window;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.placeable_shapes.*;
import game.server.Game;
import game.server.generation.Structure;
import game.server.material.Materials;
import game.settings.FloatSettings;
import game.settings.OptionSettings;
import game.settings.ToggleSettings;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static game.utils.Constants.AMOUNT_OF_MATERIALS;
import static game.utils.Constants.STONE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class ShapesTab extends Renderable implements InventoryTab {

    public ShapesTab(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        itemNameDisplay.setAddTransparentBackground(true);
        itemNameDisplay.setDoAutoFocusScaling(false);
        itemNameDisplay.setScaleWithGuiSize(false);

        OptionToggle placeModeToggle = new OptionToggle(new Vector2f(0.125F, 0.1F), new Vector2f(0.35F, 0.9F), OptionSettings.PLACE_MODE, null, true);
        Toggle offsetToggle = new Toggle(new Vector2f(0.125F, 0.1F), new Vector2f(0.525F, 0.9F), ToggleSettings.OFFSET_FROM_GROUND, UiMessages.OFFSET_FROM_GROUND, true);

        long start = System.nanoTime();
        for (int material = 0; material < AMOUNT_OF_MATERIALS; material++) {
            sizeToParent = new Vector2f();
            offsetToParent = new Vector2f();
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
        addRenderable(placeModeToggle);
        addRenderable(offsetToggle);
    }

    @Override
    public void resizeSelfTo(int width, int height) {
        for (ShapeDisplay shapeDisplay : shapeDisplays) shapeDisplay.setSizeToParent(0.0475F, 0.0475F * Window.getAspectRatio());
    }

    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (CubeDisplay display : cubeDisplays)
            if (display.display.containsPixelCoordinate(pixelCoordinate)) {
                if (selectedDisplay == null) return new CubePlaceable(display.material);
                return selectedDisplay.placeable.copyWithMaterial(display.material);
            }
        return null;
    }

    @Override
    public void handleScroll(Vector2i pixelCoordinate, double yScroll) {
        InventoryInput input = Game.getPlayer().getInventory().getInput();
        float newScroll = Math.max((float) (input.materialScroll - yScroll * 0.05), 0.0F);
        moveMaterialButtons(newScroll - input.materialScroll);
        input.materialScroll = newScroll;
    }

    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        itemNameDisplay.setVisible(false);
        for (Renderable renderable : getChildren()) renderable.setFocused(renderable.containsPixelCoordinate(pixelCoordinate));
        for (CubeDisplay display : cubeDisplays) {
            StructureDisplay structureDisplay = display.display;
            if (!structureDisplay.containsPixelCoordinate(pixelCoordinate)) continue;

            itemNameDisplay.setText(Language.getTranslation(Materials.getTranslatable(display.material)));
            itemNameDisplay.setOffsetToParent((float) pixelCoordinate.x / Window.getWidth() - itemNameDisplay.getLength(), (float) pixelCoordinate.y / Window.getHeight());
            itemNameDisplay.setVisible(true);
            break;
        }
    }

    void updateDisplayPositions() {
        InventoryInput input = Game.getPlayer().getInventory().getInput();
        float itemSize = FloatSettings.INVENTORY_ITEM_SIZE.value();
        int itemsPerRow = Math.max(1, (int) Math.floor(1.0 / (3 * itemSize)));

        Vector2f sizeToParent = new Vector2f(itemSize, itemSize * Window.getAspectRatio());

        for (CubeDisplay display : cubeDisplays) {
            int index = display.material & 0xFF;
            int row = index / itemsPerRow, column = index % itemsPerRow;
            float x = 1.0F - itemSize * (column + 1);
            float y = 1.0F - itemSize * 2 * (row + 1);

            display.display.setOffsetToParent(x, y + input.materialScroll);
            display.display.setSizeToParent(sizeToParent.x, sizeToParent.y);
        }
    }

    private void loadShapeDisplays() {
        for (Renderable shapeDisplay : shapeDisplays) removeRenderable(shapeDisplay).delete();
        for (Renderable slider : shapePlaceableSettingSliders) removeRenderable(slider).delete();
        shapeDisplays.clear();
        shapePlaceableSettingSliders.clear();
        Vector2f sizeToParent = new Vector2f(0.0475F, 0.0475F * Window.getAspectRatio());
        float firstRowY = 0.8F;
        float secondRowY = 0.8F - 0.05F * Window.getAspectRatio();

        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.35F, firstRowY), new CubePlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.4F, firstRowY), new SpherePlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.45F, firstRowY), new CylinderPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.5F, firstRowY), new ConePlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.55F, firstRowY), new SlabPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.6F, firstRowY), new EllipsoidPlaceable(STONE), this));

        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.35F, secondRowY), new StairPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.4F, secondRowY), new InsideStairPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.45F, secondRowY), new OutsideStairPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.5F, secondRowY), new ArcPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.55F, secondRowY), new InsideArcPlaceable(STONE), this));
        shapeDisplays.add(new ShapeDisplay(sizeToParent, new Vector2f(0.6F, secondRowY), new OutsideArcPlaceable(STONE), this));

        for (Renderable renderable : shapeDisplays) addRenderable(renderable);
        selectedDisplay = shapeDisplays.getFirst();
        for (Renderable renderable : selectedDisplay.settingElements) renderable.setVisible(true);
    }

    private void moveMaterialButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (CubeDisplay button : cubeDisplays) button.display.move(offset);
    }
    private final ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>();
    private final ArrayList<ShapeDisplay> shapeDisplays = new ArrayList<>();
    private final ArrayList<UiBackgroundElement> shapePlaceableSettingSliders = new ArrayList<>();

    private final TextElement itemNameDisplay = new TextElement(new Vector2f());

    private ShapeDisplay selectedDisplay;

    private record CubeDisplay(StructureDisplay display, byte material) {

    }

    private static class ShapeDisplay extends UiButton {

        private ShapeDisplay(Vector2f sizeToParent, Vector2f offsetToParent, ShapePlaceable placeable, ShapesTab placeablesTab) {
            super(sizeToParent, offsetToParent);
            setAction(getAction());
            setRimThicknessMultiplier(0.5F);
            setDoAutoFocusScaling(false);
            setScalingFactor(1.2F);
            this.placeable = placeable;

            int index = 0;
            for (UiBackgroundElement settingElement : placeable.settings()) {
                settingElement.setSizeToParent(0.3F, 0.075F);
                settingElement.setOffsetToParent(0.35F, 0.7F - 0.05F * Window.getAspectRatio() - index++ * 0.08F);
                settingElement.setVisible(false);
                settingElement.setRimThicknessMultiplier(0.5F);
                settingElements.add(settingElement);
            }

            placeablesTab.shapePlaceableSettingSliders.addAll(settingElements);
            for (UiBackgroundElement settingElement : settingElements) placeablesTab.addRenderable(settingElement);

            addRenderable(new StructureDisplay(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F), placeable.getSmallStructure()));
        }

        @Override
        public void renderSelf(Vector2f position, Vector2f size) {
            if (Game.getPlayer().getInventory().shapesTab.selectedDisplay == this) scaleForFocused(position, size);
            super.renderSelf(position, size);
        }

        private Clickable getAction() {
            return (Vector2i _, int _, int action) -> {
                if (action != GLFW_PRESS) return;
                Inventory inventory = Game.getPlayer().getInventory();
                inventory.shapesTab.selectedDisplay = this;
                for (UiBackgroundElement settingElement : inventory.shapesTab.shapePlaceableSettingSliders) settingElement.setVisible(false);
                for (UiBackgroundElement settingElement : settingElements) settingElement.setVisible(true);
            };
        }

        private final ArrayList<UiBackgroundElement> settingElements = new ArrayList<>();
        private final ShapePlaceable placeable;
    }
}
