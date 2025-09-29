package game.player.movement;

import core.settings.KeySetting;

import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public final class FollowDirectionState extends MovementState {
    @Override
    protected Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPositon) {
        return new Vector3f();
    }

    @Override
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        velocity.set(Utils.getDirection(playerRotation));
    }


    @Override
    protected void handleInput(int key, int action) {
        if (key == KeySetting.TOGGLE_FLYING_FOLLOWING_MOVEMENT_STATE.value() && action == GLFW.GLFW_PRESS) next = new FlyingState();
    }
}
