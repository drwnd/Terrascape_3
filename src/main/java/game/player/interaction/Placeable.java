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

    /**
     * Saves a placeable to a saver.
     *
     * @param placeable the placeable to save
     * @param saver     the saver to use
     */
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

    /**
     * Loads a placeable from a saver.
     *
     * @param saver the saver to load from
     * @return the loaded placeable
     */
    static Placeable loadPlaceable(Saver<?> saver) {
        Placeable placeable = switch (saver.loadByte()) {
            case 1 -> CubePlaceable.load(saver);
            case 2 -> StructurePlaceable.load(saver);
            case 3 -> ChunkRebuildPlaceable.load(saver);
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
            case 19 -> StructureSelector.load(saver);
            default -> null;
        };
        if (placeable instanceof ShapePlaceable shapePlaceable) shapePlaceable.invert.setValue(saver.loadBoolean());
        return placeable;
    }

    /**
     * Places the placeable in the world at the specified position and LOD.
     *
     * @param position the world block coordinates at the specified LOD
     * @param lod      the Level of Detail
     */
    void place(Vector3l position, int lod);

    /**
     * Returns a list of chunks affected by this placeable.
     *
     * @return an {@code ArrayList} of affected {@code Chunk} objects
     */
    ArrayList<Chunk> getAffectedChunks();

    Structure getStructure();

    /**
     * Checks if the placeable at the given position intersects with an Axis-Aligned Bounding Box.
     *
     * @param position the world block coordinates (LOD 0) of the placeable's origin
     * @param min      the minimum world block coordinates (LOD 0) of the AABB
     * @param max      the maximum world block coordinates (LOD 0) of the AABB
     * @return {@code true} if it intersects, {@code false} otherwise
     */
    boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max);

    /**
     * Offsets the given position based on the targeted side and the placeable's properties.
     *
     * @param position     the world block coordinates (LOD 0) to offset
     * @param targetedSide the side being targeted
     */
    void offsetPosition(Vector3l position, int targetedSide);

    /**
     * Spawns particles at the specified position when the placeable is used.
     *
     * @param position the world block coordinates (LOD 0) where particles should spawn
     */
    void spawnParticles(Vector3l position);

    /**
     * Saves the placeable's state to a saver.
     *
     * @param saver the saver to use
     */
    void save(Saver<?> saver);

    default void rotateForwards() {
    }

    default void rotateBackwards() {
    }

    default boolean allowBreak() {
        return true;
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
