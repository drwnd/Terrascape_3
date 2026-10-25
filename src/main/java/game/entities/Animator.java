package game.entities;

import core.utils.MainThread;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;

public interface Animator {

    Animator DEFAULT_ANIMATOR = new Animator() {
        @Override
        @MainThread
        public void applyAnimation(RenderEntity entity, Vector3f rotation, float frameTime, float gameTickFraction) {
            Animator.super.applyAnimation(entity, rotation, frameTime, gameTickFraction);
        }
    };

    int HEAD = 0;
    int BODY = 1;
    int RIGHT_ARM = 2;
    int LEFT_ARM = 3;
    int RIGHT_LEG = 4;
    int LEFT_LEG = 5;

    /**
     * Animates the given Entity.
     * This method should only be called by {@code Entity::updateFrame}
     *
     * @param entity           The Entity to animate.
     * @param frameTime        The time since the last frame in ms.
     * @param gameTickFraction The time within the current Game-Tick. In the range [0, 1].
     */
    @MainThread
    default void applyAnimation(RenderEntity entity, Vector3f rotation, float frameTime, float gameTickFraction) {
        Matrix4f[] transforms = entity.transforms();
        Model.ModelPart[] parts = entity.parts();

        float angle = (float) -Math.toRadians(rotation.y);

        for (int index = 0; index < parts.length; index++)
            transforms[index].identity()
                    .rotate(angle, 0, 1, 0)
                    .translate(parts[index].position());
    }

    static void rotateBody(Matrix4f[] transforms, Model.ModelPart[] parts, Vector3f rotation) {
        float angle = (float) -Math.toRadians(rotation.y);

        for (int index = 0; index < transforms.length; index++) {
            transforms[index].identity();
            if (parts[index].parentIndex() == null || parts[index].ignoreParentRotation()) transforms[index].rotate(angle, 0, 1, 0);
            transforms[index].translate(parts[index].position());
        }
    }


    static void applyParenting(RenderEntity entity) {
        Matrix4f[] transforms = entity.transforms();
        Matrix4f[] parentedTransforms = entity.parentedTransforms();
        Model.ModelPart[] parts = entity.parts();

        Arrays.fill(parentedTransforms, null);
        for (int index = 0; index < transforms.length; index++)
            fillParentTransforms(transforms, parentedTransforms, parts, index);
    }

    private static Matrix4f fillParentTransforms(Matrix4f[] transforms, Matrix4f[] parentedTransforms, Model.ModelPart[] parts, int index) {
        if (parentedTransforms[index] != null) return parentedTransforms[index];
        if (parts[index].parentIndex() != null) {
            Matrix4f parentTransform = fillParentTransforms(transforms, parentedTransforms, parts, parts[index].parentIndex());
            Matrix3f rotation = parts[index].ignoreParentRotation() ? transforms[index].get3x3(new Matrix3f()) : null;
            parentTransform.mul(transforms[index], transforms[index]);
            if (parts[index].ignoreParentRotation()) transforms[index].set3x3(rotation);
        }
        parentedTransforms[index] = transforms[index];
        return parentedTransforms[index];
    }
}
