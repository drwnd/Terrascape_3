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
            case Rotation24Way.NORTH_1 -> isInside(outerThreshold, innerThreshold, invert - z, x, y);
            case Rotation24Way.NORTH_2 -> isInside(outerThreshold, innerThreshold, invert - z, x, invert - y);
            case Rotation24Way.NORTH_3 -> isInside(outerThreshold, innerThreshold, invert - z, invert - x, invert - y);
            case Rotation24Way.NORTH_4 -> isInside(outerThreshold, innerThreshold, invert - z, invert - x, y);

            case Rotation24Way.TOP_1 -> isInside(outerThreshold, innerThreshold, invert - y, x, z);
            case Rotation24Way.TOP_2 -> isInside(outerThreshold, innerThreshold, invert - y, x, invert - z);
            case Rotation24Way.TOP_3 -> isInside(outerThreshold, innerThreshold, invert - y, invert - x, invert - z);
            case Rotation24Way.TOP_4 -> isInside(outerThreshold, innerThreshold, invert - y, invert - x, z);

            case Rotation24Way.WEST_1 -> isInside(outerThreshold, innerThreshold, invert - x, y, z);
            case Rotation24Way.WEST_2 -> isInside(outerThreshold, innerThreshold, invert - x, y, invert - z);
            case Rotation24Way.WEST_3 -> isInside(outerThreshold, innerThreshold, invert - x, invert - y, invert - z);
            case Rotation24Way.WEST_4 -> isInside(outerThreshold, innerThreshold, invert - x, invert - y, z);

            case Rotation24Way.SOUTH_1 -> isInside(outerThreshold, innerThreshold, z, x, y);
            case Rotation24Way.SOUTH_2 -> isInside(outerThreshold, innerThreshold, z, x, invert - y);
            case Rotation24Way.SOUTH_3 -> isInside(outerThreshold, innerThreshold, z, invert - x, invert - y);
            case Rotation24Way.SOUTH_4 -> isInside(outerThreshold, innerThreshold, z, invert - x, y);

            case Rotation24Way.BOTTOM_1 -> isInside(outerThreshold, innerThreshold, y, x, z);
            case Rotation24Way.BOTTOM_2 -> isInside(outerThreshold, innerThreshold, y, x, invert - z);
            case Rotation24Way.BOTTOM_3 -> isInside(outerThreshold, innerThreshold, y, invert - x, invert - z);
            case Rotation24Way.BOTTOM_4 -> isInside(outerThreshold, innerThreshold, y, invert - x, z);

            case Rotation24Way.EAST_1 -> isInside(outerThreshold, innerThreshold, x, y, z);
            case Rotation24Way.EAST_2 -> isInside(outerThreshold, innerThreshold, x, y, invert - z);
            case Rotation24Way.EAST_3 -> isInside(outerThreshold, innerThreshold, x, invert - y, invert - z);
            case Rotation24Way.EAST_4 -> isInside(outerThreshold, innerThreshold, x, invert - y, z);

            case null, default -> false;
        };
    }

    boolean isInside(int outerThreshold, int innerThreshold, int a, int b, int c);

    Option getRotation();

}
