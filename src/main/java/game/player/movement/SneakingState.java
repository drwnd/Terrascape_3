package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;
import game.utils.Position;
import game.utils.Utils;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;

public final class SneakingState extends MovementState {

    @Override
    Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPositon) {
        if (!Input.isKeyPressed(KeySetting.SNEAK)) next = new WalkingState();
        if (Input.isKeyPressed(KeySetting.CRAWL)) next = new CrawlingState();

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = Utils.getHorizontalDirection(playerRotation);
        float speed = movement.isGrounded() ? SNEAKING_SPEED : IN_AIR_SPEED;

        applyXZMovement(velocityChange, speed, 1.0F);

        if (Input.isKeyPressed(KeySetting.JUMP) && movement.isGrounded()) velocityChange.y = JUMP_STRENGTH;

        normalizeXZToMaxComponent(velocityChange);
        toWorldDirection(velocityChange, playerDirection);

        return velocityChange;
    }

    @Override
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        float drag = movement.isGrounded() ? WALKING_DRAG : IN_AIR_DRAG;
        velocity.add(acceleration).mul(drag);
        applyGravity(velocity);
    }

    @Override
    void handleInput(int key, int action) {
        if (key == KeySetting.JUMP.value() && action == GLFW.GLFW_PRESS) {
            if (System.nanoTime() - lastJumpTime < JUMP_FLYING_INTERVALL) next = new FlyingState();
            lastJumpTime = System.nanoTime();
        }
    }

    @Override
    public Vector3i getHitboxSize() {
        return new Vector3i(7, 24, 7);
    }

    @Override
    int getMaxAutoStepHeight() {
        return 5;
    }

    @Override
    boolean preventsFallingFromEdge() {
        return true;
    }

    @Override
    public float getCameraElevation() {
        return 22;
    }


    private long lastJumpTime = System.nanoTime() - JUMP_FLYING_INTERVALL;

    private static final float JUMP_STRENGTH = 11.125F;
    private static final float SNEAKING_SPEED = 1.25F;
    private static final float IN_AIR_SPEED = 0.15F;
}
