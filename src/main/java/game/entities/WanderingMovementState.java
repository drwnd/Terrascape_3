package game.entities;

import game.entities.movement.EntityMovementState;
import game.server.Game;
import game.utils.Position;
import org.joml.Vector3f;

public final class WanderingMovementState extends EntityMovementState {

    @Override
    protected Vector3f computeNextGameTickAcceleration(Vector3f rotation, Position lastPosition) {
        long currentTick = Game.getServer().getCurrentGameTick();

        if (currentTick - lastActionTick > timeBetweenActions) {
            lastActionTick = currentTick;
            rotation.y = (float) Math.random() * 360;
            float distance = (float) Math.sqrt(Math.random()) * searchRadius;
            targetX = lastPosition.longX + (long) (Math.sin(Math.toRadians(rotation.y)) * distance);
            targetZ = lastPosition.longZ - (long) (Math.cos(Math.toRadians(rotation.y)) * distance);
        }

        Vector3f distance = new Vector3f(targetX - lastPosition.longX, 0, targetZ - lastPosition.longZ);
        if (distance.lengthSquared() < 100) return new Vector3f();
        return distance.normalize(walkingSpeed);
    }

    private long targetX, targetZ;

    @SuppressWarnings("unused")
    private int searchRadius, timeBetweenActions;
}
