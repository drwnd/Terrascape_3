package game.player.interaction;

import core.utils.Vector3l;

import game.entities.Entity;
import game.player.Player;
import game.server.Game;
import game.settings.IntSettings;
import game.utils.Position;

import org.joml.Vector3f;

public record EntityTarget(Vector3l position, Entity entity) implements Target {

    public static EntityTarget getPlayerTarget() {
        Player player = Game.getPlayer();
        Position origin = player.getEyePosition();
        Vector3f direction = player.getCamera().getDirection();
        return getTarget(origin, direction, IntSettings.REACH.value());
    }

    public static EntityTarget getTarget(Position origin, Vector3f direction, float maxDistance) {
        EntityTarget closestTarget = null;
        Vector3f invDirection = new Vector3f(1).div(direction);

        synchronized (Game.getWorld().entities) {
            for (Entity entity : Game.getWorld().entities)
                closestTarget = getClosestTarget(origin, direction, invDirection, maxDistance, entity, closestTarget);
        }
        return closestTarget;
    }


    private static EntityTarget getClosestTarget(Position origin, Vector3f direction, Vector3f invDirection, float maxDistance, Entity entity, EntityTarget closestTarget) {
        if (entity instanceof Player) return closestTarget;
        Vector3l minPosition = entity.getMinCoordinate();
        Vector3l maxPosition = entity.getMaxCoordinate();

        float tMin = getIntersectionTMin(direction, invDirection, origin, minPosition, maxPosition);

        if (Float.isNaN(tMin)) return closestTarget;
        Vector3l hitPosition = new Vector3l(
                origin.longX + (long) (origin.fractionX + tMin * direction.x),
                origin.longY + (long) (origin.fractionY + tMin * direction.y),
                origin.longZ + (long) (origin.fractionZ + tMin * direction.z)
        );
        float currentDistance = Target.distanceSquared(hitPosition, origin);
        if (currentDistance > maxDistance * maxDistance || closestTarget != null && currentDistance >= closestTarget.distanceSquaredTo(origin))
            return closestTarget;

        closestTarget = new EntityTarget(hitPosition, entity);
        return closestTarget;
    }

    @SuppressWarnings("MathClampMigration")
    private static float getIntersectionTMin(Vector3f direction, Vector3f invDirection, Position origin, Vector3l minPosition, Vector3l maxPosition) {
        float tMin = Float.NEGATIVE_INFINITY, tMax = Float.POSITIVE_INFINITY;
        if (Math.abs(direction.x) > DIRECTION_THRESHOLD) {
            float t1 = (minPosition.x - origin.longX - origin.fractionX) * invDirection.x;
            float t2 = (maxPosition.x - origin.longX - origin.fractionX) * invDirection.x;
            if (Math.max(t1, t2) < 0) return Float.NaN;

            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }
        if (Math.abs(direction.y) > DIRECTION_THRESHOLD) {
            float t1 = (minPosition.y - origin.longY - origin.fractionY) * invDirection.y;
            float t2 = (maxPosition.y - origin.longY - origin.fractionY) * invDirection.y;
            if (Math.max(t1, t2) < 0) return Float.NaN;

            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }
        if (Math.abs(direction.z) > DIRECTION_THRESHOLD) {
            float t1 = (minPosition.z - origin.longZ - origin.fractionZ) * invDirection.z;
            float t2 = (maxPosition.z - origin.longZ - origin.fractionZ) * invDirection.z;
            if (Math.max(t1, t2) < 0) return Float.NaN;

            tMin = Math.max(tMin, Math.min(t1, t2));
            tMax = Math.min(tMax, Math.max(t1, t2));
        }
        return tMin < tMax ? tMin : Float.NaN;
    }

    private float distanceSquaredTo(Position position) {
        return Target.distanceSquared(this.position, position);
    }

    private static final float DIRECTION_THRESHOLD = 0.00001F;
}
