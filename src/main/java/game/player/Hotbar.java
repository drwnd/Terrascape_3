package game.player;

import core.settings.FloatSetting;
import core.settings.KeySetting;
import core.renderables.UiElement;
import core.rendering_api.Window;

import game.assets.Textures;
import game.player.interaction.CubePlaceable;
import game.player.interaction.Placeable;
import game.player.rendering.StructureDisplay;
import game.player.interaction.Target;

import game.server.generation.Structure;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public final class Hotbar extends UiElement {

    public static final int LENGTH = 9;

    public Hotbar() {
        super(new Vector2f(), new Vector2f(), Textures.HOTBAR);
        setScaleWithGuiSize(false);
        setAllowFocusScaling(false);

        hotBarSelectionIndicator = new UiElement(new Vector2f(), new Vector2f(), Textures.HOTBAR_SELECTION_INDICATOR);
        hotBarSelectionIndicator.setScaleWithGuiSize(false);
        hotBarSelectionIndicator.setAllowFocusScaling(false);
        addRenderable(hotBarSelectionIndicator);
    }


    public void handleInput(int button, int action) {
        if (action != GLFW_PRESS) return;

        if (button == KeySetting.HOTBAR_SLOT_1.keybind()) setSelectedSlot(0);
        if (button == KeySetting.HOTBAR_SLOT_2.keybind()) setSelectedSlot(1);
        if (button == KeySetting.HOTBAR_SLOT_3.keybind()) setSelectedSlot(2);
        if (button == KeySetting.HOTBAR_SLOT_4.keybind()) setSelectedSlot(3);
        if (button == KeySetting.HOTBAR_SLOT_5.keybind()) setSelectedSlot(4);
        if (button == KeySetting.HOTBAR_SLOT_6.keybind()) setSelectedSlot(5);
        if (button == KeySetting.HOTBAR_SLOT_7.keybind()) setSelectedSlot(6);
        if (button == KeySetting.HOTBAR_SLOT_8.keybind()) setSelectedSlot(7);
        if (button == KeySetting.HOTBAR_SLOT_9.keybind()) setSelectedSlot(8);

        if (button == KeySetting.DROP.keybind()) setContent(selectedSlot, null);
        if (button == KeySetting.PICK_BLOCK.keybind()) handlePickBlock();
    }

    public Placeable getSelectedMaterial() {
        return contents[selectedSlot];
    }

    public void setContent(int slotIndex, byte material) {
        setContent(slotIndex, new CubePlaceable(material));
    }

    public void setContent(int slotIndex, Placeable placeable) {
        slotIndex = clampSlot(slotIndex);
        contents[slotIndex] = placeable;

        getChildren().remove(displays[slotIndex]);
        if (displays[slotIndex] != null) displays[slotIndex].delete();
        displays[slotIndex] = null;

        if (placeable == null) return;
        Structure structure = placeable.getStructure();
        if (structure == null) return;

        displays[slotIndex] = new StructureDisplay(new Vector2f(1.0F / LENGTH, 1.0F), new Vector2f((float) slotIndex / LENGTH, 0.0F), structure);
        displays[slotIndex].setScaleWithGuiSize(false);
        addRenderable(displays[slotIndex]);
    }


    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        float hotbarSize = FloatSetting.HOTBAR_SIZE.value();
        setOffsetToParent(0.5F - hotbarSize * Hotbar.LENGTH * 0.5F, 0);
        setSizeToParent(hotbarSize * Hotbar.LENGTH, hotbarSize * Window.getAspectRatio());
        super.renderSelf(getPosition(), getSize());

        float hotbarIndicatorScaler = FloatSetting.HOTBAR_INDICATOR_SCALER.value();
        float scalingOffset = (1.0F - hotbarIndicatorScaler) * 0.5F;
        hotBarSelectionIndicator.setOffsetToParent((selectedSlot + scalingOffset) / LENGTH, scalingOffset);
        hotBarSelectionIndicator.setSizeToParent(hotbarIndicatorScaler / LENGTH, hotbarIndicatorScaler);
    }


    private int nextFreeSlot() {
        for (int slot = selectedSlot; slot < selectedSlot + LENGTH; slot++)
            if (contents[slot % LENGTH] == null) return slot % LENGTH;
        return selectedSlot;
    }

    private int indexOf(byte material) {
        for (int slot = 0; slot < LENGTH; slot++) {
            Placeable placeable = contents[slot];
            if (placeable instanceof CubePlaceable && ((CubePlaceable) placeable).getMaterial() == material) return slot;
        }
        return -1;
    }

    private void handlePickBlock() {
        Target target = Target.getPlayerTarget();
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

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = clampSlot(selectedSlot);
    }

    public Placeable[] getContents() {
        return contents;
    }

    public void setContents(Placeable[] contents) {
        if (contents.length != LENGTH) return;
        for (int slot = 0; slot < LENGTH; slot++) setContent(slot, contents[slot]);
    }


    private static int clampSlot(int slot) {
        slot %= LENGTH;
        if (slot < 0) slot += LENGTH;
        return slot;
    }


    private int selectedSlot = 0;
    private final Placeable[] contents = new Placeable[LENGTH];
    private final UiElement hotBarSelectionIndicator;
    private final StructureDisplay[] displays = new StructureDisplay[LENGTH];
}
