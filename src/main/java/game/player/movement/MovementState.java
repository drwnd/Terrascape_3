package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;
import org.joml.Vector3f;
import game.utils.Position;
import org.joml.Vector3i;

public abstract class MovementState {

    MovementState next = this;

    /**
     * Computes the acceleration the Player should have in the next Gametick.
     * Only gets called when the Player is actively moving.
     *
     * @param playerRotation The rotation of the player (not necessarily the camera)
     * @param lastPositon    The position of the player in the last Gametick
     * @return The acceleration of the Player.
     */
    abstract Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPositon);

    /**
     * Changes the current Velocity the Player has.
     * Gets called every Gametick regardless of the Player being able to move actively.
     * Therefore, no inputs should be queried in this method.
     *
     * @param velocity       The current Velocity of the Player. This is also the output variable.
     * @param acceleration   The acceleration computed in computeNextGameTickAcceleration(...) or 0 if the Player can't move actively.
     * @param playerPosition The position of the Player.
     * @param playerRotation The rotation of the Player.
     */
    abstract void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation);

    abstract void handleInput(int key, int action);

    public abstract Vector3i getHitboxSize();

    public abstract float getCameraElevation();

    static void normalizeToMaxComponent(Vector3f velocity) {
        float max = Math.abs(velocity.get(velocity.maxComponent()));
        if (max < 1E-4F) return;
        velocity.normalize(max);
    }

    static void normalizeXZToMaxComponent(Vector3f velocity) {
        float max = Math.max(Math.abs(velocity.x), Math.abs(velocity.z));
        if (max < 1E-4F) return;
        float normalizer = (float) (max / Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z));
        velocity.x *= normalizer;
        velocity.z *= normalizer;
    }

    static void toWorldDirection(Vector3f relativeVelocity, Vector3f direction) {
        relativeVelocity.set(
          relativeVelocity.x * direction.x + relativeVelocity.z * direction.z,
          relativeVelocity.y,
          relativeVelocity.x * direction.z - relativeVelocity.z * direction.x
        );
    }

    static void applyXZMovement(Vector3f velocityChange, float speed, float sprintSpeedModifier) {
        if (Input.isKeyPressed(KeySetting.MOVE_FORWARD))
            velocityChange.x += speed;
        if (Input.isKeyPressed(KeySetting.SPRINT)) velocityChange.mul(sprintSpeedModifier);

        if (Input.isKeyPressed(KeySetting.MOVE_BACK))
            velocityChange.x -= speed;

        if (Input.isKeyPressed(KeySetting.MOVE_RIGHT))
            velocityChange.z -= speed;
        if (Input.isKeyPressed(KeySetting.MOVE_LEFT))
            velocityChange.z += speed;
    }

    static void applyGravity(Vector3f velocity) {
        velocity.y -= GRAVITY_ACCELERATION;
    }

    protected Movement movement;

    private static final float GRAVITY_ACCELERATION = 1.28F;
    static final long JUMP_FLYING_INTERVALL = 300_000_000; // 0.3s
    static final float WALKING_DRAG = 0.6F;
    static final float IN_AIR_DRAG = 0.94F;
}
