package game.entities;

import core.utils.MainThread;

import game.assets.EntityType;
import game.entities.model.ModelPart;
import game.entities.movement.Movement;
import game.entities.movement.MovementState;
import game.utils.Position;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Skeleton extends Entity {
    public Skeleton(Position position) {
        super(position, new RenderEntity(EntityType.SKELETON), MovementState.load(WanderingMovementState.class));
        getRenderEntity().setAnimator(new SkeletonAnimator(this));
    }

    private record SkeletonAnimator(Skeleton skeleton, float[] wobbleOffsets, float[] wobbleStrengths, float[] wobbleSpeeds) implements Animator {

        private SkeletonAnimator(Skeleton skeleton) {
            this(skeleton, getWobbleOffsets(), getWobbleStrengths(), getWobbleSpeeds());
        }

        @Override
        @MainThread
        public void applyAnimation(RenderEntity entity, Vector3f rotation, float frameTime, float gameTickFraction) {
            Matrix4f[] transforms = entity.transforms();
            ModelPart[] parts = entity.parts();
            Movement movement = skeleton.getMovement();
            Vector3f velocity = movement.getRenderVelocity().mul(gameTickFraction).add(movement.getOldRenderVelocity().mul(1 - gameTickFraction));

            Animator.rotateBody(transforms, parts, rotation);

            float speed = (float) Math.clamp(velocity.length() * 0.2, -Math.PI * 0.5, Math.PI * 0.5);
            double animationTimer = entity.animationTimer;

            transforms[HEAD].rotate((float) -Math.toRadians(rotation.x), 1, 0, 0);
            transforms[LEFT_ARM].rotate((float) Math.sin(animationTimer) * speed * 3, 1, 0, 0).rotate(-speed * 0.1F, 0, 0, 1);
            transforms[RIGHT_ARM].rotate((float) -Math.sin(animationTimer) * speed * 3, 1, 0, 0).rotate(speed * 0.1F, 0, 0, 1);
            transforms[LEFT_LEG].rotate((float) -Math.sin(animationTimer) * speed * 3, 1, 0, 0);
            transforms[RIGHT_LEG].rotate((float) Math.sin(animationTimer) * speed * 3, 1, 0, 0);

            boolean isIdle = speed < 0.05F;
            float wobbleStrength = isIdle ? 0.2F : speed * 3;
            double wobbleTime = animationTimer * WOBBLE_SPEED * (isIdle ? 0.25 : 1);
            for (int index = 0; index < 6; index++) {
                transforms[index]
                        .translate(
                                (float) Math.sin(wobbleTime * wobbleSpeeds[index * WOBBLE_COUNT + WOBBLE_X] + wobbleOffsets[index * WOBBLE_COUNT + WOBBLE_X]) * wobbleStrengths[index * WOBBLE_COUNT + WOBBLE_X] * wobbleStrength,
                                (float) Math.sin(wobbleTime * wobbleSpeeds[index * WOBBLE_COUNT + WOBBLE_Y] + wobbleOffsets[index * WOBBLE_COUNT + WOBBLE_Y]) * wobbleStrengths[index * WOBBLE_COUNT + WOBBLE_Y] * wobbleStrength,
                                (float) Math.sin(wobbleTime * wobbleSpeeds[index * WOBBLE_COUNT + WOBBLE_Z] + wobbleOffsets[index * WOBBLE_COUNT + WOBBLE_Z]) * wobbleStrengths[index * WOBBLE_COUNT + WOBBLE_Z] * wobbleStrength)
                        .rotate((float) Math.sin(wobbleTime * wobbleSpeeds[index * WOBBLE_COUNT + WOBBLE_YAW] + wobbleOffsets[index * WOBBLE_COUNT + WOBBLE_YAW]) * wobbleStrengths[index * WOBBLE_COUNT + WOBBLE_YAW] * wobbleStrength, 1, 0, 0)
                        .rotate((float) Math.sin(wobbleTime * wobbleSpeeds[index * WOBBLE_COUNT + WOBBLE_ROLL] + wobbleOffsets[index * WOBBLE_COUNT + WOBBLE_ROLL]) * wobbleStrengths[index * WOBBLE_COUNT + WOBBLE_ROLL] * wobbleStrength, 0, 0, 1)
                        .rotate((float) Math.sin(wobbleTime * wobbleSpeeds[index * WOBBLE_COUNT + WOBBLE_PITCH] + wobbleOffsets[index * WOBBLE_COUNT + WOBBLE_PITCH]) * wobbleStrengths[index * WOBBLE_COUNT + WOBBLE_PITCH] * wobbleStrength, 0, 1, 0);
            }

            Animator.applyParenting(entity);
            entity.animationTimer += frameTime * 0.005;
        }

        private static float[] getWobbleOffsets() {
            float[] offsets = new float[6 * WOBBLE_COUNT];
            for (int index = 0; index < offsets.length; index += WOBBLE_COUNT) {
                offsets[index + WOBBLE_YAW] = (float) (Math.random() * 2 * Math.PI);
                offsets[index + WOBBLE_PITCH] = (float) (Math.random() * 2 * Math.PI);
                offsets[index + WOBBLE_ROLL] = (float) (Math.random() * 2 * Math.PI);

                offsets[index + WOBBLE_X] = (float) (Math.random() * 2 * Math.PI);
                offsets[index + WOBBLE_Y] = (float) (Math.random() * 2 * Math.PI);
                offsets[index + WOBBLE_Z] = (float) (Math.random() * 2 * Math.PI);
            }
            return offsets;
        }

        private static float[] getWobbleStrengths() {
            float[] strengths = new float[6 * WOBBLE_COUNT];
            for (int index = 0; index < strengths.length; index += WOBBLE_COUNT) {
                strengths[index + WOBBLE_YAW] = (float) (1 + Math.random() * WOBBLE_STRENGTH_VARIANCE - WOBBLE_STRENGTH_VARIANCE * 0.5F) * ROTATION_WOBBLE_STRENGTH;
                strengths[index + WOBBLE_PITCH] = (float) (1 + Math.random() * WOBBLE_STRENGTH_VARIANCE - WOBBLE_STRENGTH_VARIANCE * 0.5F) * ROTATION_WOBBLE_STRENGTH;
                strengths[index + WOBBLE_ROLL] = (float) (1 + Math.random() * WOBBLE_STRENGTH_VARIANCE - WOBBLE_STRENGTH_VARIANCE * 0.5F) * ROTATION_WOBBLE_STRENGTH;

                strengths[index + WOBBLE_X] = (float) (1 + Math.random() * WOBBLE_STRENGTH_VARIANCE - WOBBLE_STRENGTH_VARIANCE * 0.5F) * TRANSLATION_WOBBLE_STRENGTH;
                strengths[index + WOBBLE_Y] = (float) (1 + Math.random() * WOBBLE_STRENGTH_VARIANCE - WOBBLE_STRENGTH_VARIANCE * 0.5F) * TRANSLATION_WOBBLE_STRENGTH;
                strengths[index + WOBBLE_Z] = (float) (1 + Math.random() * WOBBLE_STRENGTH_VARIANCE - WOBBLE_STRENGTH_VARIANCE * 0.5F) * TRANSLATION_WOBBLE_STRENGTH;
            }
            return strengths;
        }

        private static float[] getWobbleSpeeds() {
            float[] speeds = new float[6 * WOBBLE_COUNT];
            for (int index = 0; index < speeds.length; index += WOBBLE_COUNT) {
                speeds[index + WOBBLE_YAW] = (float) (1 + Math.random() * WOBBLE_SPEED_VARIANCE - WOBBLE_SPEED_VARIANCE * 0.5F);
                speeds[index + WOBBLE_PITCH] = (float) (1 + Math.random() * WOBBLE_SPEED_VARIANCE - WOBBLE_SPEED_VARIANCE * 0.5F);
                speeds[index + WOBBLE_ROLL] = (float) (1 + Math.random() * WOBBLE_SPEED_VARIANCE - WOBBLE_SPEED_VARIANCE * 0.5F);

                speeds[index + WOBBLE_X] = (float) (1 + Math.random() * WOBBLE_SPEED_VARIANCE - WOBBLE_SPEED_VARIANCE * 0.5F);
                speeds[index + WOBBLE_Y] = (float) (1 + Math.random() * WOBBLE_SPEED_VARIANCE - WOBBLE_SPEED_VARIANCE * 0.5F);
                speeds[index + WOBBLE_Z] = (float) (1 + Math.random() * WOBBLE_SPEED_VARIANCE - WOBBLE_SPEED_VARIANCE * 0.5F);
            }
            return speeds;
        }


        private static final int WOBBLE_YAW = 0;
        private static final int WOBBLE_PITCH = 1;
        private static final int WOBBLE_ROLL = 2;
        private static final int WOBBLE_X = 3;
        private static final int WOBBLE_Y = 4;
        private static final int WOBBLE_Z = 5;
        private static final int WOBBLE_COUNT = 6;

        private static final float ROTATION_WOBBLE_STRENGTH = 0.05F;
        private static final float TRANSLATION_WOBBLE_STRENGTH = 0.5F;
        private static final float WOBBLE_STRENGTH_VARIANCE = 0.1F;
        private static final float WOBBLE_SPEED_VARIANCE = 0.5F;
        private static final float WOBBLE_SPEED = 3;
    }
}
