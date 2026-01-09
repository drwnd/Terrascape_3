package game.player.movement;

import core.rendering_api.Input;
import core.settings.KeySetting;

import core.settings.ToggleSetting;
import game.server.Game;
import game.server.World;
import game.utils.Utils;
import game.utils.Position;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public abstract class MovementState {

    MovementState next = this;

    /**
     * Computes the acceleration the Player should have in the next Gametick.
     * Only gets called when the Player is actively moving.
     *
     * @param playerRotation The rotation of the player (not necessarily the camera)
     * @param lastPosition   The position of the player in the last Gametick
     * @return The acceleration of the Player.
     */
    abstract Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition);

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
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        float waterIntersection = intersectedVolume(playerPosition, this, WATER);
        float lavaIntersection = intersectedVolume(playerPosition, this, LAVA);

        float drag = movement.isGrounded() ? WALKING_DRAG : AIR_DRAG;
        float liquidDrag = (float) (Math.pow(WATER_DRAG, waterIntersection)) * (float) (Math.pow(LAVA_DRAG, lavaIntersection));

        velocity.add(acceleration).mul(drag).mul(liquidDrag);
        applyGravity(velocity);
        velocity.y += waterIntersection * WATER_BUOYANCY + lavaIntersection * LAVA_BUOYANCY;
    }

    abstract void handleInput(int key, int action);

    abstract int getMaxAutoStepHeight();

    abstract boolean preventsFallingFromEdge();

    public abstract Vector3i getHitboxSize();

    public abstract float getCameraElevation();

    public abstract byte getIdentifier();


    public static MovementState getStateFromIdentifier(byte identifier) {
        return switch (identifier) {
            case 0 -> new WalkingState();
            case 1 -> new SwimmingState();
            case 2 -> new SneakingState();
            case 3 -> new FlyingState();
            case 4 -> new CrawlingState();
            default -> null;
        };
    }


    void handleJump(Position position, Vector3f velocityChange, float jumpStrength, float swimStrength) {
        if (movement.isGrounded()) velocityChange.y = jumpStrength;
        else velocityChange.y += intersectedVolume(position, this, WATER) * swimStrength + intersectedVolume(position, this, LAVA) * swimStrength;
    }

    float getMovementSpeed(Position lastPosition, float movementSpeed, float inAirSpeed, float swimStrength) {
        float speed = movement.isGrounded() ? movementSpeed : inAirSpeed;
        speed += intersectedVolume(lastPosition, this, WATER) * swimStrength * movementSpeed * 0.25F;
        speed += intersectedVolume(lastPosition, this, LAVA) * swimStrength * movementSpeed * 0.25F;
        return speed;
    }


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

    static float intersectedVolume(Position position, MovementState state, byte targetMaterial) {
        if (ToggleSetting.NO_CLIP.value()) return 0;

        World world = Game.getWorld();
        Vector3i hitboxSize = state.getHitboxSize();

        int startX = position.intX + Utils.floor(position.fractionX - hitboxSize.x * 0.5F);
        int startY = position.intY;
        int startZ = position.intZ + Utils.floor(position.fractionZ - hitboxSize.z * 0.5F);

        int width = hitboxSize.x + 1;
        int height = hitboxSize.y;
        int depth = hitboxSize.z + 1;

        float volumne = 0.0F;
        for (int x = startX; x < startX + width; x++)
            for (int y = startY; y < startY + height; y++)
                for (int z = startZ; z < startZ + depth; z++) {
                    if (targetMaterial != world.getMaterial(x, y, z, 0)) continue;
                    volumne++;
                }
        return volumne;
    }

    static boolean intersectsLiquid(Position position, MovementState state) {
        return intersectedVolume(position, state, WATER) != 0.0F || intersectedVolume(position, state, LAVA) != 0.0F;
    }

    protected Movement movement;
    protected long lastJumpTime = System.nanoTime() - JUMP_FLYING_INTERVALL;

    private static final float GRAVITY_ACCELERATION = 1.28F;
    private static final float WATER_BUOYANCY = 0.0005F;
    private static final float LAVA_BUOYANCY = 0.0006F;
    static final float WATER_DRAG = 0.99935F;
    static final float LAVA_DRAG = 0.999F;
    static final long JUMP_FLYING_INTERVALL = 300_000_000; // 0.3s
    static final float WALKING_DRAG = 0.6F;
    static final float AIR_DRAG = 0.94F;
}
