package game.player.movement;

import game.server.Game;
import game.server.World;
import game.server.material.Properties;
import game.utils.Position;

import game.utils.Utils;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class Movement {

    public Movement() {
        state = new FlyingState();
        state.movement = this;
    }

    public Position computeNextGameTickPosition(Position lastPosition, Vector3f rotation) {
        Position position = new Position(lastPosition);
        grounded = velocity.y == 0.0F && checkGrounded(position);

        Vector3f acceleration = Game.getPlayer().canDoActiveActions() ? state.computeNextGameTickAcceleration(rotation, position) : new Vector3f(0.0F);
        state.changeVelocity(velocity, acceleration, position, rotation);

        velocity.set(move(position));
        renderVelocity = position.vectorFrom(lastPosition);

        if (Game.getPlayer().isNoClip() || !collides(position, state.next)) state = state.next;
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

    public MovementState getState() {
        return state;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public boolean collides(Position position, MovementState state) {
        Vector3i hitboxSize = state.getHitboxSize();

        int startX = position.intX + Utils.floor(position.fractionX - hitboxSize.x * 0.5F);
        int startY = position.intY;
        int startZ = position.intZ + Utils.floor(position.fractionZ - hitboxSize.z * 0.5F);

        int width = hitboxSize.x + 1;
        int height = hitboxSize.y;
        int depth = hitboxSize.z + 1;

        return collides(startX, startY, startZ, width, height, depth);
    }


    private boolean checkGrounded(Position position) {
        Vector3i hitboxSize = state.getHitboxSize();
        World world = Game.getWorld();

        int minX = position.intX + Utils.floor(position.fractionX - hitboxSize.x * 0.5F);
        int minZ = position.intZ + Utils.floor(position.fractionZ - hitboxSize.z * 0.5F);
        int width = hitboxSize.x + 1;
        int depth = hitboxSize.z + 1;
        int y = position.intY - 1;

        for (int x = minX; x < minX + width; x++)
            for (int z = minZ; z < minZ + depth; z++) {
                byte material = world.getMaterial(x, y, z, 0);
                if (Properties.doesntHaveProperties(material, NO_COLLISION)) return true;
            }
        return false;
    }

    private Vector3f move(Position position) {
        if (Game.getPlayer().isNoClip()) {
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
        if (shouldStopAtEdge(position, component)) stopAndUndoMove(nextVelocity, toMoveDistance, position, direction, units, lengths, component, moved);
        if (collides(position, component)) resolveCollision(nextVelocity, toMoveDistance, position, direction, units, lengths, component, moved);
        else advanceLength(units, lengths, component);
        if (toMoveDistance.get(component) == 0) lengths.setComponent(component, Double.POSITIVE_INFINITY);
    }

    private static void advanceLength(Vector3d units, Vector3d lengths, int component) {
        double lengthComponent = lengths.get(component);
        lengths.setComponent(component, lengthComponent + units.get(component));
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

        int startX = getStartX(position, hitboxSize, component);
        int startY = getStartY(position, hitboxSize, component);
        int startZ = getStartZ(position, hitboxSize, component);

        int width = component == X_COMPONENT ? 1 : hitboxSize.x + 1;
        int height = hitboxSize.y;
        int depth = component == Z_COMPONENT ? 1 : hitboxSize.z + 1;

        for (int y = startY + height - 1; y >= startY; y--)
            for (int x = startX; x < startX + width; x++)
                for (int z = startZ; z < startZ + depth; z++) {
                    byte material = world.getMaterial(x, y, z, 0);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return (y - position.intY + 1) - position.fractionY;
                }
        return 0.0F;
    }

    private boolean canAutoStep(Position position, float requiredStepHeight) {
        if (requiredStepHeight == 0.0F || !checkGrounded(position) || requiredStepHeight > state.getMaxAutoStepHeight()) return false;
        Position steppedPosition = new Position(position);
        steppedPosition.addComponent(Y_COMPONENT, requiredStepHeight);
        return !collides(steppedPosition, state);
    }

    private boolean collides(Position position, int component) {
        Vector3i hitboxSize = state.getHitboxSize();

        int startX = getStartX(position, hitboxSize, component);
        int startY = getStartY(position, hitboxSize, component);
        int startZ = getStartZ(position, hitboxSize, component);

        int width = component == X_COMPONENT ? 1 : hitboxSize.x + 1;
        int height = component == Y_COMPONENT ? 1 : hitboxSize.y;
        int depth = component == Z_COMPONENT ? 1 : hitboxSize.z + 1;

        return collides(startX, startY, startZ, width, height, depth);
    }

    private boolean shouldStopAtEdge(Position position, int component) {
        if (!grounded || velocity.y > 0.0F || !state.preventsFallingFromEdge()) return false;

        Position loweredPosition = new Position(position);
        loweredPosition.addComponent(Y_COMPONENT, -state.getMaxAutoStepHeight());
        return !collides(loweredPosition, state, component);
    }

    private boolean collides(Position position, MovementState state, int component) {
        Vector3i hitboxSize = state.getHitboxSize();

        int xOffset = component == X_COMPONENT ? (velocity.x > 0.0F ? -1 : 1) : 0;
        int zOffset = component == Z_COMPONENT ? (velocity.z > 0.0F ? -1 : 1) : 0;
        int startX = position.intX + Utils.floor(position.fractionX - (hitboxSize.x + xOffset) * 0.5F);
        int startY = position.intY;
        int startZ = position.intZ + Utils.floor(position.fractionZ - (hitboxSize.z + zOffset) * 0.5F);

        int width = hitboxSize.x + 1;
        int height = hitboxSize.y;
        int depth = hitboxSize.z + 1;

        return collides(startX, startY, startZ, width, height, depth);
    }

    private static boolean collides(int startX, int startY, int startZ, int width, int height, int depth) {
        World world = Game.getWorld();

        for (int x = startX; x < startX + width; x++)
            for (int y = startY; y < startY + height; y++)
                for (int z = startZ; z < startZ + depth; z++) {
                    byte material = world.getMaterial(x, y, z, 0);
                    if (Properties.doesntHaveProperties(material, NO_COLLISION)) return true;
                }
        return false;
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
                Utils.fraction(cornerFraction.x + (velocity.x > 0 ? (hitboxSize.x + 1) * 0.5F : -hitboxSize.x * 0.5F)),
                Utils.fraction(cornerFraction.y + (velocity.y > 0 ? hitboxSize.y : 0)),
                Utils.fraction(cornerFraction.z + (velocity.z > 0 ? (hitboxSize.z + 1) * 0.5F : -hitboxSize.z * 0.5F))
        );
    }

    private int getStartX(Position position, Vector3i hitboxSize, int component) {
        float offset = component == X_COMPONENT && velocity.x > 0 ? hitboxSize.x * 0.5F + 0.5F : -hitboxSize.x * 0.5F;
        return position.intX + Utils.floor(position.fractionX + offset);
    }

    private int getStartY(Position position, Vector3i hitboxSize, int component) {
        float offset = component == Y_COMPONENT && velocity.y > 0 ? hitboxSize.y : 0;
        return position.intY + Utils.floor(position.fractionY + offset);
    }

    private int getStartZ(Position position, Vector3i hitboxSize, int component) {
        float offset = component == Z_COMPONENT && velocity.z > 0 ? hitboxSize.z * 0.5F + 0.5F : -hitboxSize.z * 0.5F;
        return position.intZ + Utils.floor(position.fractionZ + offset);
    }


    private MovementState state;
    private boolean grounded;
    private final Vector3f velocity = new Vector3f();
    private Vector3f renderVelocity = new Vector3f();
}
