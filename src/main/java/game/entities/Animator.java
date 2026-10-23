package game.entities;

import core.utils.MainThread;

import org.joml.Matrix4f;
import org.joml.Vector3f;

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
}
