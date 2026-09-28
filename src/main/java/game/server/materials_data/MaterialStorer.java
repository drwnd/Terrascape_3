package game.server.materials_data;

import core.utils.MathUtils;
import game.player.interaction.PlaceMode;
import game.player.interaction.ShapePlaceable;
import game.server.Game;
import game.settings.OptionSettings;

import static game.utils.Constants.AIR;

final class MaterialStorer {

    static void storeMaterials(int inChunkX, int inChunkY, int inChunkZ, byte[] uncompressedMaterials, int totalSizeBits, int lod, ShapePlaceable placeable) {
        if (uncompressedMaterials.length != 1 << totalSizeBits * 3)
            throw new IllegalArgumentException("Mismatch totalSizeBits and uncompressedMaterials.length");

        int inChunkAlign = Integer.numberOfTrailingZeros(inChunkX | inChunkY | inChunkZ);
        int align = MathUtils.min(totalSizeBits + lod, inChunkAlign, Integer.numberOfTrailingZeros(placeable.getPreferredSizePowOf2()));
        int alignLength = 1 << Math.max(0, align - lod);

        if (alignLength >= 1) storeMaterial1Aligned(inChunkX, inChunkY, inChunkZ, uncompressedMaterials, totalSizeBits, lod, placeable);
        else storeMaterialAnyAligned(inChunkX, inChunkY, inChunkZ, uncompressedMaterials, totalSizeBits, lod, placeable, align);
    }

    private static void storeMaterial1Aligned(int inChunkX, int inChunkY, int inChunkZ, byte[] uncompressedMaterials, int totalSizeBits, int lod, ShapePlaceable placeable) {
        byte material = placeable.getMaterial();
        long[] bitMap = placeable.getBitMap();

        int startX = Math.max(0, -inChunkX), endX = Math.clamp(placeable.getLengthX() >> lod, 1, (1 << totalSizeBits) - inChunkX);
        int startY = Math.max(0, -inChunkY), endY = Math.clamp(placeable.getLengthY() >> lod, 1, (1 << totalSizeBits) - inChunkY);
        int startZ = Math.max(0, -inChunkZ), endZ = Math.clamp(placeable.getLengthZ() >> lod, 1, (1 << totalSizeBits) - inChunkZ);

        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;
        boolean breakHeldOnly = OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY;
        byte heldMaterial = breakHeldOnly ? ((ShapePlaceable) Game.getPlayer().getHeldPlaceable()).getMaterial() : AIR;

        for (int x = startX; x < endX; x++)
            for (int y = startY; y < endY; y++)
                for (int z = startZ; z < endZ; z++) {
                    int bitmapIndex = MaterialsData.getUncompressedIndex(x << lod, y << lod, z << lod);
                    int materialIndex = MaterialsData.getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex, materialIndex, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                }
    }

    private static void storeMaterialAnyAligned(int inChunkX, int inChunkY, int inChunkZ, byte[] uncompressedMaterials, int totalSizeBits, int lod, ShapePlaceable placeable, int align) {
        byte material = placeable.getMaterial();
        long[] bitMap = placeable.getBitMap();

        int shiftCount = lod * 3, stride = 1 << shiftCount, mask = -stride;
        int alignLength = 1 << Math.max(0, align - lod), count = 1 << align * 3;
        int startX = Math.max(0, -inChunkX), endX = Math.clamp(placeable.getLengthX() >> lod, 1, (1 << totalSizeBits) - inChunkX);
        int startY = Math.max(0, -inChunkY), endY = Math.clamp(placeable.getLengthY() >> lod, 1, (1 << totalSizeBits) - inChunkY);
        int startZ = Math.max(0, -inChunkZ), endZ = Math.clamp(placeable.getLengthZ() >> lod, 1, (1 << totalSizeBits) - inChunkZ);

        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;
        boolean breakHeldOnly = OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY;
        byte heldMaterial = breakHeldOnly ? ((ShapePlaceable) Game.getPlayer().getHeldPlaceable()).getMaterial() : AIR;

        for (int x = startX; x < endX; x += alignLength)
            for (int y = startY; y < endY; y += alignLength)
                for (int z = startZ; z < endZ; z += alignLength) {
                    int materialStartIndex = MaterialsData.getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z);
                    int bitMapStartIndex = MaterialsData.getUncompressedIndex(x << lod, y << lod, z << lod);
                    int endIndex = bitMapStartIndex + count, bitMapEndIndex = Math.max(bitMapStartIndex + count >> 6, (bitMapStartIndex >> 6) + 1);

                    storeMaterial(bitMap, uncompressedMaterials,
                            bitMapStartIndex, bitMapEndIndex, mask, endIndex, stride, materialStartIndex, shiftCount,
                            paint, replaceAir, breakHeldOnly,
                            heldMaterial, material);
                }
    }

    private static void storeMaterial(long[] bitMap, byte[] uncompressedMaterials,
                                      int bitMapStartIndex, int bitMapEndIndex, int mask, int endIndex, int stride, int materialStartIndex, int shiftCount,
                                      boolean paint, boolean replaceAir, boolean breakHeldOnly,
                                      byte heldMaterial, byte material) {
        for (int bitsIndex = bitMapStartIndex >> 6; bitsIndex < bitMapEndIndex; bitsIndex++)
            for (int index = Math.max((bitsIndex << 6) + Long.numberOfTrailingZeros(bitMap[bitsIndex]) & mask, bitMapStartIndex),
                 end = Math.min(bitsIndex + 1 << 6, endIndex); index < end; index += stride) {
                int materialIndex = materialStartIndex + (index - bitMapStartIndex >> shiftCount);
                storeMaterial(uncompressedMaterials, bitMap, index, materialIndex, paint, replaceAir, breakHeldOnly, heldMaterial, material);
            }
    }

    private static void storeMaterial(byte[] uncompressedMaterials, long[] bitMap, int bitmapIndex, int materialIndex,
                                      boolean paint, boolean replaceAir, boolean breakHeldOnly,
                                      byte heldMaterial, byte material) {
        if ((bitMap[bitmapIndex >> 6] & 1L << bitmapIndex) == 0
                || paint && uncompressedMaterials[materialIndex] == AIR
                || replaceAir && uncompressedMaterials[materialIndex] != AIR
                || breakHeldOnly && uncompressedMaterials[materialIndex] != heldMaterial) return;
        uncompressedMaterials[materialIndex] = material;
    }
}
