package game.player.interaction;

import core.settings.FloatSetting;

import game.server.Game;
import game.server.Material;
import game.utils.Position;
import game.utils.Utils;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public record Target(Vector3i position, int side, byte material) {

    public static Target getPlayerTarget() {
        Position playerPosition = Game.getPlayer().getPosition();
        Vector3f playerDirection = Game.getPlayer().getCamera().getDirection();
        return Target.getTarget(playerPosition, playerDirection);
    }

    public static Target getTarget(Position origin, Vector3f dir) {

        int x = origin.intPosition().x;
        int y = origin.intPosition().y;
        int z = origin.intPosition().z;

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

        double lengthX = xUnit * (dir.x < 0 ? origin.fractionPosition().x : 1 - origin.fractionPosition().x);
        double lengthY = yUnit * (dir.y < 0 ? origin.fractionPosition().y : 1 - origin.fractionPosition().y);
        double lengthZ = zUnit * (dir.z < 0 ? origin.fractionPosition().z : 1 - origin.fractionPosition().z);
        double length = 0;

        int intersectedSide = 0;
        float reach = FloatSetting.REACH.value();
        while (length < reach) {

            byte material = Game.getWorld().getMaterial(x, y, z, 0);
            if (material == OUT_OF_WORLD) return null;

            if ((Material.getMaterialProperties(material) & NO_COLLISION) == 0)
                return new Target(new Vector3i(x, y, z), intersectedSide, material);

            if (lengthX < lengthZ && lengthX < lengthY) {
                x += xDir;
                length = lengthX;
                lengthX += xUnit;
                intersectedSide = xSide;
            } else if (lengthZ < lengthX && lengthZ < lengthY) {
                z += zDir;
                length = lengthZ;
                lengthZ += zUnit;
                intersectedSide = zSide;
            } else {
                y += yDir;
                length = lengthY;
                lengthY += yUnit;
                intersectedSide = ySide;
            }
        }
        return null;
    }


    public Vector3i offsetPosition() {
        return Utils.offsetByNormal(new Vector3i(position), side);
    }

    public String string() {
        return "Targeted Position:[X:%s, Y:%s, Z:%s], Intersected Side:%s, Targeted Material:%s".formatted(position.x, position.y, position.z, side, material);
    }
}
