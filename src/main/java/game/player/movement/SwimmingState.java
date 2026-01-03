package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;

import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;
import static org.lwjgl.glfw.GLFW.*;

public final class SwimmingState extends MovementState {

    @Override
    Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition) {
        if (!Input.isKeyPressed(KeySetting.SPRINT) || !Input.isKeyPressed(KeySetting.MOVE_FORWARD)) next = new CrawlingState();

        Vector3f velocityChange = new Vector3f();
        Vector3f playerDirection = Utils.getDirection(playerRotation);

        float liquidVolume = intersectedVolume(lastPosition, this, WATER) + intersectedVolume(lastPosition, this, LAVA);
        float airVolume = Math.min(intersectedVolume(lastPosition, this, AIR), 50.0F);

        velocityChange.set(playerDirection).mul(liquidVolume * SWIM_SPEED * 0.5F + airVolume * SWIM_SPEED);

        return velocityChange;
    }

    @Override
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        if (!intersectsLiquid(playerPosition, this)) next = new CrawlingState();

        float waterIntersection = intersectedVolume(playerPosition, this, WATER);
        float lavaIntersection = intersectedVolume(playerPosition, this, LAVA);

        float liquidDrag = (float) (Math.pow(WATER_DRAG, waterIntersection)) * (float) (Math.pow(LAVA_DRAG, lavaIntersection));
        velocity.add(acceleration).mul(liquidDrag * AIR_DRAG);

        if (velocity.y > 0.0F) {
            Position nextPosition = new Position(playerPosition);
            nextPosition.addComponent(Y_COMPONENT, velocity.y);
            if (!intersectsLiquid(nextPosition, this)) velocity.y = 0.0F;
        }
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
        return 1;
    }

    @Override
    boolean preventsFallingFromEdge() {
        return false;
    }

    @Override
    public float getCameraElevation() {
        return 5;
    }

    @Override
    public byte getIdentifier() {
        return 1;
    }

    private static final float SWIM_SPEED = 0.0045F;
}
