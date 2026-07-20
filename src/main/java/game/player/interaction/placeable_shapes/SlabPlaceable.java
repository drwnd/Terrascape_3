package game.player.interaction.placeable_shapes;

import core.settings.stand_alones.StandAloneIntSetting;
import core.utils.Saver;

import game.assets.ComputeShaders;
import game.language.UiMessages;
import game.player.interaction.Rotation3Way;
import game.player.interaction.ShapePlaceable;
import game.player.interaction.ShapeSetting;
import game.server.materials_data.MaterialsData;
import game.server.generation.Structure;

public final class SlabPlaceable extends ShapePlaceable {

/**
 * Creates a new SlabPlaceable instance.
 *
 * @param material parameter
 */
    public SlabPlaceable(byte material) {
        super(ComputeShaders.SLAB, material, Rotation3Way.ROTATION_2);
        loadSettings();
    }

/**
 * Performs save.
 *
 * @param saver parameter
 */
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 13);
        saver.saveByte(getMaterial());
        saver.saveInt(thickness.value());
    }

/**
 * Performs load.
 *
 * @param saver parameter
 * @return result
 */
    public static SlabPlaceable load(Saver<?> saver) {
        SlabPlaceable placeable = new SlabPlaceable(saver.loadByte());
        placeable.thickness.setValue(saver.loadInt());
        return placeable;
    }

    @Override
    public int getLengthX() {
        return rotation() == Rotation3Way.ROTATION_1 ? thickness.value() : super.getLengthX();
    }

    @Override
    public int getLengthY() {
        return rotation() == Rotation3Way.ROTATION_2 ? thickness.value() : super.getLengthY();
    }

    @Override
    public int getLengthZ() {
        return rotation() == Rotation3Way.ROTATION_3 ? thickness.value() : super.getLengthZ();
    }

/**
 * Copies with material unique.
 *
 * @param material parameter
 * @return result
 */
    @Override
    protected ShapePlaceable copyWithMaterialUnique(byte material) {
        SlabPlaceable copy = new SlabPlaceable(material);
        copy.thickness.setValue(thickness.value());
        return copy;
    }

/**
 * Returns the settings.
 * @return array result
 */
    @Override
    protected ShapeSetting[] getSettings() {
        return new ShapeSetting[]{
                new ShapeSetting(thickness, UiMessages.WALL_THICKNESS, "thickness")
        };
    }

/**
 * Returns the small structure.
 * @return result
 */
    @Override
    public Structure getSmallStructure() {
        long[] bitMap = new long[64];
        int lengthX = rotation() == Rotation3Way.ROTATION_1 ? thickness.value() : 16;
        int lengthY = rotation() == Rotation3Way.ROTATION_2 ? thickness.value() : 16;
        int lengthZ = rotation() == Rotation3Way.ROTATION_3 ? thickness.value() : 16;
        for (int x = 0; x < lengthX; x++)
            for (int y = 0; y < lengthY; y++)
                for (int z = 0; z < lengthZ; z++) {
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
        return new Structure(4, getMaterial(), bitMap);
    }

    private final StandAloneIntSetting thickness = new StandAloneIntSetting(0, 128, 8);
}
