package game.player.interaction;

import game.server.Game;
import game.server.material.Material;
import game.server.material.Properties;
import game.settings.IntSettings;
import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public record Target(Position position, int side, byte material) {

    public Target(Target target) {
        this(new Position(target.position), target.side, target.material);
    }

    public static Target getPlayerTarget() {
        Position playerPosition = Game.getPlayer().getCamera().getPosition();
        Vector3f playerDirection = Game.getPlayer().getCamera().getDirection();
        return Target.getTarget(playerPosition, playerDirection, IntSettings.REACH.value());
    }

    public static Target getTarget(Position origin, Vector3f dir, float maxLength) {

        long x = origin.longX;
        long y = origin.longY;
        long z = origin.longZ;

        int xDir = dir.x < 0 ? -1 : 1;
        int yDir = dir.y < 0 ? -1 : 1;
        int zDir = dir.z < 0 ? -1 : 1;

        int xSide = dir.x < 0 ? WEST : EAST;
        int ySide = dir.y < 0 ? TOP : BOTTOM;
        int zSide = dir.z < 0 ? NORTH : SOUTH;

        double dirXSquared = dir.x * dir.x;
        double dirYSquared = dir.y * dir.y;
        double dirZSquared = dir.z * dir.z;
        double xUnit = (float) Math.sqrt(1 + (dirYSquared + dirZSquared) / dirXSquared);
        double yUnit = (float) Math.sqrt(1 + (dirXSquared + dirZSquared) / dirYSquared);
        double zUnit = (float) Math.sqrt(1 + (dirXSquared + dirYSquared) / dirZSquared);

        double lengthX = xUnit * (dir.x < 0 ? origin.fractionX : 1 - origin.fractionX);
        double lengthY = yUnit * (dir.y < 0 ? origin.fractionY : 1 - origin.fractionY);
        double lengthZ = zUnit * (dir.z < 0 ? origin.fractionZ : 1 - origin.fractionZ);
        double length = 0;

        int intersectedSide = 0;
        while (length < maxLength) {

            byte material = Game.getWorld().getMaterial(x, y, z, 0);
            if (material == OUT_OF_WORLD) return null;

            if (Properties.doesntHaveProperties(material, NO_COLLISION))
                return new Target(new Position(x, y, z, origin.fractionX, origin.fractionY, origin.fractionZ), intersectedSide, material);

            if (lengthX < lengthZ && lengthX < lengthY) {
                x = x + xDir;
                length = lengthX;
                lengthX += xUnit;
                intersectedSide = xSide;
            } else if (lengthZ < lengthX && lengthZ < lengthY) {
                z = z + zDir;
                length = lengthZ;
                lengthZ += zUnit;
                intersectedSide = zSide;
            } else {
                y = y + yDir;
                length = lengthY;
                lengthY += yUnit;
                intersectedSide = ySide;
            }
        }
        return null;
    }

    public void shiftPosition(Vector3i movement) {
        position.add(movement.x, movement.y, movement.z);
    }

    public Position offsetPosition() {
        return Utils.offsetByNormal(position(), side);
    }

    public Position position() {
        return new Position(position);
    }

    public String string() {
        return "Targeted Position:[X:%s, Y:%s, Z:%s], Intersected Side:%s, Targeted Material:%s".formatted(
                position.longX,
                position.longY,
                position.longZ, side, Material.getSystemName(material));
    }
}
