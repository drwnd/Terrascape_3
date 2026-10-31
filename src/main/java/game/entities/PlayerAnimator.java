package game.entities;

import core.utils.MainThread;

import core.utils.MathUtils;
import game.entities.model.ModelPart;
import game.entities.movement.Movement;
import game.player.Player;
import game.player.rendering.Camera;
import game.server.Game;
import game.settings.OptionSettings;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class PlayerAnimator implements Animator {

    @Override
    @MainThread
    public void applyAnimation(RenderEntity entity, Vector3f rotation, float frameTime, float gameTickFraction) {
        Player player = Game.getPlayer();
        Movement movement = player.getMovement();
        Vector3f velocity = movement.getRenderVelocity().mul(gameTickFraction).add(movement.getOldRenderVelocity().mul(1 - gameTickFraction));

        rotateBody(entity.transforms(), entity.parts(), player.getCamera().getRotation(), velocity);
        player.getMovement().getState().applyAnimation(entity, player.getCamera(), frameTime, gameTickFraction, velocity);
        Animator.applyParenting(entity);
    }

    @MainThread
    private static void rotateBody(Matrix4f[] transforms, ModelPart[] parts, Vector3f cameraRotation, Vector3f velocity) {
        Vector3f direction = MathUtils.getHorizontalDirection(cameraRotation);
        float angle = (float) -Math.toRadians(cameraRotation.y);
        float sidewaysVelocity = -velocity.x * direction.z + velocity.z * direction.x;
        float sidewaysTilt = (float) Math.clamp(sidewaysVelocity * 0.2F, -Math.PI * 0.25, Math.PI * 0.25);
        boolean isFirstPerson = OptionSettings.PERSPECTIVE.value() == Camera.Perspective.FIRST_PERSON;

        for (int index = 0; index < transforms.length; index++) {
            transforms[index].identity();
            if (parts[index].parentIndex() == null || parts[index].ignoreParentRotation()) transforms[index].rotate(angle - sidewaysTilt, 0, 1, 0);
            transforms[index]
                    .translate(parts[index].position())
                    .translate(0, 0, isFirstPerson && parts[index].parentIndex() == null ? 3 : 0);
        }

        transforms[HEAD].rotate(sidewaysTilt, 0, 1, 0);
    }
}
