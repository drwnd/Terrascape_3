package game.player;

import core.assets.CoreSounds;
import core.renderables.UiElement;
import core.rendering_api.Window;
import core.settings.CoreFloatSettings;
import core.sound.Sound;
import core.utils.MainThread;

import game.assets.Textures;
import game.items.EmptySlot;
import game.items.Item;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.StructurePlaceable;
import game.player.interaction.placeable_shapes.CubePlaceable;
import game.player.interaction.placeable_shapes.CustomShape;
import game.player.inventory.InventoryTab;
import game.player.inventory.ShapesTab;
import game.player.inventory.StructureDisplay;
import game.player.interaction.VoxelTarget;
import game.player.inventory.StructureTab;
import game.server.Game;
import game.server.generation.Structure;
import game.server.material.Material;
import game.settings.FloatSettings;
import game.settings.KeySettings;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

public final class Hotbar extends UiElement {

    public static final int LENGTH = 9;

    @MainThread
    public Hotbar() {
        super(new Vector2f(), new Vector2f(), Textures.HOTBAR);
        Arrays.fill(content, EmptySlot.EMPTY_SLOT);
        setScaleWithGuiSize(false);
        setDoAutoFocusScaling(false);

        hotBarSelectionIndicator = new UiElement(new Vector2f(), new Vector2f(), Textures.HOTBAR_SELECTION_INDICATOR);
        hotBarSelectionIndicator.setScaleWithGuiSize(false);
        hotBarSelectionIndicator.setDoAutoFocusScaling(false);
        addRenderable(hotBarSelectionIndicator);
    }

    @MainThread
    public void handleInput(int button, int action) {
        if (action != GLFW_PRESS) return;

        if (button == KeySettings.HOTBAR_SLOT_1.keybind()) setSelectedSlot(0);
        if (button == KeySettings.HOTBAR_SLOT_2.keybind()) setSelectedSlot(1);
        if (button == KeySettings.HOTBAR_SLOT_3.keybind()) setSelectedSlot(2);
        if (button == KeySettings.HOTBAR_SLOT_4.keybind()) setSelectedSlot(3);
        if (button == KeySettings.HOTBAR_SLOT_5.keybind()) setSelectedSlot(4);
        if (button == KeySettings.HOTBAR_SLOT_6.keybind()) setSelectedSlot(5);
        if (button == KeySettings.HOTBAR_SLOT_7.keybind()) setSelectedSlot(6);
        if (button == KeySettings.HOTBAR_SLOT_8.keybind()) setSelectedSlot(7);
        if (button == KeySettings.HOTBAR_SLOT_9.keybind()) setSelectedSlot(8);

        if (button == KeySettings.DROP.keybind()) setContent(selectedSlot, null);
        if (button == KeySettings.PICK_BLOCK.keybind()) handlePickBlock();
    }

    public Item getSelectedItem() {
        return content[selectedSlot];
    }

    @MainThread
    public void setContent(int slotIndex, byte material) {
        setContent(slotIndex, new CubePlaceable(material));
    }

    @MainThread
    public void setContent(int slotIndex, Item item) {
        slotIndex = clampSlot(slotIndex);
        if (item == null) item = EmptySlot.EMPTY_SLOT;

        playSound(item);
        playSound(content[slotIndex]);

        if (content[slotIndex] instanceof CustomShape customShape) customShape.delete();
        removeRenderable(displays[slotIndex]).delete();
        displays[slotIndex] = null;
        content[slotIndex] = item;

        Structure structure = item instanceof ShapePlaceable shapePlaceable ? shapePlaceable.updateBitMap(false).getSmallStructure() : item.getStructure();
        if (structure == null) return;

        Vector2f sizeToParent = new Vector2f(1.0F / LENGTH, 1.0F);
        Vector2f offsetToParent = new Vector2f((float) slotIndex / LENGTH, 0.0F);
        displays[slotIndex] = new StructureDisplay(sizeToParent, offsetToParent, structure, item.rotateStructureDisplay());
        displays[slotIndex].setScaleWithGuiSize(false);
        addRenderable(displays[slotIndex]);
    }


