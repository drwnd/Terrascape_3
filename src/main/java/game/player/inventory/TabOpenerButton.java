package game.player.inventory;

import core.renderables.Clickable;
import core.renderables.TextElement;
import core.renderables.UiButton;
import core.rendering_api.Window;
import core.utils.StringGetter;

import game.server.Game;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class TabOpenerButton extends UiButton {

    static final float SIZE = 0.075F;

    TabOpenerButton(Inventory inventory, int index, InventoryTab toOpenTab, StringGetter name) {
        super(new Vector2f(SIZE * 0.95F, SIZE * 0.95F * Window.getAspectRatio()), new Vector2f(0.0F, 1.0F - (index + 1) * SIZE * Window.getAspectRatio()));

        setScaleWithGuiSize(false);
        setRimThicknessMultiplier(0.75F);
        setDoAutoFocusScaling(false);
        setAction(getAction(inventory, toOpenTab));
        setScalingFactor(1.1F);

        TextElement textElement = new TextElement(new Vector2f(0.1F, 0.5F), name);
        textElement.setDoAutoFocusScaling(false);
        addRenderable(textElement);
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        if (Game.getPlayer().getInventory().openTabButton == this) scaleForFocused(position, size);
        super.renderSelf(position, size);
    }

    private Clickable getAction(Inventory inventory, InventoryTab toOpenTab) {
        return (Vector2i _, int _, int action) -> {
            if (action != GLFW_PRESS) return;
            inventory.structureTab.setVisible(false);
            inventory.shapesTab.setVisible(false);
            inventory.miscellaneousTab.setVisible(false);

            inventory.openInventoryTab = toOpenTab;
            inventory.openInventoryTab.setVisible(true);

            inventory.openTabButton = this;
        };
    }
}
