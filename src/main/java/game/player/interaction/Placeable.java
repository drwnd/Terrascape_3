package game.player.interaction;

import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import game.player.interaction.placeable_shapes.*;
import game.server.Chunk;
import game.server.generation.Structure;
import game.settings.IntSettings;

import java.util.ArrayList;

public interface Placeable {

    static void savePlaceable(Placeable placeable, Saver<?> saver) {
        if (placeable == null) saver.saveByte((byte) 0);
        else saver.saveGeneric(placeable::save);
    }

    static Placeable loadPlaceable(Saver<?> saver) {
        byte identifier = saver.loadByte();
        return switch (identifier) {
            case 0 -> null;
            case 2 -> StructurePlaceable.load(saver);
            case 3 -> ChunkRebuildPlaceable.load(saver);
            default -> ShapePlaceable.load(saver, identifier);
        };
    }

    void place(Vector3l position, int lod);

    ArrayList<Chunk> getAffectedChunks();

    Structure getStructure();

    boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max);

    void offsetPosition(Vector3l position, int targetedSide);

    void spawnParticles(Vector3l position);

    void save(Saver<?> saver);

    default int getLengthX() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    default int getLengthY() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    default int getLengthZ() {
        return 1 << IntSettings.BREAK_PLACE_SIZE.value();
    }

    default int getPreferredSizePowOf2() {
        return MathUtils.nextLargestPowOf2(getPreferredSize());
    }

    default int getPreferredSize() {
        return Math.max(getLengthX(), Math.max(getLengthY(), getLengthZ()));
    }
}
