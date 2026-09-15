package game.player.interaction;

import core.utils.Vector3l;

import game.player.Player;
import game.server.Game;
import game.server.material.Material;
import game.server.material.Properties;
import game.settings.IntSettings;
import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public record Target(Vector3l position, int side, byte material) {

    public Target(Target target) {
        this(new Vector3l(target.position), target.side, target.material);
    }

    public static Target getPlayerTarget() {
        Player player = Game.getPlayer();
        Position origin = player.getPosition().addComponent(Y_COMPONENT, player.getMovement().getState().getCameraElevation());
        Vector3f direction = player.getCamera().getDirection();
        return Target.getTarget(origin, direction);
    }

    public static Target getTarget(Position origin, Vector3f direction) {

        long x = origin.longX;
        long y = origin.longY;
        long z = origin.longZ;

        int xDir = direction.x < 0 ? -1 : 1;
        int yDir = direction.y < 0 ? -1 : 1;
        int zDir = direction.z < 0 ? -1 : 1;

        int xSide = direction.x < 0 ? WEST : EAST;
        int ySide = direction.y < 0 ? TOP : BOTTOM;
        int zSide = direction.z < 0 ? NORTH : SOUTH;

        double dirXSquared = direction.x * direction.x;
        double dirYSquared = direction.y * direction.y;
        double dirZSquared = direction.z * direction.z;
        double xUnit = (float) Math.sqrt(1 + (dirYSquared + dirZSquared) / dirXSquared);
        double yUnit = (float) Math.sqrt(1 + (dirXSquared + dirZSquared) / dirYSquared);
        double zUnit = (float) Math.sqrt(1 + (dirXSquared + dirYSquared) / dirZSquared);

        double lengthX = xUnit * (direction.x < 0 ? origin.fractionX : 1 - origin.fractionX);
        double lengthY = yUnit * (direction.y < 0 ? origin.fractionY : 1 - origin.fractionY);
        double lengthZ = zUnit * (direction.z < 0 ? origin.fractionZ : 1 - origin.fractionZ);
        double length = 0;

        int intersectedSide = 0;
        float reach = IntSettings.REACH.value();
        while (length < reach) {

            byte material = Game.getWorld().getMaterial(x, y, z);
            if (material == OUT_OF_WORLD) return null;

            if (Properties.doesntHaveProperties(material, NO_COLLISION))
                return new Target(new Vector3l(x, y, z), intersectedSide, material);

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

    public Vector3l offsetPosition() {
        return Utils.offsetByNormal(position(), side);
    }

    public Vector3l position() {
        return new Vector3l(
                position.x,
                position.y,
                position.z);
    }

    public String string() {
        return "Targeted Position:[X:%s, Y:%s, Z:%s], Intersected Side:%s, Targeted Material:%s".formatted(
                position.x,
                position.y,
                position.z, side, Material.getSystemName(material));
    }
}
