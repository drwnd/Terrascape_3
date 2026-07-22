package game.player.movement;

import core.rendering_api.Input;
import core.utils.MathUtils;

import game.server.Game;
import game.server.World;
import game.server.material.Properties;
import game.settings.KeySettings;
import game.settings.ToggleSettings;
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
     * @param lastPosition   The position of the player in the last Gametick in absolute world block coordinates (LOD 0)
     * @return The acceleration of the Player in blocks per gametick^2 (LOD 0).
     */
    abstract Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition);

    /**
     * Changes the current velocity of the player.
     * Gets called every gametick regardless of the player being able to move actively.
     * Therefore, no inputs should be queried in this method.
     *
     * @param velocity       The current velocity of the player. This is also the output variable. Measured in blocks per gametick (LOD 0).
     * @param acceleration   The acceleration computed in computeNextGameTickAcceleration(...) or 0 if the player can't move actively. Measured in blocks per gametick (LOD 0).
     * @param playerPosition The position of the player in absolute world block coordinates (LOD 0).
     * @param playerRotation The rotation of the player.
     */
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        float waterIntersection = intersectedVolume(playerPosition, this, WATER);
        float lavaIntersection = intersectedVolume(playerPosition, this, LAVA);

        float drag = movement.isWideGrounded() ? WALKING_DRAG : AIR_DRAG;
        float liquidDrag = (float) (Math.pow(WATER_DRAG, waterIntersection)) * (float) (Math.pow(LAVA_DRAG, lavaIntersection));

        velocity.add(acceleration).mul(drag).mul(liquidDrag);
        applyGravity(velocity);
        velocity.y += waterIntersection * WATER_BUOYANCY + lavaIntersection * LAVA_BUOYANCY;
    }

    /**
     * Returns the material the player is currently standing on.
     *
     * @param position The position of the player in absolute world block coordinates (LOD 0).
     * @return The material identifier of the block the player is standing on.
     */
    byte getStandingMaterial(Position position) {
        Vector3i hitboxSize = getHitboxSize();
        World world = Game.getWorld();

        byte centerMaterial = world.getMaterial(position.longX, position.longY, position.longZ, 0);
        if (Properties.doesntHaveProperties(centerMaterial, NO_COLLISION)) return centerMaterial;

        long minX = position.longX + MathUtils.floor(position.fractionX - hitboxSize.x * 0.5F);
        long minZ = position.longZ + MathUtils.floor(position.fractionZ - hitboxSize.z * 0.5F);
        long y = position.longY - 1;
        int width = hitboxSize.x + 1;
        int depth = hitboxSize.z + 1;

        for (long x = minX; x != minX + width; x++)
            for (long z = minZ; z != minZ + depth; z++) {
                byte material = world.getMaterial(x, y, z, 0);
                if (Properties.doesntHaveProperties(material, NO_COLLISION)) return material;
            }

        return centerMaterial;
    }

    abstract void handleInput(int key, int action);

    abstract int getMaxAutoStepHeight();

    abstract int ticksBetweenFootsteps();

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


    /**
     * Handles jumping logic based on current position and environment (e.g., ground vs. liquid).
     *
     * @param position       The current position of the player in absolute world block coordinates (LOD 0).
     * @param velocityChange The vector to modify with the jump velocity. Measured in blocks per gametick (LOD 0).
     * @param jumpStrength   The base jump strength to apply when on ground. Measured in blocks per gametick (LOD 0).
     * @param swimStrength   The swim strength to apply when in liquid. Measured in blocks per gametick (LOD 0).
     */
    void handleJump(Position position, Vector3f velocityChange, float jumpStrength, float swimStrength) {
        if (movement.isWideGrounded()) velocityChange.y = jumpStrength;
        else velocityChange.y += intersectedVolume(position, this, WATER) * swimStrength + intersectedVolume(position, this, LAVA) * swimStrength;
    }

    /**
     * Calculates the player's movement speed based on their state and environment.
     *
     * @param lastPosition  The player's position in the last gametick in absolute world block coordinates (LOD 0).
     * @param movementSpeed The base movement speed. Measured in blocks per gametick (LOD 0).
     * @param inAirSpeed    The movement speed when in the air. Measured in blocks per gametick (LOD 0).
     * @param swimStrength  The multiplier applied when in liquid.
     * @return The calculated movement speed in blocks per gametick (LOD 0).
     */
    float getMovementSpeed(Position lastPosition, float movementSpeed, float inAirSpeed, float swimStrength) {
        float speed = movement.isWideGrounded() ? movementSpeed : inAirSpeed;
        speed += intersectedVolume(lastPosition, this, WATER) * swimStrength * movementSpeed * 0.25F;
        speed += intersectedVolume(lastPosition, this, LAVA) * swimStrength * movementSpeed * 0.25F;
        return speed;
    }


    /**
     * Normalizes the velocity vector so that its maximum component is 1 (or -1), preserving direction.
     *
     * @param velocity The velocity vector to normalize.
     */
    static void normalizeToMaxComponent(Vector3f velocity) {
        float max = Math.abs(velocity.get(velocity.maxComponent()));
        if (max < 1E-4F) return;
        velocity.normalize(max);
    }

    /**
     * Normalizes the X and Z components of the velocity vector so that the horizontal magnitude is consistent with the maximum component.
     *
     * @param velocity The velocity vector whose X and Z components should be normalized.
     */
    static void normalizeXZToMaxComponent(Vector3f velocity) {
        float max = Math.max(Math.abs(velocity.x), Math.abs(velocity.z));
        if (max < 1E-4F) return;
        float normalizer = (float) (max / Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z));
        velocity.x *= normalizer;
        velocity.z *= normalizer;
    }

    /**
     * Transforms a relative velocity into a world-space direction based on a given direction vector.
     *
     * @param relativeVelocity The relative velocity vector to transform.
     * @param direction        The direction vector to use for the transformation (usually player rotation).
     */
    static void toWorldDirection(Vector3f relativeVelocity, Vector3f direction) {
        relativeVelocity.set(
                relativeVelocity.x * direction.x + relativeVelocity.z * direction.z,
                relativeVelocity.y,
                relativeVelocity.x * direction.z - relativeVelocity.z * direction.x
        );
    }

    /**
     * Applies horizontal (X and Z) movement acceleration based on user input.
     *
     * @param velocityChange      The vector to modify with the movement acceleration. Measured in blocks per gametick (LOD 0).
     * @param speed               The base movement speed to apply.
     * @param sprintSpeedModifier The multiplier to apply when sprinting.
     */
    static void applyXZMovement(Vector3f velocityChange, float speed, float sprintSpeedModifier) {
        if (Input.isKeyPressed(KeySettings.MOVE_FORWARD)) velocityChange.x += speed;
        if (Input.isKeyPressed(KeySettings.SPRINT)) velocityChange.mul(sprintSpeedModifier);

        if (Input.isKeyPressed(KeySettings.MOVE_BACK)) velocityChange.x -= speed;

        if (Input.isKeyPressed(KeySettings.MOVE_RIGHT)) velocityChange.z -= speed;
        if (Input.isKeyPressed(KeySettings.MOVE_LEFT)) velocityChange.z += speed;
    }

    static void applyGravity(Vector3f velocity) {
        velocity.y -= GRAVITY_ACCELERATION;
    }

    /**
     * Calculates the volume of a specific material that intersects with the player's hitbox.
     *
     * @param position       The current position of the player in absolute world block coordinates (LOD 0).
     * @param state          The current movement state, used to determine hitbox size.
     * @param targetMaterial The material identifier to check for intersections.
     * @return The number of blocks of the target material intersecting the hitbox (LOD 0).
     */
    static float intersectedVolume(Position position, MovementState state, byte targetMaterial) {
        if (ToggleSettings.NO_CLIP.value()) return 0;

        World world = Game.getWorld();
        Vector3i hitboxSize = state.getHitboxSize();

        long startX = position.longX + MathUtils.floor(position.fractionX - hitboxSize.x * 0.5F);
        long startY = position.longY;
        long startZ = position.longZ + MathUtils.floor(position.fractionZ - hitboxSize.z * 0.5F);

        int width = hitboxSize.x + 1;
        int height = hitboxSize.y;
        int depth = hitboxSize.z + 1;

        float volumne = 0.0F;
        for (long x = startX; x < startX + width; x++)
            for (long y = startY; y < startY + height; y++)
                for (long z = startZ; z < startZ + depth; z++) {
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
