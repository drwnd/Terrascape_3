package game.entities;

import core.utils.MainThread;

import game.assets.Models;
import game.entities.movement.EntityMovementState;
import game.entities.movement.Movement;
import game.entities.movement.MovementState;
import game.entities.movement.WalkingState;
import game.server.Game;
import game.utils.Position;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Zombie extends Entity {

    public Zombie(Position position) {
        super(position, new RenderEntity(Models.ZOMBIE), MovementState.load(ZombieMovementState.class));
        getRenderEntity().setAnimator(new ZombieAnimator(this));
    }

    public static class ZombieAnimator implements Animator {

        public static final int HEAD = 0;
        public static final int BODY = 1;
        public static final int RIGHT_ARM = 2;
        public static final int LEFT_ARM = 3;
        public static final int RIGHT_LEG = 4;
        public static final int LEFT_LEG = 5;

        public ZombieAnimator(Zombie zombie) {
            this.zombie = zombie;
        }

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

        private final Zombie zombie;
    }

    public static class ZombieMovementState extends EntityMovementState {

        @Override
        protected Vector3f computeNextGameTickAcceleration(Vector3f rotation, Position lastPosition) {
            long currentTick = Game.getServer().getCurrentGameTick();

            if (currentTick - lastActionTick > timeBetweenActions) {
                lastActionTick = currentTick;
                rotation.y = (float) Math.random() * 360;
                float distance = (float) Math.sqrt(Math.random()) * searchRadius;
                targetX = lastPosition.longX + (long) (Math.sin(Math.toRadians(rotation.y)) * distance);
                targetZ = lastPosition.longZ - (long) (Math.cos(Math.toRadians(rotation.y)) * distance);
            }

            Vector3f distance = new Vector3f(targetX - lastPosition.longX, 0, targetZ - lastPosition.longZ);
            if (distance.lengthSquared() < 100) return new Vector3f();
            return distance.normalize(walkingSpeed);
        }

        private long targetX, targetZ;

        @SuppressWarnings("unused")
        private int searchRadius, timeBetweenActions;
    }
}
