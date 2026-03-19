package game.player.interaction;

import core.settings.Setting;
import core.utils.Vector3l;

import game.server.Game;

import java.util.List;

public abstract class ShapePlaceable implements Placeable {

    ShapePlaceable() {
        size = 1 << Game.getPlayer().getInteractionHandler().getPlaceBreakSize();
    }

    public int getSize() {
        return size;
    }

    public abstract List<Setting> settings();

    public abstract long[] getBitMap();

    @Override
    public void offsetPosition(Vector3l position) {
        int mask = -size;
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    final int size;
}
