package game.player.interaction.placeable_shapes;

import core.settings.optionSettings.Option;
import game.player.interaction.Rotation24Way;
import game.server.MaterialsData;

interface ConerStairPlaceable  {

    default void fillBitMap(long[] bitMap, int sideLength, int outerThreshold, int innerThreshold) {
        for (int x = 0; x < sideLength; x++)
            for (int y = 0; y < sideLength; y++)
                for (int z = 0; z < sideLength; z++) {
                    if (!isInside(x, y, z, sideLength, outerThreshold, innerThreshold)) continue;
                    int bitMapIndex = MaterialsData.getUncompressedIndex(x, y, z);
                    bitMap[bitMapIndex >> 6] |= 1L << bitMapIndex;
                }
    }

    default boolean isInside(int x, int y, int z, int sideLength, int outerThreshold, int innerThreshold) {
        int invert = sideLength - 1;
        return switch (getRotation()) {
            case Rotation24Way.ROTATION_01 -> isInside(outerThreshold, innerThreshold, invert - z, x, y);
            case Rotation24Way.ROTATION_02 -> isInside(outerThreshold, innerThreshold, invert - z, x, invert - y);
            case Rotation24Way.ROTATION_03 -> isInside(outerThreshold, innerThreshold, invert - z, invert - x, invert - y);
            case Rotation24Way.ROTATION_04 -> isInside(outerThreshold, innerThreshold, invert - z, invert - x, y);

            case Rotation24Way.ROTATION_05 -> isInside(outerThreshold, innerThreshold, invert - y, x, z);
            case Rotation24Way.ROTATION_06 -> isInside(outerThreshold, innerThreshold, invert - y, x, invert - z);
            case Rotation24Way.ROTATION_07 -> isInside(outerThreshold, innerThreshold, invert - y, invert - x, invert - z);
            case Rotation24Way.ROTATION_08 -> isInside(outerThreshold, innerThreshold, invert - y, invert - x, z);

            case Rotation24Way.ROTATION_09 -> isInside(outerThreshold, innerThreshold, invert - x, y, z);
            case Rotation24Way.ROTATION_10 -> isInside(outerThreshold, innerThreshold, invert - x, y, invert - z);
            case Rotation24Way.ROTATION_11 -> isInside(outerThreshold, innerThreshold, invert - x, invert - y, invert - z);
            case Rotation24Way.ROTATION_12 -> isInside(outerThreshold, innerThreshold, invert - x, invert - y, z);

            case Rotation24Way.ROTATION_13 -> isInside(outerThreshold, innerThreshold, z, x, y);
            case Rotation24Way.ROTATION_14 -> isInside(outerThreshold, innerThreshold, z, x, invert - y);
            case Rotation24Way.ROTATION_15 -> isInside(outerThreshold, innerThreshold, z, invert - x, invert - y);
            case Rotation24Way.ROTATION_16 -> isInside(outerThreshold, innerThreshold, z, invert - x, y);

            case Rotation24Way.ROTATION_17 -> isInside(outerThreshold, innerThreshold, y, x, z);
            case Rotation24Way.ROTATION_18 -> isInside(outerThreshold, innerThreshold, y, x, invert - z);
            case Rotation24Way.ROTATION_19 -> isInside(outerThreshold, innerThreshold, y, invert - x, invert - z);
            case Rotation24Way.ROTATION_20 -> isInside(outerThreshold, innerThreshold, y, invert - x, z);

            case Rotation24Way.ROTATION_21 -> isInside(outerThreshold, innerThreshold, x, y, z);
            case Rotation24Way.ROTATION_22 -> isInside(outerThreshold, innerThreshold, x, y, invert - z);
            case Rotation24Way.ROTATION_23 -> isInside(outerThreshold, innerThreshold, x, invert - y, invert - z);
            case Rotation24Way.ROTATION_24 -> isInside(outerThreshold, innerThreshold, x, invert - y, z);

            case null, default -> false;
        };
    }

    boolean isInside(int outerThreshold, int innerThreshold, int a, int b, int c);

    Option getRotation();

}
