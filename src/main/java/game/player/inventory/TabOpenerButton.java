package game.player.inventory;

import core.renderables.ButtonResult;
import core.renderables.TextElement;
import core.renderables.UiButton;
import core.rendering_api.Window;
import core.utils.StringGetter;

import game.server.Game;

import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public final class TabOpenerButton extends UiButton {

    static final float SIZE = 0.13333334F;

/**
 * Creates a new TabOpenerButton instance.
 *
 * @param inventory Y coordinate in local block coordinates
 * @param index X coordinate in local block coordinates
 * @param toOpenTab parameter
 * @param name parameter
 */
    TabOpenerButton(Inventory inventory, int index, InventoryTab toOpenTab, StringGetter name) {
        super(getButtonSizeToParent(), getButtonOffsetToParent(index));

        this.index = index;
        this.inventory = inventory;
        this.toOpenTab = toOpenTab;

        setScaleWithGuiSize(false);
        setRimThicknessMultiplier(0.75F);
        setDoAutoFocusScaling(false);
        setAction(this::action);
        setScalingFactor(1.1F);

        TextElement textElement = new TextElement(new Vector2f(0.1F, 0.5F), name);
        textElement.setDoAutoFocusScaling(false);
        addRenderable(textElement);
    }

/**
 * Performs render self.
 *
 * @param position parameter
 * @param size parameter
 */
    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        if (Game.getPlayer().getInventory().getOpenTabButton() == this) scaleForFocused(position, size);
        super.renderSelf(position, size);
    }

/**
 * Performs resize self to.
 *
 * @param width parameter
 * @param height parameter
 */
    @Override
    public void resizeSelfTo(int width, int height) {
        Vector2f sizeToParent = getButtonSizeToParent();
        Vector2f offsetToParent = getButtonOffsetToParent(index);

        setSizeToParent(sizeToParent.x, sizeToParent.y);
        setOffsetToParent(offsetToParent.x, offsetToParent.y);
    }

/**
 * Performs action.
 *
 * @param pixelCoordinate parameter
 * @param button parameter
 * @param action parameter
 * @return result
 */
    private ButtonResult action(Vector2i pixelCoordinate, int button, int action) {
        if (action != GLFW_PRESS) return ButtonResult.IGNORE;
        inventory.setOpenTab(toOpenTab, this);
        return ButtonResult.SUCCESS;
    }

    private static Vector2f getButtonSizeToParent() {
        return new Vector2f(SIZE * 0.95F / Window.getAspectRatio(), SIZE * 0.95F);
    }

    private static Vector2f getButtonOffsetToParent(int index) {
        return new Vector2f(0.0F, 1.0F - (index + 1) * SIZE);
    }

    private final int index;
    private final Inventory inventory;
    private final InventoryTab toOpenTab;
}
