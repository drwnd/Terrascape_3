package core.renderables;

import core.rendering_api.Window;

import org.joml.Vector2f;
import org.joml.Vector2i;

import java.util.ArrayList;

public class Renderable {

    public Renderable(Vector2f sizeToParent, Vector2f offsetToParent) {
        this.sizeToParent = new Vector2f(sizeToParent);
        this.offsetToParent = new Vector2f(offsetToParent);
    }

    public void scaleForFocused(Vector2f position, Vector2f size) {
        float dx = (size.x - size.x * scalingFactor) * 0.5F;
        float dy = (size.y - size.y * scalingFactor) * 0.5F;

        size.mul(scalingFactor);
        position.add(dx, dy);
    }

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

    public final void resize(Vector2i size, float parentSizeX, float parentSizeY) {
        float sizeX = parentSizeX * sizeToParent.x;
        float sizeY = parentSizeY * sizeToParent.y;
        resizeSelfTo((int) (size.x * sizeX), (int) (size.y * sizeY));
        for (Renderable child : children) child.resize(size, sizeX, sizeY);
    }

    public final void delete() {
        deleteSelf();
        for (Renderable renderable : children) renderable.delete();
    }

    public void addRenderable(Renderable renderable) {
        children.add(renderable);
        renderable.parent = this;
    }

    public void clickOn(Vector2i pixelCoordinate, int mouseButton, int action) {
        for (Renderable renderable : children)
            if (renderable.isVisible() && renderable.containsPixelCoordinate(pixelCoordinate)) renderable.clickOn(pixelCoordinate, mouseButton, action);
    }

    public void hoverOver(Vector2i pixelCoordinate) {
        if (isFocused()) return;
        for (Renderable renderable : children)
            if (renderable.isVisible()) renderable.setFocused(renderable.containsPixelCoordinate(pixelCoordinate));
    }

    public void dragOver(Vector2i pixelCoordinate) {
        hoverOver(pixelCoordinate);
    }

    public void move(Vector2f offset) {
        offsetToParent.add(offset);
    }

    public boolean containsPixelCoordinate(Vector2i pixelCoordinate) {
        Vector2f position = Window.toPixelCoordinate(getPosition(), scalesWithGuiSize());
        Vector2f size = Window.toPixelSize(getSize(), scalesWithGuiSize());

        return position.x <= pixelCoordinate.x && position.x + size.x >= pixelCoordinate.x && position.y <= pixelCoordinate.y && position.y + size.y >= pixelCoordinate.y;
    }


    // Override if needed
    protected void renderSelf(Vector2f position, Vector2f size) {

    }

    // Override if needed
    protected void resizeSelfTo(int width, int height) {

    }

    // Override if needed
    protected void deleteSelf() {

    }

    // Override if needed
    public void setOnTop() {

    }

    public Vector2f getPosition() {
        return parent.getPosition().add(parent.getSize().mul(offsetToParent));
    }

    public Vector2f getSize() {
        return parent.getSize().mul(sizeToParent);
    }

    public Vector2f getOffsetToParent() {
        return offsetToParent;
    }

    public Vector2f getSizeToParent() {
        return sizeToParent;
    }

    public ArrayList<Renderable> getChildren() {
        return children;
    }

    @SuppressWarnings("unchecked")
    public <T extends Renderable> T firstChildOf(Class<T> type) {
        for (Renderable child : children) if (type.isInstance(child)) return (T) child;
        return null;
    }

    public Renderable getParent() {
        return parent;
    }

    public void setOffsetToParent(float x, float y) {
        this.offsetToParent.set(x, y);
    }

    public void setSizeToParent(float x, float y) {
        this.sizeToParent.set(x, y);
    }

    public boolean isVisible() {
        return isFlag(VISIBILITY_MASK);
    }

    public boolean isFocused() {
        return isFlag(FOCUSSED_MASK);
    }

    public boolean allowsFocusScaling() {
        return isFlag(ALLOW_FOCUS_SCALING_MASK);
    }

    public boolean scalesWithGuiSize() {
        return isFlag(SCALES_WITH_GUI_SIZE_MASK) && parent.scalesWithGuiSize();
    }

    public void setScaleWithGuiSize(boolean scaleWithGuiSize) {
        setFlag(scaleWithGuiSize, SCALES_WITH_GUI_SIZE_MASK);
    }

    public void setAllowFocusScaling(boolean allowScaling) {
        setFlag(allowScaling, ALLOW_FOCUS_SCALING_MASK);
        if (!isFlag(ALLOW_FOCUS_SCALING_MASK)) setFlag(false, FOCUSSED_MASK);
    }

    public void setVisible(boolean visible) {
        setFlag(visible, VISIBILITY_MASK);
    }

    public void setFocused(boolean focused) {
        if (!isFlag(ALLOW_FOCUS_SCALING_MASK)) return;
        setFlag(focused, FOCUSSED_MASK);

        if (isFocused()) return;
        for (Renderable renderable : children) renderable.setFocused(false);
    }

    public void setScalingFactor(float scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    public float getScalingFactor() {
        return scalingFactor;
    }

    private boolean isFlag(int flagMask) {
        return (flags & flagMask) == flagMask;
    }

    private void setFlag(boolean value, int mask) {
        if (value) flags |= mask;
        else flags &= ~mask;
    }

    private final ArrayList<Renderable> children = new ArrayList<>();
    private final Vector2f sizeToParent;
    private final Vector2f offsetToParent;
    private Renderable parent = DummyRenderable.dummy;
    private float scalingFactor = 1.05F;
    private int flags = VISIBILITY_MASK | ALLOW_FOCUS_SCALING_MASK | SCALES_WITH_GUI_SIZE_MASK;

    private static final int VISIBILITY_MASK = 0x1;
    private static final int FOCUSSED_MASK = 0x2;
    private static final int ALLOW_FOCUS_SCALING_MASK = 0x4;
    private static final int SCALES_WITH_GUI_SIZE_MASK = 0x8;
}
