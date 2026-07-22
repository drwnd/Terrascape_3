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
     * Initializes a tab opener button for the inventory.
     * @param inventory the parent inventory
     * @param index the position index of the button
     * @param toOpenTab the tab that this button will open
     * @param name the translatable name of the tab
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

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        if (Game.getPlayer().getInventory().getOpenTabButton() == this) scaleForFocused(position, size);
        super.renderSelf(position, size);
    }

    /**
     * Resizes and repositions the button when the window size changes.
     * @param width the new window width
     * @param height the new window height
     */
    @Override
    public void resizeSelfTo(int width, int height) {
        Vector2f sizeToParent = getButtonSizeToParent();
        Vector2f offsetToParent = getButtonOffsetToParent(index);

        setSizeToParent(sizeToParent.x, sizeToParent.y);
        setOffsetToParent(offsetToParent.x, offsetToParent.y);
    }

    /**
     * The action performed when the button is clicked.
     * @param pixelCoordinate the current mouse position in screen pixels
     * @param button the mouse button
     * @param action the action (press/release)
     * @return the result of the button action
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
