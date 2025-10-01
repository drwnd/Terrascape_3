package game.player;

import core.assets.identifiers.TextureIdentifier;
import core.settings.KeySetting;
import game.player.rendering.StructureDisplay;
import core.renderables.UiElement;
import core.rendering_api.Window;
import core.settings.FloatSetting;

import game.player.interaction.Target;
import game.server.generation.Structure;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;


import static game.utils.Constants.*;

public final class Hotbar extends UiElement {

    public static final int LENGTH = 9;

    public Hotbar() {
        super(new Vector2f(), new Vector2f(), TextureIdentifier.HOTBAR);
        setScaleWithGuiSize(false);
        setAllowFocusScaling(false);

        hotBarSelectionIndicator = new UiElement(new Vector2f(), new Vector2f(), TextureIdentifier.HOTBAR_SELECTION_INDICATOR);
        hotBarSelectionIndicator.setScaleWithGuiSize(false);
        hotBarSelectionIndicator.setAllowFocusScaling(false);
        addRenderable(hotBarSelectionIndicator);
    }


    public void handleInput(int button, int action) {
        if (action != GLFW.GLFW_PRESS) return;

        if (button == KeySetting.HOTBAR_SLOT_1.value()) setSelectedSlot(0);
        if (button == KeySetting.HOTBAR_SLOT_2.value()) setSelectedSlot(1);
        if (button == KeySetting.HOTBAR_SLOT_3.value()) setSelectedSlot(2);
        if (button == KeySetting.HOTBAR_SLOT_4.value()) setSelectedSlot(3);
        if (button == KeySetting.HOTBAR_SLOT_5.value()) setSelectedSlot(4);
        if (button == KeySetting.HOTBAR_SLOT_6.value()) setSelectedSlot(5);
        if (button == KeySetting.HOTBAR_SLOT_7.value()) setSelectedSlot(6);
        if (button == KeySetting.HOTBAR_SLOT_8.value()) setSelectedSlot(7);
        if (button == KeySetting.HOTBAR_SLOT_9.value()) setSelectedSlot(8);

        if (button == KeySetting.DROP.value()) setContent(selectedSlot, AIR);
        if (button == KeySetting.PICK_BLOCK.value()) handlePickBlock();
    }

    public byte getSelectedMaterial() {
        return contents[selectedSlot];
    }

    public void setContent(int slotIndex, byte material) {
        slotIndex = clampSlot(slotIndex);
        contents[slotIndex] = material;

        if (displays[slotIndex] != null) displays[slotIndex].delete();
        getChildren().remove(displays[slotIndex]);

        displays[slotIndex] = new StructureDisplay(new Vector2f(1.0f / LENGTH, 1.0f), new Vector2f((float) slotIndex / LENGTH, 0.0f), new Structure(material));
        displays[slotIndex].setScaleWithGuiSize(false);
        addRenderable(displays[slotIndex]);
    }


    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        float hotbarSize = FloatSetting.HOTBAR_SIZE.value();
        setOffsetToParent(0.5f - hotbarSize * Hotbar.LENGTH * 0.5f, 0);
        setSizeToParent(hotbarSize * Hotbar.LENGTH, hotbarSize * Window.getAspectRatio());
        super.renderSelf(getPosition(), getSize());

        float hotbarIndicatorScaler = FloatSetting.HOTBAR_INDICATOR_SCALER.value();
        float scalingOffset = (1.0f - hotbarIndicatorScaler) * 0.5f;
        hotBarSelectionIndicator.setOffsetToParent((selectedSlot + scalingOffset) / LENGTH, scalingOffset);
        hotBarSelectionIndicator.setSizeToParent(hotbarIndicatorScaler / LENGTH, hotbarIndicatorScaler);
    }


    private int clampSlot(int slot) {
        slot %= LENGTH;
        if (slot < 0) slot += LENGTH;
        return slot;
    }

    private int nextFreeSlot() {
        for (int slot = selectedSlot; slot < selectedSlot + LENGTH; slot++)
            if (contents[slot % LENGTH] == AIR) return slot % LENGTH;
        return selectedSlot;
    }

    private int indexOf(byte material) {
        for (int slot = 0; slot < LENGTH; slot++)
            if (contents[slot] == material) return slot;
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

    public byte[] getContents() {
        return contents;
    }

    public void setContent(byte[] contents) {
        for (int slot = 0; slot < LENGTH; slot++) setContent(slot, contents[slot]);
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = clampSlot(selectedSlot);
    }


    private int selectedSlot = 0;
    private final UiElement hotBarSelectionIndicator;
    private final byte[] contents = new byte[LENGTH];
    private final StructureDisplay[] displays = new StructureDisplay[LENGTH];
}
