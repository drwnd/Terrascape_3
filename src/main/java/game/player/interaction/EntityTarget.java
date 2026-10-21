package game.player.interaction;

import core.utils.Vector3l;

import game.entities.Entity;
import game.player.Player;
import game.server.Game;
import game.settings.IntSettings;
import game.utils.Position;

import org.joml.Vector3f;

import static game.utils.Constants.*;

public record EntityTarget(Vector3l position, Entity entity) implements Target {

    public static EntityTarget getPlayerTarget() {
        Player player = Game.getPlayer();
        Position origin = player.getPosition().addComponent(Y_COMPONENT, player.getMovement().getState().getCameraElevation());
        Vector3f direction = player.getCamera().getDirection();
        return getTarget(origin, direction, IntSettings.REACH.value());
    }

    @SuppressWarnings("MathClampMigration")
    public static EntityTarget getTarget(Position origin, Vector3f direction, float maxDistance) {
        Vector3f invDirection = new Vector3f(1).div(direction);
        EntityTarget closestTarget = null;
        float closestDistance = Float.POSITIVE_INFINITY;

        synchronized (Game.getWorld().entities) {
            for (Entity entity : Game.getWorld().entities) {
                if (entity instanceof Player) continue;
                Vector3l minPosition = entity.getMinCoordinate();
                Vector3l maxPosition = entity.getMaxCoordinate();

                float tMin = Float.NEGATIVE_INFINITY, tMax = Float.POSITIVE_INFINITY;
                if (Math.abs(direction.x) > DIRECTION_THRESHOLD) {
                    float t1 = (minPosition.x - origin.longX - origin.fractionX) * invDirection.x;
                    float t2 = (maxPosition.x - origin.longX - origin.fractionX) * invDirection.x;

                    tMin = Math.max(tMin, Math.min(t1, t2));
                    tMax = Math.min(tMax, Math.max(t1, t2));
                }
                if (Math.abs(direction.y) > DIRECTION_THRESHOLD) {
                    float t1 = (minPosition.y - origin.longY - origin.fractionY) * invDirection.y;
                    float t2 = (maxPosition.y - origin.longY - origin.fractionY) * invDirection.y;

                    tMin = Math.max(tMin, Math.min(t1, t2));
                    tMax = Math.min(tMax, Math.max(t1, t2));
                }
                if (Math.abs(direction.z) > DIRECTION_THRESHOLD) {
                    float t1 = (minPosition.z - origin.longZ - origin.fractionZ) * invDirection.z;
                    float t2 = (maxPosition.z - origin.longZ - origin.fractionZ) * invDirection.z;

                    tMin = Math.max(tMin, Math.min(t1, t2));
                    tMax = Math.min(tMax, Math.max(t1, t2));
                }

                if (tMin > tMax) continue;
                Vector3l hitPosition = new Vector3l(
                        origin.longX + (long) (origin.fractionX + tMin * direction.x),
                        origin.longY + (long) (origin.fractionY + tMin * direction.y),
                        origin.longZ + (long) (origin.fractionZ + tMin * direction.z)
                );
                float currentDistance = Target.distanceSquared(hitPosition, origin);
                if (currentDistance >= closestDistance || currentDistance > maxDistance * maxDistance) continue;

                closestDistance = currentDistance;
                closestTarget = new EntityTarget(hitPosition, entity);
            }
        }
        return closestTarget;
    }

    private static final float DIRECTION_THRESHOLD = 0.00001F;
}
