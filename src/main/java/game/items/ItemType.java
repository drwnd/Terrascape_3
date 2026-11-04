package game.items;

import core.utils.Saver;
import game.player.interaction.ChunkRebuildPlaceable;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.StructurePlaceable;
import game.player.interaction.StructureSelector;
import game.player.interaction.placeable_shapes.*;

public enum ItemType {
    NONE((_) -> EmptySlot.EMPTY_SLOT),
    ARC_PLACEABLE((saver) -> ShapePlaceable.load(saver, ArcPlaceable.load(saver))),
    CAPSULE_PLACEABLE((saver) -> ShapePlaceable.load(saver, CapsulePlaceable.load(saver))),
    CONE_PLACEABLE((saver) -> ShapePlaceable.load(saver, ConePlaceable.load(saver))),
    CUBE_PLACEABLE((saver) -> ShapePlaceable.load(saver, CubePlaceable.load(saver))),
    CUBOID_PLACEABLE((saver) -> ShapePlaceable.load(saver, CuboidPlaceable.load(saver))),
    CYLINDER_PLACEABLE((saver) -> ShapePlaceable.load(saver, CylinderPlaceable.load(saver))),
    ELLIPSOID_PLACEABLE((saver) -> ShapePlaceable.load(saver, EllipsoidPlaceable.load(saver))),
    INSIDE_ARC_PLACEABLE((saver) -> ShapePlaceable.load(saver, InsideArcPlaceable.load(saver))),
    INSIDE_STAIR_PLACEABLE((saver) -> ShapePlaceable.load(saver, InsideStairPlaceable.load(saver))),
    OUTSIDE_ARC_PLACEABLE((saver) -> ShapePlaceable.load(saver, OutsideArcPlaceable.load(saver))),
    OUTSIDE_STAIR_PLACEABLE((saver) -> ShapePlaceable.load(saver, OutsideStairPlaceable.load(saver))),
    SPHERE_PLACEABLE((saver) -> ShapePlaceable.load(saver, SpherePlaceable.load(saver))),
    STAIR_PLACEABLE((saver) -> ShapePlaceable.load(saver, StairPlaceable.load(saver))),
    CUSTOM_SHAPE((saver) -> ShapePlaceable.load(saver, CustomShape.load(saver))),
    CHUNK_REBUILD_PLACEABLE((_) -> new ChunkRebuildPlaceable()),
    STRUCTURE_PLACEABLE(StructurePlaceable::load),
    STRUCTURE_SELECTOR((_) -> new StructureSelector());

    ItemType(Saver.GenericLoader<Item> loader) {
        this.loader = loader;
    }

    public Item getItem(Saver<?> saver) {
        return loader.load(saver);
    }

    private final Saver.GenericLoader<Item> loader;

}
