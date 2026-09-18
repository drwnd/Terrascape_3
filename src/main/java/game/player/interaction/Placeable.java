package game.player.interaction;

import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import game.player.interaction.placeable_shapes.*;
import game.server.Chunk;
import game.server.generation.Structure;
import game.settings.IntSettings;
import core.utils.MainThread;
import game.utils.ServerThread;

import java.util.ArrayList;

public interface Placeable {

    @MainThread
    static void savePlaceable(Placeable placeable, Saver<?> saver) {
        if (placeable == null) {
            saver.saveByte((byte) 0);
            return;
        }
        saver.saveGeneric(placeable::save);
        if (placeable instanceof ShapePlaceable shapePlaceable) {
            saver.saveBoolean(shapePlaceable.invert.value());
            shapePlaceable.delete();
        }
    }

    @MainThread
    static Placeable loadPlaceable(Saver<?> saver) {
        Placeable placeable = switch (saver.loadByte()) {
            case 1 -> CubePlaceable.load(saver);
            case 2 -> StructurePlaceable.load(saver);
            case 3 -> ChunkRebuildPlaceable.load();
            case 4 -> SpherePlaceable.load(saver);
            case 5 -> CylinderPlaceable.load(saver);
            case 6 -> StairPlaceable.load(saver);
            case 8 -> ConePlaceable.load(saver);
            case 9 -> InsideStairPlaceable.load(saver);
            case 10 -> OutsideStairPlaceable.load(saver);
            case 13 -> SlabPlaceable.load(saver);
            case 14 -> EllipsoidPlaceable.load(saver);
            case 15 -> ArcPlaceable.load(saver);
            case 16 -> InsideArcPlaceable.load(saver);
            case 17 -> OutsideArcPlaceable.load(saver);
            case 18 -> CustomShape.load(saver);
            case 19 -> StructureSelector.load();
            case 20 -> CapsulePlaceable.load(saver);
            default -> null;
        };
        if (placeable instanceof ShapePlaceable shapePlaceable) shapePlaceable.invert.setValue(saver.loadBoolean());
        return placeable;
    }

    @ServerThread
    void place(Vector3l position, int lod);

    @ServerThread
    ArrayList<Chunk> getAffectedChunks();

    @MainThread
    Structure getStructure();

    @ServerThread
    boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max);

    @ServerThread
    void offsetPosition(Vector3l position, int targetedSide);

    @ServerThread
    void spawnParticles(Vector3l position);

    @ServerThread
    void playSounds(Vector3l position);

    @MainThread
    void save(Saver<?> saver);

    default void rotateForwards() {
    }

    default void rotateBackwards() {
    }

    default boolean allowBreak() {
        return true;
    }

    default boolean allowPlace() {
        return true;
    }

    default boolean offsetOnPlace() {
        return true;
    }

    default boolean offsetOnBreak() {
        return false;
    }

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
