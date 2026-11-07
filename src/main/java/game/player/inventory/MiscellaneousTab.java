package game.player.inventory;

import core.renderables.Renderable;
import core.renderables.TextElement;
import core.rendering_api.Window;
import core.utils.MainThread;

import game.assets.EntityType;
import game.items.Item;
import game.items.SpawnEgg;
import game.player.interaction.ChunkRebuildPlaceable;
import game.player.interaction.StructureSelector;

import game.settings.FloatSettings;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

public final class MiscellaneousTab extends Renderable implements InventoryTab {

    @MainThread
    public MiscellaneousTab(Vector2f sizeToParent, Vector2f offsetToParent) {
        super(sizeToParent, offsetToParent);
        setVisible(false);
        setDoAutoFocusScaling(false);
        setScaleWithGuiSize(false);

        itemNameDisplay.setAddTransparentBackground(true);
        itemNameDisplay.setDoAutoFocusScaling(false);
        itemNameDisplay.setScaleWithGuiSize(false);

        float width = FloatSettings.INVENTORY_ITEM_SIZE.value() * 2, height = width * getAspectRatio() * Window.getAspectRatio();
        Vector2f size = new Vector2f(width, height);
        int itemsPerColumn = Math.max(1, (int) (1 / height));

        addItem(size, width, height, itemsPerColumn, new ChunkRebuildPlaceable());
        addItem(size, width, height, itemsPerColumn, new StructureSelector());
        for (EntityType entityType : EntityType.values()) addItem(size, width, height, itemsPerColumn, new SpawnEgg(entityType));

        addRenderable(itemNameDisplay);
    }

    @Override
    @MainThread
    public Item getSelectedItem(Vector2i pixelCoordinate) {
        for (ItemDisplay display : displays) if (display.display.containsPixelCoordinate(pixelCoordinate)) return display.item;
        return null;
    }

    @Override
    public void handleScroll(Vector2i pixelCoordinate, double yScroll) {

    }

    @Override
    @MainThread
    public void hoverOver(Vector2i pixelCoordinate) {
        Item item = getSelectedItem(pixelCoordinate);
        itemNameDisplay.setVisible(item != null);
        if (item == null) return;
        itemNameDisplay.setText(item);
        Vector2f size = Window.toPixelSize(getSize(), scalesWithGuiSize());
        Vector2f position = Window.toPixelCoordinate(getPosition(), scalesWithGuiSize());
        itemNameDisplay.setOffsetToParent(
                (pixelCoordinate.x - position.x) / size.x,
                (pixelCoordinate.y - position.y) / size.y);
    }

    @Override
    public float getMaxScroll(Vector2i pixelCoordinate) {
        return 0;
    }

    @MainThread
    private void addItem(Vector2f size, float width, float height, int itemsPerColumn, Item item) {
        int index = displays.size();
        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
        float x = 0.05F + (index / itemsPerColumn) * width;
        float y = 0.95F - ((index % itemsPerColumn) + 1) * height;
        StructureDisplay structureDisplay = new StructureDisplay(size, new Vector2f(x, y), item.getStructure(), item.rotateStructureDisplay());
        addRenderable(structureDisplay);
        displays.add(new ItemDisplay(structureDisplay, item));
    }

    private final ArrayList<ItemDisplay> displays = new ArrayList<>();
    private final TextElement itemNameDisplay = new TextElement(new Vector2f());

    private record ItemDisplay(StructureDisplay display, Item item) {}
}
