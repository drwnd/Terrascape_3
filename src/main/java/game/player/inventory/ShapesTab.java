package game.player.inventory;

import core.language.Language;
import core.renderables.*;
import core.rendering_api.Input;
import core.rendering_api.Window;

import game.language.UiMessages;
import game.player.interaction.Placeable;
import game.player.interaction.placeable_shapes.*;
import game.server.Game;
import game.server.generation.Structure;
import game.server.material.Materials;
import game.settings.FloatSettings;
import game.settings.OptionSettings;
import game.settings.ToggleSettings;

import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

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
        updateDisplayPositions();
        for (ShapeDisplay shapeDisplay : shapeDisplays) shapeDisplay.setSizeToParent(0.0475F, 0.0475F * Window.getAspectRatio() * getAspectRatio());

        if (shapePreview != null) {
            float sizeToParentY = 0.325F * Window.getAspectRatio() * getAspectRatio();
            shapePreview.setSizeToParent(0.325F, sizeToParentY);
            shapePreview.setOffsetToParent(0.0F, 0.5F - sizeToParentY * 0.5F);
        }
    }

    @Override
    public Placeable getSelectedPlaceable(Vector2i pixelCoordinate) {
        for (CubeDisplay display : cubeDisplays)
            if (display.display.containsPixelCoordinate(pixelCoordinate)) {
                if (selectedDisplay == null) return new CubePlaceable(display.material);
                return selectedDisplay.getPlaceable().copyWithMaterial(display.material);
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
    public void hoverOver(Vector2i pixelCoordinate) {
        if (!Input.isKeyPressed(GLFW_MOUSE_BUTTON_LEFT | Input.IS_MOUSE_BUTTON)) lastCursorPos.set(pixelCoordinate);
        itemNameDisplay.setVisible(false);
        for (Renderable renderable : getChildren()) renderable.setFocused(renderable.containsPixelCoordinate(pixelCoordinate));
        for (CubeDisplay display : cubeDisplays) {
            StructureDisplay structureDisplay = display.display;
            if (!structureDisplay.containsPixelCoordinate(pixelCoordinate)) continue;

            Vector2f size = Window.toPixelSize(getSize(), scalesWithGuiSize());
            Vector2f position = Window.toPixelCoordinate(getPosition(), scalesWithGuiSize());
            itemNameDisplay.setText(Language.getTranslation(Materials.getTranslatable(display.material)));
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

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);
        if (!refreshShapePreview) return;

        refreshShapePreview = false;
        Vector3f rotation = shapePreview != null ? shapePreview.getRotation() : null;
        float zoom = shapePreview != null ? shapePreview.getZoom() : 1.0F;
        removeRenderable(shapePreview).delete();

        Vector2f sizeToParent = new Vector2f(0.325F, 0.325F * Window.getAspectRatio() * getAspectRatio());
        Vector2f offsetToParent = new Vector2f(0.0F, 0.5F - sizeToParent.y * 0.5F);
        selectedDisplay.getPlaceable().updateBitMap();
        Structure structure = selectedDisplay.getPlaceable().getStructure();

        shapePreview = new StructureDisplay(sizeToParent, offsetToParent, structure);
        shapePreview.setRotation(rotation);
        shapePreview.changeZoom(zoom);
        shapePreview.setDoAutoFocusScaling(false);
        shapePreview.setScaleWithGuiSize(false);

        addRenderable(shapePreview);
    }

    void refreshShapePreview() {
        refreshShapePreview = true;
    }

    void updateDisplayPositions() {
        InventoryInput input = Game.getPlayer().getInventory().getInput();
        float itemSize = FloatSettings.INVENTORY_ITEM_SIZE.value();
        int itemsPerRow = Math.max(1, (int) Math.floor(0.33333334F / itemSize));

        Vector2f sizeToParent = new Vector2f(itemSize, itemSize * Window.getAspectRatio() * getAspectRatio());

        for (CubeDisplay display : cubeDisplays) {
            int index = display.material & 0xFF;
            int row = index / itemsPerRow, column = index % itemsPerRow;
            float offsetX = 1.0F - sizeToParent.x * (column + 1);
            float offsetY = 1.0F - sizeToParent.y * (row + 1);

            display.display.setOffsetToParent(offsetX, offsetY + input.materialScroll);
            display.display.setSizeToParent(sizeToParent.x, sizeToParent.y);
        }
    }

    ShapeDisplay getSelectedDisplay() {
        return selectedDisplay;
    }

    void setSelectedDisplay(ShapeDisplay selectedDisplay) {
        this.selectedDisplay = selectedDisplay;
    }

    ArrayList<UiBackgroundElement> getShapePlaceableSettingSliders() {
        return shapePlaceableSettingSliders;
    }

    private void loadShapeDisplays() {
        for (Renderable shapeDisplay : shapeDisplays) removeRenderable(shapeDisplay).delete();
        for (Renderable slider : shapePlaceableSettingSliders) removeRenderable(slider).delete();
        shapeDisplays.clear();
        shapePlaceableSettingSliders.clear();

        shapeDisplays.add(new ShapeDisplay(0, new CubePlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(1, new SpherePlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(2, new CylinderPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(3, new ConePlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(4, new SlabPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(5, new EllipsoidPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(6, new StairPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(7, new InsideStairPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(8, new OutsideStairPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(9, new ArcPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(10, new InsideArcPlaceable(STONE).updateBitMap(), this));
        shapeDisplays.add(new ShapeDisplay(11, new OutsideArcPlaceable(STONE).updateBitMap(), this));

        for (Renderable renderable : shapeDisplays) addRenderable(renderable);
        selectedDisplay = shapeDisplays.getFirst();
        for (Renderable renderable : selectedDisplay.getSettingElements()) renderable.setVisible(true);
    }

    private void moveMaterialButtons(float movement) {
        Vector2f offset = new Vector2f(0.0F, movement);
        for (CubeDisplay button : cubeDisplays) button.display.move(offset);
    }

    private final ArrayList<CubeDisplay> cubeDisplays = new ArrayList<>();
    private final ArrayList<ShapeDisplay> shapeDisplays = new ArrayList<>();
    private final ArrayList<UiBackgroundElement> shapePlaceableSettingSliders = new ArrayList<>();
    private final TextElement itemNameDisplay = new TextElement(new Vector2f());
    private final Vector2i lastCursorPos = new Vector2i();

    private ShapeDisplay selectedDisplay;
    private StructureDisplay shapePreview;
    private boolean refreshShapePreview = true;

    private record CubeDisplay(StructureDisplay display, byte material) {

    }
}
