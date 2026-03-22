package game.player.interaction;

import core.utils.Saver;
import core.utils.Vector3l;

import game.player.interaction.placeable_shapes.*;
import game.server.Chunk;
import game.server.generation.Structure;

import java.util.ArrayList;

public interface Placeable {

    static void savePlaceable(Placeable placeable, Saver<?> saver) {
        if (placeable == null) saver.saveByte((byte) 0);
        else {
            saver.saveGeneric(placeable, placeable::save);
            if (placeable instanceof ShapePlaceable shapePlaceable) saver.saveBoolean(shapePlaceable.invert.value());
        }
    }

    static Placeable loadPlaceable(Saver<?> saver) {
        Placeable placeable = switch (saver.loadByte()) {
            case 1 -> CubePlaceable.load(saver);
            case 2 -> StructurePlaceable.load(saver);
            case 3 -> ChunkRebuildPlaceable.load(saver);
            case 4 -> SpherePlaceable.load(saver);
            case 5 -> CylinderPlaceable.load(saver);
            case 6 -> StairPlaceable.load(saver);
            case 7 -> SlopedStairPlaceable.load(saver);
            case 8 -> ConePlaceable.load(saver);
            default -> null;
        };
        if (placeable instanceof ShapePlaceable shapePlaceable) shapePlaceable.invert.setValue(saver.loadBoolean());
        return placeable;
    }

    void place(Vector3l position, int lod);

    ArrayList<Chunk> getAffectedChunks();

    Structure getStructure();

    boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max);

    void offsetPosition(Vector3l position);

    void spawnParticles(Vector3l position);

    void save(Placeable placeable, Saver<?> saver);
}
