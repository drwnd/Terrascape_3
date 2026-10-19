package game.entities;

import core.utils.MainThread;
import org.joml.Matrix4f;

public interface Animator {

    /**
     * Animates the given Entity.
     * This method should only be called by {@code Entity::updateFrame}
     *
     * @param entity          The Entity to animate.
     * @param frameTime       The time since the last frame in ms.
     */
    @MainThread
    default void applyAnimation(RenderEntity entity, float frameTime) {
        Matrix4f[] transforms = entity.transforms();
        Model.ModelPart[] parts = entity.parts();

        for (int index = 0; index < parts.length; index++)
            transforms[index].identity().translate(parts[index].position());
    }
}
