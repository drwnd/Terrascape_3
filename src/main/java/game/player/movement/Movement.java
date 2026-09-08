package game.player.movement;

import core.rendering_api.Input;
import core.sound.Sound;

import game.server.Game;
import game.server.World;
import game.server.material.Material;
import game.server.material.Properties;
import game.settings.FloatSettings;
import game.settings.KeySettings;
import game.settings.ToggleSettings;
import game.utils.Position;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Movement {

    public Movement() {
        state = MovementState.load(WalkingState.class);
        state.movement = this;
    }

    public Position computeNextGameTickPosition(Position lastPosition, Vector3f rotation) {
        Position position = new Position(lastPosition);
        grounded = velocity.y == 0.0F && checkGrounded(position);

        Vector3f acceleration = Game.getPlayer().canDoActiveActions() ? state.computeNextGameTickAcceleration(rotation, position) : new Vector3f(0.0F);
        state.changeVelocity(velocity, acceleration, position, rotation);

        velocity.set(move(position));
        oldRenderVelocity = renderVelocity;
        renderVelocity = position.vectorFrom(lastPosition);
        playFootstepSound(position);

        if (ToggleSettings.NO_CLIP.value() || noCollision(position, state.next)) state = state.next;
        state.movement = this;
        return position;
    }

    public void handleInput(int button, int action) {
        state.handleInput(button, action);
    }

    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public Vector3f getRenderVelocity() {
        return new Vector3f(renderVelocity);
    }

    public Vector3f getOldRenderVelocity() {
        return new Vector3f(oldRenderVelocity);
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
    }

    public void setState(byte identifier) {
        MovementState state = MovementState.getStateFromIdentifier(identifier);
        if (state == null) return;
        this.state = state;
        this.state.movement = this;
    }

    public void setState(MovementState state) {
        if (state == null) return;
        this.state = state;
        this.state.movement = this;
    }

    public MovementState getState() {
        return state;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public static long minX(Position position, Vector3i hitboxSize) {
        return position.longX - (hitboxSize.x >> 1);
    }

    public static long minY(Position position, @SuppressWarnings("unused") Vector3i hitboxSize) {
        return position.longY;
    }

    public static long minZ(Position position, Vector3i hitboxSize) {
        return position.longZ - (hitboxSize.z >> 1);
    }

    public static long maxX(Position position, Vector3i hitboxSize) {
        return minX(position, hitboxSize) + hitboxSize.x;
    }

    public static long maxY(Position position, Vector3i hitboxSize) {
        return minY(position, hitboxSize) + hitboxSize.y;
    }

    public static long maxZ(Position position, Vector3i hitboxSize) {
        return minZ(position, hitboxSize) + hitboxSize.z;
    }


    private Vector3f move(Position position) {
        if (ToggleSettings.NO_CLIP.value()) {
            position.add(velocity.x, velocity.y, velocity.z);
            return velocity;
        }

        Vector3f nextGTVelocity = new Vector3f(velocity);
        Vector3f toMoveDistance = new Vector3f(velocity);
        Vector3d units = new Vector3d(), lengths = new Vector3d();
        groundSnappedHeight = autoSteppedHeight = 0;
        computeRayCastConstants(toMoveDistance, units, lengths);
        while (toMoveDistance.lengthSquared() != 0) move(nextGTVelocity, toMoveDistance, position, units, lengths, lengths.minComponent());
        return nextGTVelocity;
    }

    private void move(Vector3f nextVelocity, Vector3f toMoveDistance, Position position, Vector3d units, Vector3d lengths, int component) {
        float toMove = toMoveDistance.get(component), moved;
        if (toMove == 0) {
            advanceLength(units, lengths, component);
            lengths.setComponent(component, Double.POSITIVE_INFINITY);
            return;
        }
        if (Math.abs(toMove) <= 1) {
            moved = toMove;
            position.addComponent(component, toMove);
            toMoveDistance.setComponent(component, 0);
        } else {
            int directionComponent = toMove > 0 ? 1 : -1;
            moved = directionComponent;
            position.addComponent(component, directionComponent);
            toMoveDistance.setComponent(component, toMove - directionComponent);
        }
        snapToGround(position, component, moved);
        if (shouldStopAtEdge(position, component, moved)) stopAndUndoMove(nextVelocity, toMoveDistance, position, units, lengths, component, moved);
        if (collides(position, component)) resolveCollision(nextVelocity, toMoveDistance, position, units, lengths, component, moved);
        else advanceLength(units, lengths, component);
        if (toMoveDistance.get(component) == 0) lengths.setComponent(component, Double.POSITIVE_INFINITY);
    }

    private void resolveCollision(Vector3f nextVelocity, Vector3f toMoveDistance, Position position, Vector3d units, Vector3d lengths,
                                  int component, float moved) {
        int requiredStepHeight = getRequiredStepHeight(position, component);
        if (canAutoStep(position, requiredStepHeight)) {
            position.addComponent(Y_COMPONENT, requiredStepHeight);
            autoSteppedHeight += requiredStepHeight;
        } else stopAndUndoMove(nextVelocity, toMoveDistance, position, units, lengths, component, moved);
    }

    private int getRequiredStepHeight(Position position, int component) {
        int maxStepHeight = state.getMaxAutoStepHeight();
        if (component == Y_COMPONENT || autoSteppedHeight > maxStepHeight * MAX_STEP_HEIGHT_CAP_MULTIPLIER) return Integer.MAX_VALUE;
        Vector3i hitboxSize = state.getHitboxSize();
        World world = Game.getWorld();

        long startX = getStartX(position, hitboxSize, component);
        long startY = getStartY(position, hitboxSize, component);
        long startZ = getStartZ(position, hitboxSize, component);

        int width = component == X_COMPONENT ? 1 : hitboxSize.x;
        int depth = component == Z_COMPONENT ? 1 : hitboxSize.z;

        for (long y = startY + maxStepHeight + 2; y != startY - 1; y--)
            for (long x = startX; x != startX + width; x++)
                for (long z = startZ; z != startZ + depth; z++) {
                    byte material = world.getMaterial(x, y, z, 0);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return (int) (y - position.longY + 1);
                }
        return Integer.MAX_VALUE;
    }

    private boolean canAutoStep(Position position, int requiredStepHeight) {
        int maxStepHeight = state.getMaxAutoStepHeight();
        if (autoSteppedHeight + requiredStepHeight > maxStepHeight * MAX_STEP_HEIGHT_CAP_MULTIPLIER) return false;

        boolean swimming = MovementState.intersectsLiquid(position, state);
        if (swimming && Input.isKeyPressed(KeySettings.JUMP)) maxStepHeight += 2;
        if ((!checkGrounded(position) && !swimming) || requiredStepHeight > maxStepHeight) return false;

        Position steppedPosition = new Position(position);
        steppedPosition.addComponent(Y_COMPONENT, requiredStepHeight);
        return noCollision(steppedPosition, state);
    }

    private boolean shouldStopAtEdge(Position position, int component, float moved) {
        if (component == Y_COMPONENT || velocity.y > 0.0F) return false;
        int maxStepHeight = state.getMaxAutoStepHeight();
        Position originPosition = new Position(position).addComponent(component, -moved).addComponent(Y_COMPONENT, -maxStepHeight);
        if (!wideCollides(originPosition, state)) return false;

        if (groundSnappedHeight > maxStepHeight * MAY_SNAP_HEIGHT_CAP_MULTIPLIER) return true;
        Position loweredPosition = new Position(position).addComponent(Y_COMPONENT, -maxStepHeight - 1);
        return state.preventsFallingFromEdge() && noCollision(loweredPosition, state);
    }

    private void snapToGround(Position position, int component, float moved) {
        int maxStepHeight = state.getMaxAutoStepHeight();
        if (component == Y_COMPONENT || groundSnappedHeight > maxStepHeight * MAY_SNAP_HEIGHT_CAP_MULTIPLIER) return;
        Position originPosition = new Position(position).addComponent(component, -moved);
        if (!checkGrounded(originPosition)) return;

        Position loweredPosition = new Position(position).addComponent(Y_COMPONENT, -maxStepHeight - 1);
        if (noCollision(loweredPosition, state)) return;

        int requiredSnapHeight = 0;
        Position snappedPosition = new Position(position);

        while (!checkGrounded(snappedPosition)) {
            snappedPosition.addComponent(Y_COMPONENT, -1);
            requiredSnapHeight++;
        }

        groundSnappedHeight += requiredSnapHeight;
        if (groundSnappedHeight <= maxStepHeight * MAY_SNAP_HEIGHT_CAP_MULTIPLIER) position.addComponent(Y_COMPONENT, -requiredSnapHeight);
    }

    private void playFootstepSound(Position position) {
        long currentGameTick = Game.getServer().getCurrentGameTick();
        int ticksBetweenFootsteps = state.ticksBetweenFootsteps();
        if (ticksBetweenFootsteps < 0 || currentGameTick < lastFootstepTick + ticksBetweenFootsteps || velocity.lengthSquared() < 1) return;
        lastFootstepTick = currentGameTick;

        byte standingMaterial = state.getStandingMaterial(position);
        Sound.play3D(Material.getStepSounds(standingMaterial), FloatSettings.FOOTSTEPS_AUDIO, position, null);
    }

    private long getStartX(Position position, Vector3i hitboxSize, int component) {
        return component == X_COMPONENT && velocity.x > 0 ? maxX(position, hitboxSize) - 1 : minX(position, hitboxSize);
    }

    private long getStartY(Position position, Vector3i hitboxSize, int component) {
        return component == Y_COMPONENT && velocity.y > 0 ? maxY(position, hitboxSize) - 1 : minY(position, hitboxSize);
    }

    private long getStartZ(Position position, Vector3i hitboxSize, int component) {
        return component == Z_COMPONENT && velocity.z > 0 ? maxZ(position, hitboxSize) - 1 : minZ(position, hitboxSize);
    }

    private boolean collides(Position position, int component) {
        Vector3i hitboxSize = state.getHitboxSize();

        long startX = getStartX(position, hitboxSize, component);
        long startY = getStartY(position, hitboxSize, component);
        long startZ = getStartZ(position, hitboxSize, component);

        int width = component == X_COMPONENT ? 1 : hitboxSize.x;
        int height = component == Y_COMPONENT ? 1 : hitboxSize.y;
        int depth = component == Z_COMPONENT ? 1 : hitboxSize.z;

        return collides(startX, startY, startZ, width, height, depth);
    }

    private boolean checkGrounded(Position position) {
        Vector3i hitboxSize = state.getHitboxSize();

        long minX = minX(position, hitboxSize);
        long minY = minY(position, hitboxSize);
        long minZ = minZ(position, hitboxSize);
        return collides(minX, minY - 1, minZ, hitboxSize.x, 1, hitboxSize.z);
    }


    private static boolean noCollision(Position position, MovementState state) {
        Vector3i hitboxSize = state.getHitboxSize();
        long startX = minX(position, hitboxSize);
        long startY = minY(position, hitboxSize);
        long startZ = minZ(position, hitboxSize);

        return !collides(startX, startY, startZ, hitboxSize.x, hitboxSize.y, hitboxSize.z);
    }

    private static boolean wideCollides(Position position, MovementState state) {
        Vector3i hitboxSize = state.getHitboxSize();

        long startX = minX(position, hitboxSize) - 1;
        long startY = minY(position, hitboxSize);
        long startZ = minZ(position, hitboxSize) - 1;

        return collides(startX, startY, startZ, hitboxSize.x + 2, hitboxSize.y, hitboxSize.z + 2);
    }

    private static boolean collides(long startX, long startY, long startZ, int width, int height, int depth) {
        World world = Game.getWorld();

        for (long x = startX; x != startX + width; x++)
            for (long y = startY; y != startY + height; y++)
                for (long z = startZ; z != startZ + depth; z++) {
                    byte material = world.getMaterial(x, y, z, 0);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return true;
                }
        return false;
    }

    private static void stopAndUndoMove(Vector3f nextVelocity, Vector3f toMoveDistance, Position position, Vector3d units, Vector3d lengths,
                                        int component, float moved) {
        nextVelocity.setComponent(component, 0);
        lengths.setComponent(component, Double.POSITIVE_INFINITY);
        toMoveDistance.setComponent(component, 0);
        position.addComponent(component, -moved);

        if (component == X_COMPONENT) position.fractionX = moved < 0 ? 0 : 0.9999999F;
        if (component == Y_COMPONENT) position.fractionY = moved < 0 ? 0 : 0.9999999F;
        if (component == Z_COMPONENT) position.fractionZ = moved < 0 ? 0 : 0.9999999F;

        computeRayCastConstants(toMoveDistance, units, lengths);
    }

    private static void computeRayCastConstants(Vector3f toMoveDistance, Vector3d units, Vector3d lengths) {
        double dirXSquared = toMoveDistance.x * toMoveDistance.x;
        double dirYSquared = toMoveDistance.y * toMoveDistance.y;
        double dirZSquared = toMoveDistance.z * toMoveDistance.z;
        units.x = Math.sqrt(1 + (dirYSquared + dirZSquared) / dirXSquared);
        units.y = Math.sqrt(1 + (dirXSquared + dirZSquared) / dirYSquared);
        units.z = Math.sqrt(1 + (dirXSquared + dirYSquared) / dirZSquared);

        lengths.zero();
    }

    private static void advanceLength(Vector3d units, Vector3d lengths, int component) {
        lengths.setComponent(component, lengths.get(component) + units.get(component));
    }


    private long lastFootstepTick = 0;
    private MovementState state;
    private boolean grounded;
    private int autoSteppedHeight, groundSnappedHeight;
    private final Vector3f velocity = new Vector3f();
    private Vector3f renderVelocity = new Vector3f(), oldRenderVelocity = new Vector3f();

    private static final float MAX_STEP_HEIGHT_CAP_MULTIPLIER = 1.6F;
    private static final float MAY_SNAP_HEIGHT_CAP_MULTIPLIER = 1.6F;
}
