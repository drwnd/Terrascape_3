package game.player.movement;

import com.google.gson.Gson;
import core.assets.AssetManager;
import core.rendering_api.Input;
import core.utils.FileManager;

import game.assets.Model;
import game.player.rendering.Camera;
import game.server.Game;
import game.server.World;
import game.server.material.Properties;
import game.settings.KeySettings;
import game.settings.ToggleSettings;
import game.utils.Position;

import org.joml.Vector3f;
import org.joml.Vector3i;

import java.nio.file.Path;

import static game.utils.Constants.*;

public abstract class MovementState {

    MovementState next = this;

    public static <T extends MovementState> T load(Class<T> tClass) {
        String json = FileManager.loadJson(AssetManager.getAssetFilepath(Path.of("movementStates", tClass.getSimpleName() + ".json")));
        return new Gson().fromJson(json, tClass);
    }

    /**
     * Computes the acceleration the Player should have in the next GameTick.
     * Only gets called when the Player is actively moving.
     *
     * @param playerRotation The rotation of the player (not necessarily the camera)
     * @param lastPosition   The position of the player in the last GameTick
     * @return The acceleration of the Player.
     */
    abstract Vector3f computeNextGameTickAcceleration(Vector3f playerRotation, Position lastPosition);

    /**
     * Changes the current Velocity the Player has.
     * Gets called every GameTick regardless of the Player being able to move actively.
     * Therefore, no inputs should be queried in this method.
     *
     * @param velocity       The current Velocity of the Player. This is also the output variable.
     * @param acceleration   The acceleration computed in computeNextGameTickAcceleration(...) or 0 if the Player can't move actively.
     * @param playerPosition The position of the Player.
     * @param playerRotation The rotation of the Player.
     */
    void changeVelocity(Vector3f velocity, Vector3f acceleration, Position playerPosition, Vector3f playerRotation) {
        int waterIntersection = intersectedVolume(playerPosition, this, WATER);
        int lavaIntersection = intersectedVolume(playerPosition, this, LAVA);

        float drag = movement.isGrounded() ? WALKING_DRAG : AIR_DRAG;
        float liquidDrag = (float) (Math.pow(WATER_DRAG, waterIntersection)) * (float) (Math.pow(LAVA_DRAG, lavaIntersection));

        velocity.add(acceleration).mul(drag).mul(liquidDrag);
        applyGravity(velocity);
        velocity.y += waterIntersection * WATER_BUOYANCY + lavaIntersection * LAVA_BUOYANCY * (Input.isKeyPressed(KeySettings.JUMP) ? 2 : 1);
    }

    /**
     * Animates the given Model (assumed to be a player model).
     *
     * @param playerCharacter The model to animate.
     * @param camera          The camera of the player.
     * @param animationTimer  A value to be used as input into trigonometric functions. In whatever unit is convenient.
     * @param frameTime       The time since the last frame in ms.
     * @return The new value of {@code animationTimer}
     */
    public abstract double applyAnimation(Model playerCharacter, Camera camera, double animationTimer, float frameTime);

    byte getStandingMaterial(Position position) {
        World world = Game.getWorld();

        byte centerMaterial = world.getMaterial(position.longX, position.longY - 1, position.longZ, 0);
        if (Properties.doesntHaveProperties(centerMaterial, NO_COLLISION)) return centerMaterial;

        long minX = Movement.minX(position, hitboxSize);
        long minZ = Movement.minZ(position, hitboxSize);
        long y = position.longY - 1;
        int width = hitboxSize.x;
        int depth = hitboxSize.z;

        for (long x = minX; x != minX + width; x++)
            for (long z = minZ; z != minZ + depth; z++) {
                byte material = world.getMaterial(x, y, z, 0);
                if (Properties.doesntHaveProperties(material, NO_COLLISION)) return material;
            }

        return centerMaterial;
    }

    abstract void handleInput(int key, int action);

    public abstract byte getIdentifier();

    int ticksBetweenFootsteps() {
        return ticksBetweenFootsteps;
    }

    final int getMaxAutoStepHeight() {
        return maxAutoStepHeight;
    }

    final boolean preventsFallingFromEdge() {
        return preventsFallingFromEdge;
    }

    public boolean hideBodyInFirstPerson() {
        return hideBodyInFirstPerson;
    }

    public boolean hideHeadInFirstPerson() {
        return hideHeadInFirstPerson;
    }

    public final Vector3i getHitboxSize() {
        return hitboxSize;
    }

    public final float getCameraElevation() {
        return cameraElevation;
    }


    public static MovementState getStateFromIdentifier(byte identifier) {
        return switch (identifier) {
            case 0 -> MovementState.load(WalkingState.class);
            case 1 -> MovementState.load(SwimmingState.class);
            case 2 -> MovementState.load(SneakingState.class);
            case 3 -> MovementState.load(FlyingState.class);
            case 4 -> MovementState.load(CrawlingState.class);
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
        if (Input.isKeyPressed(KeySettings.MOVE_FORWARD)) velocityChange.x += speed;
        if (Input.isKeyPressed(KeySettings.SPRINT)) velocityChange.mul(sprintSpeedModifier);

        if (Input.isKeyPressed(KeySettings.MOVE_BACK)) velocityChange.x -= speed;

        if (Input.isKeyPressed(KeySettings.MOVE_RIGHT)) velocityChange.z -= speed;
        if (Input.isKeyPressed(KeySettings.MOVE_LEFT)) velocityChange.z += speed;
    }

    static void applyGravity(Vector3f velocity) {
        velocity.y -= GRAVITY_ACCELERATION;
    }

    static int intersectedVolume(Position position, MovementState state, byte targetMaterial) {
        if (ToggleSettings.NO_CLIP.value()) return 0;

        World world = Game.getWorld();
        Vector3i hitboxSize = state.hitboxSize;

        long startX = Movement.minX(position, hitboxSize);
        long startY = Movement.minY(position, hitboxSize);
        long startZ = Movement.minZ(position, hitboxSize);

        int width = hitboxSize.x;
        int height = hitboxSize.y;
        int depth = hitboxSize.z;

        int volume = 0;
        for (long x = startX; x < startX + width; x++)
            for (long y = startY; y < startY + height; y++)
                for (long z = startZ; z < startZ + depth; z++)
                    if (targetMaterial == world.getMaterial(x, y, z, 0)) volume++;
        return volume;
    }

    static boolean intersectsLiquid(Position position, MovementState state) {
        return intersectedVolume(position, state, WATER) != 0 || intersectedVolume(position, state, LAVA) != 0;
    }

    protected Movement movement;
    protected long lastJumpTime = System.nanoTime() - JUMP_FLYING_INTERVALL;
    @SuppressWarnings("unused")
    private int maxAutoStepHeight, ticksBetweenFootsteps;
    @SuppressWarnings({"unused", "FieldCanBeLocal", "FieldMayBeFinal"})
    private boolean preventsFallingFromEdge, hideBodyInFirstPerson = false, hideHeadInFirstPerson = true;
    @SuppressWarnings("unused")
    private float cameraElevation;
    @SuppressWarnings("unused")
    private Vector3i hitboxSize;

    private static final float GRAVITY_ACCELERATION = 1.28F;
    private static final float WATER_BUOYANCY = 0.0005F;
    private static final float LAVA_BUOYANCY = 0.0006F;
    static final float WATER_DRAG = 0.99935F;
    static final float LAVA_DRAG = 0.999F;
    static final long JUMP_FLYING_INTERVALL = 300_000_000; // 0.3s
    static final float WALKING_DRAG = 0.6F;
    static final float AIR_DRAG = 0.94F;
}
