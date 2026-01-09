package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;
import core.settings.ToggleSetting;

import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.lwjgl.glfw.GLFW.*;

public final class FlyingState extends MovementState {

    @Override
    protected Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition) {

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = Utils.getHorizontalDirection(playerRotation);

        WalkingState.applyXZMovement(velocityChange, HORIZONTAL_FLY_SPEED, SPRINT_SPEED_MODIFIER);

        if (Input.isKeyPressed(KeySetting.SNEAK)) velocityChange.mul(SNEAK_SPEED_MODIFIER);

        if (Input.isKeyPressed(KeySetting.JUMP)) velocityChange.y += VERTICAL_FLY_SPEED;
        if (Input.isKeyPressed(KeySetting.SNEAK)) velocityChange.y -= VERTICAL_FLY_SPEED;

        if (Input.isKeyPressed(KeySetting.FLY_FAST)) velocityChange.mul(FLY_FAST_SPEED_MODIFIER);
        normalizeToMaxComponent(velocityChange);
        toWorldDirection(velocityChange, playerDirection);

        return velocityChange;
    }

    @Override
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        velocity.add(acceleration).mul(AIR_DRAG);
        if (movement.isGrounded() && !ToggleSetting.NO_CLIP.value()) next = new WalkingState();
    }

    @Override
    protected void handleInput(int key, int action) {
        if (key == KeySetting.JUMP.keybind() && action == GLFW_PRESS) {
            if (System.nanoTime() - lastJumpTime < JUMP_FLYING_INTERVALL) next = new WalkingState();
            lastJumpTime = System.nanoTime();
        }
    }

    @Override
    public Vector3i getHitboxSize() {
        return new Vector3i(7, 28, 7);
    }

    @Override
    int getMaxAutoStepHeight() {
        return 0;
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
        return 3;
    }


    private static final float VERTICAL_FLY_SPEED = 0.5F;
    private static final float HORIZONTAL_FLY_SPEED = 1.0F;
    private static final float SNEAK_SPEED_MODIFIER = 0.5F;
    private static final float SPRINT_SPEED_MODIFIER = 2.0F;
    private static final float FLY_FAST_SPEED_MODIFIER = 5.0F;
}
