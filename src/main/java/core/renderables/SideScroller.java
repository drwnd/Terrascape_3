package core.renderables;

import core.assets.CoreTextures;
import core.rendering_api.MenuInput;
import core.rendering_api.Window;
import core.utils.MainThread;
import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

public class SideScroller extends UiButton {

    @MainThread
    public SideScroller(Vector2f sizeToParent, Vector2f offsetToParent, MenuInput<?> input) {
        super(sizeToParent, offsetToParent);
        this.input = input;
        setAction(this::setValueOnClick);

        slider = new UiBackgroundElement(new Vector2f(1.0F, 0.5625F * Window.getAspectRatio() * getAspectRatio()), new Vector2f());
        slider.setTextureIdentifier(CoreTextures.SLIDER_ICON);
        addRenderable(slider);
    }

    public void setInput(MenuInput<?> input) {
        this.input = input;
    }

    @MainThread
    protected void applyScrolling(Vector2i cursorPos, Vector2f position, Vector2f size) {
        float fraction = 1 - (cursorPos.y - position.y) / size.y;
        fraction = Math.clamp(fraction, 0.0F, 1.0F);
        input.setScroll(input.maxScrollGetter.getMaxScroll() * fraction);
        slider.setOffsetToParent(0.0F, fraction - slider.getSizeToParent().y * 0.5F);
    }

    @Override
    @MainThread
    public void dragOver(Vector2i pixelCoordinate) {
        setValueOnClick(pixelCoordinate, GLFW_MOUSE_BUTTON_LEFT, GLFW_HOVERED);
    }

    @Override
    @MainThread
    protected void renderSelf(Vector2f position, Vector2f size) {
        super.renderSelf(position, size);
        float fraction = 1 - input.getScroll() / input.maxScrollGetter.getMaxScroll();
        slider.setOffsetToParent(0.0F, fraction - slider.getSizeToParent().y * 0.5F);
    }

    @Override
    @MainThread
    protected void resizeSelfTo(int width, int height) {
        slider.setSizeToParent(1.0F, 0.5625F * Window.getAspectRatio() * getAspectRatio());
    }

    @Override
    public boolean isVisible() {
        if (!super.isVisible()) return false;
        float maxScroll = input.maxScrollGetter.getMaxScroll();
        return maxScroll > 0 && maxScroll != Float.POSITIVE_INFINITY;
    }

    @MainThread
    private ButtonResult setValueOnClick(Vector2i cursorPos, int button, int action) {
        DraggableInfo info = getOwnDraggableInfo(action);
        if (info == null) return ButtonResult.IGNORE;

        applyScrolling(cursorPos, info.position(), info.size());
        return ButtonResult.SUCCESS;
    }

    protected MenuInput<?> input;
    protected final UiBackgroundElement slider;
}
