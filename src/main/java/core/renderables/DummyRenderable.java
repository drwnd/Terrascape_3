package core.renderables;

import org.joml.Vector2f;

public final class DummyRenderable extends Renderable {

    static final DummyRenderable dummy = new DummyRenderable();

    public DummyRenderable() {
        super(new Vector2f(), new Vector2f());
    }

    @Override
    public Vector2f getPosition() {
        return new Vector2f();
    }

    @Override
    public Vector2f getSize() {
        return new Vector2f(1.0F, 1.0F);
    }

    @Override
    public boolean scalesWithGuiSize() {
        return true;
    }
}
