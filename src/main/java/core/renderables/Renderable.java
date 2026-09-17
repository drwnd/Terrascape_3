package core.renderables;

import core.assets.CoreSounds;
import core.rendering_api.Window;
import core.settings.CoreFloatSettings;
import core.sound.Sound;

import core.utils.MainThread;
import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class Renderable {

    @MainThread
    public Renderable(Vector2f sizeToParent, Vector2f offsetToParent) {
        this.sizeToParent = new Vector2f(sizeToParent);
        this.offsetToParent = new Vector2f(offsetToParent);
    }

    @MainThread
    public void scaleForFocused(Vector2f position, Vector2f size) {
        float dx = (size.x - size.x * scalingFactor) * 0.5F;
        float dy = (size.y - size.y * scalingFactor) * 0.5F;

        size.mul(scalingFactor);
        position.add(dx, dy);
    }

    @MainThread
    public final void render(Vector2f parentPosition, Vector2f parentSize) {
        if (!isVisible()) return;
        Vector2f thisSize = new Vector2f(parentSize.x, parentSize.y).mul(sizeToParent);
        Vector2f thisPosition = new Vector2f(
                parentPosition.x + parentSize.x * offsetToParent.x,
                parentPosition.y + parentSize.y * offsetToParent.y);
        if (isFocused() && allowsFocusScaling()) scaleForFocused(thisPosition, thisSize);

        renderSelf(thisPosition, thisSize);
        for (Renderable child : children) child.render(thisPosition, thisSize);
    }

    @MainThread
    public final void resize(Vector2i size, float parentSizeX, float parentSizeY) {
        float sizeX = parentSizeX * sizeToParent.x;
        float sizeY = parentSizeY * sizeToParent.y;
        resizeSelfTo((int) (size.x * sizeX), (int) (size.y * sizeY));
        for (Renderable child : children) child.resize(size, sizeX, sizeY);
    }

    @MainThread
    public final void delete() {
        deleteSelf();
        for (Renderable renderable : children) renderable.delete();
    }

    @MainThread
    public void addRenderable(Renderable renderable) {
        children.add(renderable);
        renderable.parent = this;
    }

    @MainThread
    public Renderable removeRenderable(Renderable renderable) {
        return children.remove(renderable) && renderable != null ? renderable : DummyRenderable.dummy;
    }

    @MainThread
    public boolean clickOn(Vector2i pixelCoordinate, int mouseButton, int action) {
        for (Renderable renderable : children)
            if (renderable.isVisible() && renderable.containsPixelCoordinate(pixelCoordinate) && renderable.clickOn(pixelCoordinate, mouseButton, action))
                return true;
        return false;
    }

    @MainThread
    public void hoverOver(Vector2i pixelCoordinate) {
        if (isFocused()) return;
        for (Renderable renderable : children)
            renderable.setFocused(renderable.containsPixelCoordinate(pixelCoordinate));
    }

    @MainThread
    public void dragOver(Vector2i pixelCoordinate) {
        if (selectedDraggable != null) selectedDraggable.dragOver(pixelCoordinate);
        hoverOver(pixelCoordinate);
        for (Renderable renderable : children)
            if (renderable.isVisible() && renderable.containsPixelCoordinate(pixelCoordinate)) renderable.dragOver(pixelCoordinate);
    }

    @MainThread
    public void move(Vector2f offset) {
        offsetToParent.add(offset);
    }

    @MainThread
    public boolean containsPixelCoordinate(Vector2i pixelCoordinate) {
        Vector2f position = getPosition(), size = getSize();
        if (isFocused()) scaleForFocused(position, size);

        position = Window.toPixelCoordinate(position, scalesWithGuiSize());
        size = Window.toPixelSize(size, scalesWithGuiSize());

        return position.x <= pixelCoordinate.x && position.x + size.x >= pixelCoordinate.x && position.y <= pixelCoordinate.y && position.y + size.y >= pixelCoordinate.y;
    }


    @MainThread
    protected final DraggableInfo getOwnDraggableInfo(int action) {
        if (action == GLFW_HOVERED && selectedDraggable != this) return null;
        if (action == GLFW_PRESS) selectedDraggable = this;
        if (action == GLFW_RELEASE)
            if (selectedDraggable == this) selectedDraggable = null;
            else return null;

        Vector2f position = getPosition(), size = getSize();
        scaleForFocused(position, size);

        position = Window.toPixelCoordinate(position, scalesWithGuiSize());
        size = Window.toPixelSize(size, scalesWithGuiSize());
        return new DraggableInfo(position, size);
    }

    // Override if needed
    @MainThread
    protected void renderSelf(Vector2f position, Vector2f size) {

    }

    // Override if needed
    @MainThread
    protected void resizeSelfTo(int width, int height) {

    }

    // Override if needed
    @MainThread
    protected void deleteSelf() {

    }

    // Override if needed
    @MainThread
    public void setOnTop() {

    }

    @MainThread
    public float getAspectRatio() {
        Vector2f size = getSize();
        return size.x / size.y;
    }

    @MainThread
    public Vector2f getPosition() {
        return parent.getPosition().add(parent.getSize().mul(offsetToParent));
    }

    @MainThread
    public Vector2f getSize() {
        return parent.getSize().mul(sizeToParent);
    }

    @MainThread
    public Vector2f getOffsetToParent() {
        return offsetToParent;
    }

    @MainThread
    public Vector2f getSizeToParent() {
        return sizeToParent;
    }

    @MainThread
    public ArrayList<Renderable> getChildren() {
        return children;
    }

    @SuppressWarnings("unchecked")
    @MainThread
    public <T extends Renderable> T firstChildOf(Class<T> type) {
        for (Renderable child : children) if (type.isInstance(child)) return (T) child;
        return null;
    }

    @MainThread
    public Renderable getParent() {
        return parent;
    }

    @MainThread
    public void setOffsetToParent(float x, float y) {
        this.offsetToParent.set(x, y);
    }

    @MainThread
    public void setSizeToParent(float x, float y) {
        this.sizeToParent.set(x, y);
    }

    public boolean isVisible() {
        return isFlag(VISIBILITY_MASK);
    }

    @MainThread
    public boolean isFocused() {
        return isFlag(FOCUSSED_MASK);
    }

    @MainThread
    public boolean allowsFocusScaling() {
        return isFlag(DO_AUTO_FOCUS_SCALING);
    }

    @MainThread
    public boolean scalesWithGuiSize() {
        return isFlag(SCALES_WITH_GUI_SIZE_MASK) && parent.scalesWithGuiSize();
    }

    @MainThread
    public void setPlayFocusSound(boolean playFocusSound) {
        setFlag(playFocusSound, PLAY_FOCUS_SOUNDS_MASK);
    }

    @MainThread
    public void setScaleWithGuiSize(boolean scaleWithGuiSize) {
        setFlag(scaleWithGuiSize, SCALES_WITH_GUI_SIZE_MASK);
    }

    @MainThread
    public void setDoAutoFocusScaling(boolean allowScaling) {
        setFlag(allowScaling, DO_AUTO_FOCUS_SCALING);
        if (!isFlag(DO_AUTO_FOCUS_SCALING)) setFlag(false, FOCUSSED_MASK);
    }

    @MainThread
    public void setVisible(boolean visible) {
        setFlag(visible, VISIBILITY_MASK);
    }

    @MainThread
    public void setFocused(boolean focused) {
        if (!isVisible() || !isFlag(DO_AUTO_FOCUS_SCALING) || isFocused() == focused) return;

        if (isFlag(PLAY_FOCUS_SOUNDS_MASK))
            Sound.playUI(CoreSounds.BUTTON_SUCCESS, CoreFloatSettings.UI_AUDIO, 0.35F, 0.5F);

        setFlag(focused, FOCUSSED_MASK);
        if (isFocused()) return;
        for (Renderable renderable : children) renderable.setFocused(false);
    }

    @MainThread
    public void setScalingFactor(float scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    @MainThread
    public float getScalingFactor() {
        return scalingFactor;
    }

    private boolean isFlag(int flagMask) {
        return (flags & flagMask) == flagMask;
    }

    @MainThread
    private void setFlag(boolean value, int mask) {
        if (value) flags |= mask;
        else flags &= ~mask;
    }


    @MainThread
    public static void releaseSelectedDraggable() {
        selectedDraggable = null;
    }


    private final ArrayList<Renderable> children = new ArrayList<>();
    private final Vector2f sizeToParent;
    private final Vector2f offsetToParent;
    private Renderable parent = DummyRenderable.dummy;
    private float scalingFactor = 1.05F;
    private int flags = VISIBILITY_MASK | DO_AUTO_FOCUS_SCALING | SCALES_WITH_GUI_SIZE_MASK | PLAY_FOCUS_SOUNDS_MASK;

    protected static Renderable selectedDraggable;

    protected record DraggableInfo(Vector2f position, Vector2f size) {
    }

    private static final int VISIBILITY_MASK = 0x1;
    private static final int FOCUSSED_MASK = 0x2;
    private static final int DO_AUTO_FOCUS_SCALING = 0x4;
    private static final int SCALES_WITH_GUI_SIZE_MASK = 0x8;
    private static final int PLAY_FOCUS_SOUNDS_MASK = 0x10;
}
