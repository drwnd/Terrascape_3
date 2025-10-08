package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;

import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.glfw.GLFW;

public final class FlyingState extends MovementState {
    @Override
    protected Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPositon) {

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = Utils.getHorizontalDirection(playerRotation);

        if (Input.isKeyPressed(KeySetting.MOVE_FORWARD))
            velocityChange.add(playerDirection.x * HORIZONTAL_FLY_SPEED, 0, playerDirection.z * HORIZONTAL_FLY_SPEED);
        if (Input.isKeyPressed(KeySetting.MOVE_BACK))
            velocityChange.sub(playerDirection.x * HORIZONTAL_FLY_SPEED, 0, playerDirection.z * HORIZONTAL_FLY_SPEED);

        if (Input.isKeyPressed(KeySetting.SPRINT)) velocityChange.mul(SPRINT_SPEED_MODIFIER);

        if (Input.isKeyPressed(KeySetting.MOVE_RIGHT))
            velocityChange.sub(playerDirection.z * HORIZONTAL_FLY_SPEED, 0, -playerDirection.x * HORIZONTAL_FLY_SPEED);
        if (Input.isKeyPressed(KeySetting.MOVE_LEFT))
            velocityChange.add(playerDirection.z * HORIZONTAL_FLY_SPEED, 0, -playerDirection.x * HORIZONTAL_FLY_SPEED);

        if (Input.isKeyPressed(KeySetting.SNEAK)) velocityChange.mul(SNEAK_SPEED_MODIFIER);

        if (Input.isKeyPressed(KeySetting.JUMP)) velocityChange.y += VERTICAL_FLY_SPEED;
        if (Input.isKeyPressed(KeySetting.SNEAK)) velocityChange.y -= VERTICAL_FLY_SPEED;

        if (Input.isKeyPressed(KeySetting.FLY_FAST)) velocityChange.mul(FLY_FAST_SPEED_MODIFIER);
        Utils.normalizeToMaxComponent(velocityChange);

        return velocityChange;
    }

    @Override
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        velocity.add(acceleration).mul(FLY_DRAG);
    }

    @Override
    protected void handleInput(int key, int action) {
        if (key == KeySetting.TOGGLE_FLYING_FOLLOWING_MOVEMENT_STATE.value() && action == GLFW.GLFW_PRESS)
            next = new FollowDirectionState();
    }

    @Override
    public Vector3i getHitboxSize() {
        return new Vector3i(7, 28, 7);
    }
    @Override
    public float getCameraElevation() {
        return 26;
    }

    private static final float VERTICAL_FLY_SPEED = 0.5f;
    private static final float HORIZONTAL_FLY_SPEED = 1.0f;
    private static final float SNEAK_SPEED_MODIFIER = 0.5f;
    private static final float SPRINT_SPEED_MODIFIER = 2.0f;
    private static final float FLY_FAST_SPEED_MODIFIER = 5.0f;
    private static final float FLY_DRAG = 0.94f;
}
