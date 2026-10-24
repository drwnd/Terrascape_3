package game.entities;

import core.assets.AssetManager;
import core.assets.GuiElement;
import core.assets.Texture2D;
import core.assets.identifiers.AssetIdentifier;
import core.utils.MainThread;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderEntity {

    public double animationTimer = 0;

    public RenderEntity(AssetIdentifier<Model> modelIdentifier, AssetIdentifier<Texture2D> textureIdentifier, Animator animator) {
        this.modelIdentifier = modelIdentifier;
        this.animator = animator;
        this.textureIdentifier = textureIdentifier;
    }

    public RenderEntity(AssetIdentifier<Model> modelIdentifier, Animator animator) {
        this.modelIdentifier = modelIdentifier;
        this.animator = animator;
        this.textureIdentifier = NULL_TEXTURE_IDENTIFIER;
    }

    public RenderEntity(AssetIdentifier<Model> modelIdentifier) {
        this.modelIdentifier = modelIdentifier;
        animator = Animator.DEFAULT_ANIMATOR;
        this.textureIdentifier = NULL_TEXTURE_IDENTIFIER;
    }

    @MainThread
    public void updateFrame(float frameTime, float gameTickFraction, Vector3f rotation) {
        synchronizeTransforms();
        animator.applyAnimation(this, rotation, frameTime, gameTickFraction);
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

    public AssetIdentifier<Texture2D> getTextureIdentifier() {
        return textureIdentifier;
    }

    private final AssetIdentifier<Model> modelIdentifier;
    private final AssetIdentifier<Texture2D> textureIdentifier;
    private Animator animator;
    private Model model;
    private Matrix4f[] transforms = new Matrix4f[0];

    private static final AssetIdentifier<Texture2D> NULL_TEXTURE_IDENTIFIER = () -> new Texture2D(0);
}
