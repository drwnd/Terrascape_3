package game.entities;

import core.utils.MainThread;

import game.assets.EntityType;
import game.entities.movement.Movement;
import game.entities.movement.MovementState;
import game.entities.movement.WalkingState;
import game.server.Game;
import game.utils.Position;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Zombie extends Entity {

    public Zombie(Position position) {
        super(position, new RenderEntity(EntityType.ZOMBIE), MovementState.load(WanderingMovementState.class));
        getRenderEntity().setAnimator(new ZombieAnimator(this));
    }

    private record ZombieAnimator(Zombie zombie) implements Animator {

        @Override
        @MainThread
        public void applyAnimation(RenderEntity entity, Vector3f rotation, float frameTime) {
            Matrix4f[] transforms = entity.transforms();
            Model.ModelPart[] parts = entity.parts();
            float fraction = Math.clamp(Game.getServer().getCurrentGameTickFraction(), 0, 1);
            Movement movement = zombie.getMovement();
            Vector3f velocity = movement.getRenderVelocity().mul(fraction).add(movement.getOldRenderVelocity().mul(1 - fraction));

            WalkingState.rotateBody(transforms, parts, rotation, velocity);

            float speed = (float) Math.clamp(velocity.length() * 0.2, -Math.PI * 0.5, Math.PI * 0.5);
            double animationTimer = entity.animationTimer;

            transforms[HEAD].translate(0, -4, -3.5F)
                    .rotate((float) -Math.toRadians(rotation.x), 1, 0, 0)
                    .rotate((float) Math.sin(animationTimer * 0.5) * 0.1F, 0, 0, 1);
            transforms[BODY].translate(0, -2, 2.5F)
                    .rotate(-0.5F, 1, 0, 0);
            transforms[LEFT_ARM].translate(0, -4, -3.5F)
                    .rotate((float) Math.sin(animationTimer) * (speed + 0.1F) + (float) Math.toRadians(90), 1, 0, 0)
                    .rotate(-speed * 0.2F, 0, 0, 1);
            transforms[RIGHT_ARM].translate(0, -4, -3.5F)
                    .rotate((float) -Math.sin(animationTimer) * (speed + 0.1F) + (float) Math.toRadians(90), 1, 0, 0)
                    .rotate(speed * 0.2F, 0, 0, 1);
            transforms[LEFT_LEG].translate(0, 0, 2)
                    .rotate((float) -Math.sin(animationTimer) * speed, 1, 0, 0)
                    .rotate(-0.1F, 0, 0, 1);
            transforms[RIGHT_LEG].translate(0, 0, 2)
                    .rotate((float) Math.sin(animationTimer) * speed, 1, 0, 0)
                    .rotate(0.1F, 0, 0, 1);

            entity.animationTimer += frameTime * 0.005;
        }
    }
}