    @Override
    @MainThread
    public void renderSelf(Vector2f position, Vector2f size) {
        float hotbarSize = FloatSettings.HOTBAR_SIZE.value();
        setOffsetToParent(0.5F - hotbarSize * Hotbar.LENGTH * 0.5F, 0);
        setSizeToParent(hotbarSize * Hotbar.LENGTH, hotbarSize * Window.getAspectRatio());
        super.renderSelf(getPosition(), getSize());

        float hotbarIndicatorScaler = FloatSettings.HOTBAR_INDICATOR_SCALER.value();
        float scalingOffset = (1.0F - hotbarIndicatorScaler) * 0.5F;
        hotBarSelectionIndicator.setOffsetToParent((selectedSlot + scalingOffset) / LENGTH, scalingOffset);
        hotBarSelectionIndicator.setSizeToParent(hotbarIndicatorScaler / LENGTH, hotbarIndicatorScaler);
    }

    @Override
    @MainThread
    public boolean clickOn(Vector2i pixelCoordinate, int button, int action) {
        if (action != GLFW_PRESS) return false;

        float relativeX = (float) pixelCoordinate.x / Window.getWidth();
        int slot = Math.clamp((int) ((relativeX - getOffsetToParent().x) * LENGTH / getSizeToParent().x), 0, LENGTH - 1);
        if (content[slot] == EmptySlot.EMPTY_SLOT) return true;

        switch (content[slot]) {
            case CustomShape customShape -> {
                Game.getPlayer().getInventory().setOpenTab(InventoryTab.Types.CUSTOM_SHAPES);
                Sound.playUI(Material.getStepSounds(customShape.getMaterial()), FloatSettings.INVENTORY_AUDIO);
            }
            case ShapePlaceable shapePlaceable -> {
                Game.getPlayer().getInventory().setOpenTab(InventoryTab.Types.SHAPES);
                Sound.playUI(Material.getStepSounds(shapePlaceable.getMaterial()), FloatSettings.INVENTORY_AUDIO);
                ((ShapesTab) InventoryTab.Types.SHAPES.getButton().getToOpenTab()).selectDisplayOfClass(shapePlaceable);
            }
            case StructurePlaceable structurePlaceable -> {
                Game.getPlayer().getInventory().setOpenTab(InventoryTab.Types.STRUCTURES);
                Sound.playUI(CoreSounds.BUTTON_SUCCESS, CoreFloatSettings.UI_AUDIO);
                ((StructureTab) InventoryTab.Types.STRUCTURES.getButton().getToOpenTab()).selectStructure(structurePlaceable.getStructure());
            }
            default -> {
                Game.getPlayer().getInventory().setOpenTab(InventoryTab.Types.MISCELLANEOUS);
                Sound.playUI(CoreSounds.BUTTON_SUCCESS, CoreFloatSettings.UI_AUDIO);
            }
        }
        return true;
    }


    private int nextFreeSlot() {
        for (int slot = selectedSlot; slot < selectedSlot + LENGTH; slot++)
            if (content[slot % LENGTH] == EmptySlot.EMPTY_SLOT) return slot % LENGTH;
        return selectedSlot;
    }

    private int indexOf(byte material) {
        for (int slot = 0; slot < LENGTH; slot++) {
            Item placeable = content[slot];
            if (placeable instanceof CubePlaceable && ((CubePlaceable) placeable).getMaterial() == material) return slot;
        }
        return -1;
    }

    @MainThread
    private void handlePickBlock() {
        VoxelTarget target = VoxelTarget.getPlayerTarget();
        if (target == null) return;

        int slot = indexOf(target.material());
        if (slot != -1) {
            setSelectedSlot(slot);
            return;
        }

        slot = nextFreeSlot();
        setContent(slot, target.material());
        setSelectedSlot(slot);
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    @MainThread
    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = clampSlot(selectedSlot);
        if (content[selectedSlot] instanceof ShapePlaceable shapePlaceable) shapePlaceable.updateBitMap(false);
        playSound(content[this.selectedSlot]);
    }

    public Item[] getContent() {
        return content;
    }

    @MainThread
    public void setContent(Item[] content) {
        if (content.length != LENGTH) return;
        for (int slot = 0; slot < LENGTH; slot++) setContent(slot, content[slot]);
    }


    private static int clampSlot(int slot) {
        slot %= LENGTH;
        if (slot < 0) slot += LENGTH;
        return slot;
    }

    private static void playSound(Item placeable) {
        if (placeable instanceof ShapePlaceable shapePlaceable)
            Sound.playUI(Material.getStepSounds(shapePlaceable.getMaterial()), FloatSettings.INVENTORY_AUDIO);
    }


    private int selectedSlot = 0;
    private final Item[] content = new Item[LENGTH];
    private final UiElement hotBarSelectionIndicator;
    private final StructureDisplay[] displays = new StructureDisplay[LENGTH];
}
