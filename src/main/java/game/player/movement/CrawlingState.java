package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;

import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

public final class CrawlingState extends MovementState {

    @Override
    Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition) {
        if (!Input.isKeyPressed(KeySetting.CRAWL)) next = new SneakingState();
        if (Input.isKeyPressed(KeySetting.SPRINT) && Input.isKeyPressed(KeySetting.MOVE_FORWARD) && intersectsLiquid(lastPosition, this)) next = new SwimmingState();

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = Utils.getHorizontalDirection(playerRotation);
        float speed = getMovementSpeed(lastPosition, CRAWLING_SPEED, IN_AIR_SPEED, SWIM_STRENGTH);

        applyXZMovement(velocityChange, speed, 1.0F);

        if (Input.isKeyPressed(KeySetting.JUMP)) handleJump(lastPosition, velocityChange, JUMP_STRENGTH, SWIM_STRENGTH);

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
        return new Vector3i(7, 7, 7);
    }

    @Override
    int getMaxAutoStepHeight() {
        return 3;
    }

    @Override
    boolean preventsFallingFromEdge() {
        return true;
    }

    @Override
    public float getCameraElevation() {
        return 5;
    }

    @Override
    public byte getIdentifier() {
        return 4;
    }


    private static final float JUMP_STRENGTH = 10.0F;
    private static final float SWIM_STRENGTH = 0.0033F;
    private static final float CRAWLING_SPEED = 0.75F;
    private static final float IN_AIR_SPEED = 0.075F;
}
