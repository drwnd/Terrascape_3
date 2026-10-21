package game.entities;

import core.assets.AssetManager;
import core.assets.GuiElement;
import core.assets.identifiers.AssetIdentifier;
import core.utils.MainThread;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class RenderEntity {

    public double animationTimer = 0;

    public RenderEntity(AssetIdentifier<Model> modelIdentifier, Animator animator) {
        this.modelIdentifier = modelIdentifier;
        this.animator = animator;
    }

    public RenderEntity(AssetIdentifier<Model> modelIdentifier) {
        this.modelIdentifier = modelIdentifier;
    }

    @MainThread
    public void updateFrame(float frameTime, Vector3f rotation) {
        synchronizeTransforms();
        animator.applyAnimation(this, rotation, frameTime);
    }

    public void synchronizeTransforms() {
        model = AssetManager.get(modelIdentifier);
        if (transforms.length != model.parts().length) {
            transforms = new Matrix4f[model.parts().length];
            for (int index = 0; index < transforms.length; index++) transforms[index] = new Matrix4f().zero();
        }
    }

    public void setAnimator(Animator animator) {
        this.animator = animator;
    }

    public Matrix4f[] transforms() {
        return transforms;
    }

    public Model.ModelPart[] parts() {
        return model.parts();
    }

    public GuiElement guiElement() {
        return model.guiElement();
    }

    private final AssetIdentifier<Model> modelIdentifier;
    private Animator animator;
    private Model model;
    private Matrix4f[] transforms = new Matrix4f[0];
}
