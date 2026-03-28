package game.player.inventory;

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

    @Override
    public void resizeSelfTo(int width, int height) {
        Vector2f sizeToParent = getButtonSizeToParent();
        Vector2f offsetToParent = getButtonOffsetToParent(index);

        setSizeToParent(sizeToParent.x, sizeToParent.y);
        setOffsetToParent(offsetToParent.x, offsetToParent.y);
    }

    private void action(Vector2i pixelCoordinate, int button, int action) {
        if (action != GLFW_PRESS) return;
        inventory.setOpenTab(toOpenTab, this);
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
