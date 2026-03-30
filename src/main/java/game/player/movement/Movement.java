package game.player.movement;

import core.utils.MathUtils;

import game.server.Game;
import game.server.World;
import game.server.material.Properties;
import game.settings.ToggleSettings;
import game.utils.Position;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Movement {

    public Movement() {
        state = new WalkingState();
        state.movement = this;
    }

    public Position computeNextGameTickPosition(Position lastPosition, Vector3f rotation) {
        Position position = new Position(lastPosition);
        grounded = velocity.y == 0.0F && checkGrounded(position);

        Vector3f acceleration = Game.getPlayer().canDoActiveActions() ? state.computeNextGameTickAcceleration(rotation, position) : new Vector3f(0.0F);
        state.changeVelocity(velocity, acceleration, position, rotation);

        velocity.set(move(position));
        renderVelocity = position.vectorFrom(lastPosition);

        if (ToggleSettings.NO_CLIP.value() || noCollision(position, state.next, -1)) state = state.next;
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


    private boolean checkGrounded(Position position) {
        Vector3i hitboxSize = state.getHitboxSize();
        World world = Game.getWorld();

        long minX = position.longX + MathUtils.floor(position.fractionX - hitboxSize.x * 0.5F);
        long minZ = position.longZ + MathUtils.floor(position.fractionZ - hitboxSize.z * 0.5F);
        long y = position.longY - 1;
        int width = hitboxSize.x + 1;
        int depth = hitboxSize.z + 1;

        for (long x = minX; x != minX + width; x++)
            for (long z = minZ; z != minZ + depth; z++) {
                byte material = world.getMaterial(x, y, z, 0);
                if (Properties.doesntHaveProperties(material, NO_COLLISION)) return true;
            }
        return false;
    }

    private Vector3f move(Position position) {
        if (ToggleSettings.NO_CLIP.value()) {
            position.add(velocity.x, velocity.y, velocity.z);
            return velocity;
        }

        Vector3f nextGTVelocity = new Vector3f(velocity);
        Vector3f toMoveDistance = new Vector3f(this.velocity);
        Vector3i direction = new Vector3i();
        Vector3d units = new Vector3d();
        Vector3d lengths = new Vector3d();
        computeRayCastConstants(position, toMoveDistance, direction, units, lengths);

        while (toMoveDistance.lengthSquared() != 0) {
            int minComponent = lengths.minComponent();
            move(nextGTVelocity, toMoveDistance, position, direction, units, lengths, minComponent);
        }
        return nextGTVelocity;
    }

    private void move(Vector3f nextVelocity, Vector3f toMoveDistance, Position position, Vector3i direction, Vector3d units, Vector3d lengths, int component) {
        float toMove = toMoveDistance.get(component);
        float moved;
        if (Math.abs(toMove) <= 1) {
            moved = toMove;
            position.addComponent(component, toMove);
            toMoveDistance.setComponent(component, 0);
        } else {
            int directionComponent = direction.get(component);
            moved = directionComponent;
            position.addComponent(component, directionComponent);
            toMoveDistance.setComponent(component, toMove - directionComponent);
        }
        if (shouldStopAtEdge(position, component, moved)) stopAndUndoMove(nextVelocity, toMoveDistance, position, direction, units, lengths, component, moved);
        if (collides(position, component)) resolveCollision(nextVelocity, toMoveDistance, position, direction, units, lengths, component, moved);
        else advanceLength(units, lengths, component);
        if (toMoveDistance.get(component) == 0) lengths.setComponent(component, Double.POSITIVE_INFINITY);
    }

    private void resolveCollision(Vector3f nextVelocity, Vector3f toMoveDistance, Position position, Vector3i direction, Vector3d units, Vector3d lengths, int component, float moved) {
        float requiredStepHeight = getRequiredStepHeight(position, component);
        if (canAutoStep(position, requiredStepHeight)) {
            position.addComponent(Y_COMPONENT, requiredStepHeight);
            advanceLength(units, lengths, component);
            toMoveDistance.y = 0.0F;
            computeRayCastConstants(position, toMoveDistance, direction, units, lengths);
            return;
        }

        stopAndUndoMove(nextVelocity, toMoveDistance, position, direction, units, lengths, component, moved);
    }

    private void stopAndUndoMove(Vector3f nextVelocity, Vector3f toMoveDistance, Position position, Vector3i direction, Vector3d units, Vector3d lengths, int component, float moved) {
        nextVelocity.setComponent(component, 0);
        lengths.setComponent(component, Double.POSITIVE_INFINITY);
        toMoveDistance.setComponent(component, 0);
        position.addComponent(component, -moved);
        computeRayCastConstants(position, toMoveDistance, direction, units, lengths);
    }

    private float getRequiredStepHeight(Position position, int component) {
        if (component == Y_COMPONENT) return Float.POSITIVE_INFINITY;
        Vector3i hitboxSize = state.getHitboxSize();
        World world = Game.getWorld();

        long startX = getStartX(position, hitboxSize, component);
        long startY = getStartY(position, hitboxSize, component);
        long startZ = getStartZ(position, hitboxSize, component);

        int width = component == X_COMPONENT ? 1 : hitboxSize.x + 1;
        int height = hitboxSize.y;
        int depth = component == Z_COMPONENT ? 1 : hitboxSize.z + 1;

        for (long y = startY + height - 1; y != startY - 1; y--)
            for (long x = startX; x != startX + width; x++)
                for (long z = startZ; z != startZ + depth; z++) {
                    byte material = world.getMaterial(x, y, z, 0);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return (y - position.longY + 1) - position.fractionY;
                }
        return 0.0F;
    }

    private boolean canAutoStep(Position position, float requiredStepHeight) {
        if (requiredStepHeight == 0.0F) return false;

        boolean swimming = MovementState.intersectsLiquid(position, state);
        float maxStepHeight = state.getMaxAutoStepHeight() * (swimming ? 2.5F : 1.0F);
        if ((!checkGrounded(position) && !swimming) || requiredStepHeight > maxStepHeight) return false;

        Position steppedPosition = new Position(position);
        steppedPosition.addComponent(Y_COMPONENT, requiredStepHeight);
        return noCollision(steppedPosition, state, -1);
    }

    private boolean collides(Position position, int component) {
        Vector3i hitboxSize = state.getHitboxSize();

        long startX = getStartX(position, hitboxSize, component);
        long startY = getStartY(position, hitboxSize, component);
        long startZ = getStartZ(position, hitboxSize, component);

        int width = component == X_COMPONENT ? 1 : hitboxSize.x + 1;
        int height = component == Y_COMPONENT ? 1 : hitboxSize.y;
        int depth = component == Z_COMPONENT ? 1 : hitboxSize.z + 1;

        return collides(startX, startY, startZ, width, height, depth);
    }

    private boolean shouldStopAtEdge(Position position, int component, float moved) {
        Position originPosition = new Position(position).addComponent(component, -moved).addComponent(Y_COMPONENT, -state.getMaxAutoStepHeight());
        boolean grounded = wideCollides(originPosition, state, component);
        if (!grounded || component == Y_COMPONENT || velocity.y > 0.0F || !state.preventsFallingFromEdge()) return false;

        Position loweredPosition = new Position(position).addComponent(Y_COMPONENT, -state.getMaxAutoStepHeight());
        return noCollision(loweredPosition, state, component);
    }

    private boolean noCollision(Position position, MovementState state, int component) {
        Vector3i hitboxSize = state.getHitboxSize();

        int xOffset = component == X_COMPONENT ? (velocity.x > 0.0F ? -1 : 1) : 0;
        int zOffset = component == Z_COMPONENT ? (velocity.z > 0.0F ? -1 : 1) : 0;
        long startX = position.longX + MathUtils.floor(position.fractionX - (hitboxSize.x + xOffset) * 0.5F);
        long startY = position.longY;
        long startZ = position.longZ + MathUtils.floor(position.fractionZ - (hitboxSize.z + zOffset) * 0.5F);

        return !collides(startX, startY, startZ, hitboxSize.x + 1, hitboxSize.y, hitboxSize.z + 1);
    }

    private boolean wideCollides(Position position, MovementState state, int component) {
        Vector3i hitboxSize = state.getHitboxSize();

        int xOffset = component == X_COMPONENT ? (velocity.x > 0.0F ? -1 : 1) : 0;
        int zOffset = component == Z_COMPONENT ? (velocity.z > 0.0F ? -1 : 1) : 0;
        long startX = position.longX + MathUtils.floor(position.fractionX - (hitboxSize.x + 1 + xOffset) * 0.5F);
        long startY = position.longY;
        long startZ = position.longZ + MathUtils.floor(position.fractionZ - (hitboxSize.z + 1 + zOffset) * 0.5F);

        return collides(startX, startY, startZ, hitboxSize.x + 2, hitboxSize.y, hitboxSize.z + 2);
    }

    private void computeRayCastConstants(Position position, Vector3f velocity, Vector3i direction, Vector3d units, Vector3d lengths) {
        Vector3f cornerFraction = getCornerFractionPosition(position);
        direction.x = velocity.x < 0 ? -1 : 1;
        direction.y = velocity.y < 0 ? -1 : 1;
        direction.z = velocity.z < 0 ? -1 : 1;

        double dirXSquared = velocity.x * velocity.x;
        double dirYSquared = velocity.y * velocity.y;
        double dirZSquared = velocity.z * velocity.z;
        units.x = (float) Math.sqrt(1 + (dirYSquared + dirZSquared) / dirXSquared);
        units.y = (float) Math.sqrt(1 + (dirXSquared + dirZSquared) / dirYSquared);
        units.z = (float) Math.sqrt(1 + (dirXSquared + dirYSquared) / dirZSquared);

        lengths.x = units.x * (this.velocity.x < 0 ? cornerFraction.x : 1 - cornerFraction.x);
        lengths.y = units.y * (this.velocity.y < 0 ? cornerFraction.y : 1 - cornerFraction.y);
        lengths.z = units.z * (this.velocity.z < 0 ? cornerFraction.z : 1 - cornerFraction.z);

        if (Double.isNaN(lengths.x)) lengths.x = Double.POSITIVE_INFINITY;
        if (Double.isNaN(lengths.y)) lengths.y = Double.POSITIVE_INFINITY;
        if (Double.isNaN(lengths.z)) lengths.z = Double.POSITIVE_INFINITY;
    }

    private Vector3f getCornerFractionPosition(Position position) {
        Vector3i hitboxSize = state.getHitboxSize();
        Vector3f cornerFraction = position.fractionPosition();
        return cornerFraction.set(
                MathUtils.fraction(cornerFraction.x + (velocity.x > 0 ? (hitboxSize.x + 1) * 0.5F : -hitboxSize.x * 0.5F)),
                MathUtils.fraction(cornerFraction.y + (velocity.y > 0 ? hitboxSize.y : 0)),
                MathUtils.fraction(cornerFraction.z + (velocity.z > 0 ? (hitboxSize.z + 1) * 0.5F : -hitboxSize.z * 0.5F))
        );
    }

    private long getStartX(Position position, Vector3i hitboxSize, int component) {
        float offset = component == X_COMPONENT && velocity.x > 0 ? hitboxSize.x * 0.5F + 0.5F : -hitboxSize.x * 0.5F;
        return position.longX + MathUtils.floor(position.fractionX + offset);
    }

    private long getStartY(Position position, Vector3i hitboxSize, int component) {
        float offset = component == Y_COMPONENT && velocity.y > 0 ? hitboxSize.y : 0;
        return position.longY + MathUtils.floor(position.fractionY + offset);
    }

    private long getStartZ(Position position, Vector3i hitboxSize, int component) {
        float offset = component == Z_COMPONENT && velocity.z > 0 ? hitboxSize.z * 0.5F + 0.5F : -hitboxSize.z * 0.5F;
        return position.longZ + MathUtils.floor(position.fractionZ + offset);
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

    private static void advanceLength(Vector3d units, Vector3d lengths, int component) {
        double lengthComponent = lengths.get(component);
        lengths.setComponent(component, lengthComponent + units.get(component));
    }


    private MovementState state;
    private boolean grounded;
    private final Vector3f velocity = new Vector3f();
    private Vector3f renderVelocity = new Vector3f();
}
