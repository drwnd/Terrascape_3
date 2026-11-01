package game.player.interaction;

import core.utils.Vector3l;
import game.player.Player;
import game.server.Game;
import game.utils.Position;

import static game.utils.Constants.Y_COMPONENT;

public interface Target {

    Vector3l position();

    default float distanceSquaredToPlayer() {
        Player player = Game.getPlayer();
        Position origin = player.getPosition().addComponent(Y_COMPONENT, player.getMovement().getState().getCameraElevation());
        return distanceSquared(position(), origin);
    }

    static float distanceSquared(Vector3l hitPosition, Position origin) {
        float dx = (hitPosition.x - origin.longX) - origin.fractionX;
        float dy = (hitPosition.y - origin.longY) - origin.fractionY;
        float dz = (hitPosition.z - origin.longZ) - origin.fractionZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
