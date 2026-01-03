package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;

import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

public final class WalkingState extends MovementState {

    @Override
    Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition) {
        if (Input.isKeyPressed(KeySetting.SNEAK)) next = new SneakingState();
        if (Input.isKeyPressed(KeySetting.CRAWL)) next = new CrawlingState();
        if (Input.isKeyPressed(KeySetting.SPRINT) && Input.isKeyPressed(KeySetting.MOVE_FORWARD) && intersectsLiquid(lastPosition, this)) next = new SwimmingState();

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = Utils.getHorizontalDirection(playerRotation);
        float speed = getMovementSpeed(lastPosition, WALKING_SPEED, IN_AIR_SPEED, SWIM_STRENGTH);

        applyXZMovement(velocityChange, speed, SPRINT_SPEED_MODIFIER);

        if (Input.isKeyPressed(KeySetting.JUMP)) {
            handleJump(lastPosition, velocityChange, JUMP_STRENGTH, SWIM_STRENGTH);
            if (Input.isKeyPressed(KeySetting.MOVE_FORWARD) && Input.isKeyPressed(KeySetting.SPRINT) && movement.isGrounded())
                velocityChange.x += JUMP_SPEED_GAIN;
        }

        normalizeXZToMaxComponent(velocityChange);
        toWorldDirection(velocityChange, playerDirection);

        return velocityChange;
    }

    @Override
    void handleInput(int key, int action) {
        if (key == KeySetting.JUMP.value() && action == GLFW_PRESS) {
            if (System.nanoTime() - lastJumpTime < JUMP_FLYING_INTERVALL) next = new FlyingState();
            lastJumpTime = System.nanoTime();
        }
    }

    @Override
    public Vector3i getHitboxSize() {
        return new Vector3i(7, 28, 7);
    }

    @Override
    int getMaxAutoStepHeight() {
        return 5;
    }

    @Override
    boolean preventsFallingFromEdge() {
        return false;
    }

    @Override
    public float getCameraElevation() {
        return 26;
    }

    @Override
    public byte getIdentifier() {
        return 0;
    }


    private static final float JUMP_STRENGTH = 14.25F;
    private static final float SWIM_STRENGTH = 0.0025F;
    private static final float WALKING_SPEED = 2.5F;
    private static final float IN_AIR_SPEED = 0.2F;
    private static final float SPRINT_SPEED_MODIFIER = 1.5F;
    private static final float JUMP_SPEED_GAIN = 2.0F;
}
