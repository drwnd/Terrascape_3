package game.server.materials_data;

import core.utils.MathUtils;
import game.player.interaction.PlaceMode;
import game.player.interaction.ShapePlaceable;
import game.server.Game;
import game.server.material.Properties;
import game.settings.OptionSettings;

import static game.utils.Constants.*;
import static game.server.materials_data.MaterialsData.*;

final class MaterialStorer {

    static void storeCuboid(byte[] uncompressedMaterials, byte material, boolean forceOverride,
                            int startX, int startY, int startZ,
                            int endX, int endY, int endZ) {
        if (material == AIR && !forceOverride) return;
        for (int x = startX; x < endX; x++)
            for (int z = startZ; z < endZ; z++)
                for (int y = startY; y < endY; y++) {
                    int targetIndex = getUncompressedIndex(x, y, z);
                    storeMaterial(uncompressedMaterials, targetIndex, material, forceOverride);
                }

        /*
                if (material == AIR && !forceOverride) return;
        int alignLength = 1;
        while (startX < endX && startY < endY && startZ < endZ) {
            if ((startX & alignLength) != 0) {
                storeXLayer(uncompressedMaterials, material, forceOverride, alignLength, startX, startY, startZ, endY, endZ);
                startX += alignLength;
                if (startX >= endX) return;
            }
            if ((endX & alignLength) != 0) {
                endX -= alignLength;
                storeXLayer(uncompressedMaterials, material, forceOverride, alignLength, endX, startY, startZ, endY, endZ);
                if (startX >= endX) return;
            }

            if ((startY & alignLength) != 0) {
                storeYLayer(uncompressedMaterials, material, forceOverride, alignLength, startY, startX, startZ, endX, endZ);
                startY += alignLength;
                if (startY >= endY) return;
            }
            if ((endY & alignLength) != 0) {
                endY -= alignLength;
                storeYLayer(uncompressedMaterials, material, forceOverride, alignLength, endY, startX, startZ, endX, endZ);
                if (startY >= endY) return;
            }

            if ((startZ & alignLength) != 0) {
                storeZLayer(uncompressedMaterials, material, forceOverride, alignLength, startZ, startX, startY, endX, endY);
                startZ += alignLength;
                if (startZ >= endZ) return;
            }
            if ((endZ & alignLength) != 0) {
                endZ -= alignLength;
                storeZLayer(uncompressedMaterials, material, forceOverride, alignLength, endZ, startX, startY, endX, endY);
                if (startZ >= endZ) return;
            }
            alignLength <<= 1;
        }
         */
    }

    private static void storeXLayer(byte[] uncompressedMaterials, byte material, boolean forceOverride, int alignLength,
                                    int x, int startY, int startZ, int endY, int endZ) {
        for (int y = startY; y < endY; y += alignLength)
            for (int z = startZ; z < endZ; z += alignLength)
                storeAlignedCube(uncompressedMaterials, material, forceOverride, alignLength, x, y, z);
    }

    private static void storeYLayer(byte[] uncompressedMaterials, byte material, boolean forceOverride, int alignLength,
                                    int y, int startX, int startZ, int endX, int endZ) {
        for (int x = startX; x < endX; x += alignLength)
            for (int z = startZ; z < endZ; z += alignLength)
                storeAlignedCube(uncompressedMaterials, material, forceOverride, alignLength, x, y, z);
    }

    private static void storeZLayer(byte[] uncompressedMaterials, byte material, boolean forceOverride, int alignLength,
                                    int z, int startX, int startY, int endX, int endY) {
        for (int x = startX; x < endX; x += alignLength)
            for (int y = startY; y < endY; y += alignLength)
                storeAlignedCube(uncompressedMaterials, material, forceOverride, alignLength, x, y, z);
    }

    private static void storeAlignedCube(byte[] uncompressedMaterials, byte material, boolean forceOverride, int length, int startX, int startY, int startZ) {
        for (int uncompressedIndex = getUncompressedIndex(startX, startY, startZ), endIndex = uncompressedIndex + length * length * length;
             uncompressedIndex < endIndex; uncompressedIndex++)
            storeMaterial(uncompressedMaterials, uncompressedIndex, material, forceOverride);
    }

    static void storeDetailSegment(byte[] uncompressedMaterials, boolean forceOverride, byte transform,
                                   int startX, int startY, int startZ,
                                   int lengthX, int lengthY, int lengthZ,
                                   byte[] data, int startIndex) {
        if (((startX | startY | startZ) & 1) == 0) {
            int uncompressedIndex = getUncompressedIndex(startX, startY, startZ);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 0, data[startIndex + getInDetailIndex(transform, 0, 0, 0)], forceOverride);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 1, data[startIndex + getInDetailIndex(transform, 0, 0, 1)], forceOverride);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 2, data[startIndex + getInDetailIndex(transform, 0, 1, 0)], forceOverride);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 3, data[startIndex + getInDetailIndex(transform, 0, 1, 1)], forceOverride);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 4, data[startIndex + getInDetailIndex(transform, 1, 0, 0)], forceOverride);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 5, data[startIndex + getInDetailIndex(transform, 1, 0, 1)], forceOverride);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 6, data[startIndex + getInDetailIndex(transform, 1, 1, 0)], forceOverride);
            storeMaterial(uncompressedMaterials, uncompressedIndex + 7, data[startIndex + getInDetailIndex(transform, 1, 1, 1)], forceOverride);
            return;
        }
        for (int x = 0; x < lengthX; x++)
            for (int z = 0; z < lengthZ; z++)
                for (int y = 0; y < lengthY; y++) {
                    int targetIndex = getUncompressedIndex(startX + x, startY + y, startZ + z);
                    storeMaterial(uncompressedMaterials, targetIndex, data[startIndex + getInDetailIndex(transform, x, y, z)], forceOverride);
                }
    }

    static void storeMaterials(int inChunkX, int inChunkY, int inChunkZ, byte[] uncompressedMaterials, int totalSizeBits, int lod, ShapePlaceable placeable) {
        if (uncompressedMaterials.length != 1 << totalSizeBits * 3)
            throw new IllegalArgumentException("Mismatch totalSizeBits and uncompressedMaterials.length");

        int inChunkAlign = Integer.numberOfTrailingZeros(inChunkX | inChunkY | inChunkZ);
        int align = MathUtils.min(totalSizeBits + lod, inChunkAlign, Integer.numberOfTrailingZeros(placeable.getPreferredSizePowOf2()));
        int alignLength = 1 << Math.max(0, align - lod);

        if (alignLength == 1) storeMaterial1Aligned(inChunkX, inChunkY, inChunkZ, uncompressedMaterials, totalSizeBits, lod, placeable);
        else if (alignLength == 2) storeMaterial2Aligned(inChunkX, inChunkY, inChunkZ, uncompressedMaterials, totalSizeBits, lod, placeable);
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

    private static void storeMaterial2Aligned(int inChunkX, int inChunkY, int inChunkZ, byte[] uncompressedMaterials, int totalSizeBits, int lod, ShapePlaceable placeable) {
        byte material = placeable.getMaterial();
        long[] bitMap = placeable.getBitMap();

        int stride = 1 << lod * 3;
        int startX = Math.max(0, -inChunkX), endX = Math.clamp(placeable.getLengthX() >> lod, 1, (1 << totalSizeBits) - inChunkX);
        int startY = Math.max(0, -inChunkY), endY = Math.clamp(placeable.getLengthY() >> lod, 1, (1 << totalSizeBits) - inChunkY);
        int startZ = Math.max(0, -inChunkZ), endZ = Math.clamp(placeable.getLengthZ() >> lod, 1, (1 << totalSizeBits) - inChunkZ);

        boolean paint = OptionSettings.PLACE_MODE.value() == PlaceMode.PAINT;
        boolean replaceAir = OptionSettings.PLACE_MODE.value() == PlaceMode.REPLACE_AIR;
        boolean breakHeldOnly = OptionSettings.PLACE_MODE.value() == PlaceMode.BREAK_HELD_ONLY;
        byte heldMaterial = breakHeldOnly ? ((ShapePlaceable) Game.getPlayer().getHeldPlaceable()).getMaterial() : AIR;

        for (int x = startX; x < endX; x += 2)
            for (int y = startY; y < endY; y += 2)
                for (int z = startZ; z < endZ; z += 2) {
                    int bitmapIndex = MaterialsData.getUncompressedIndex(x << lod, y << lod, z << lod);
                    int materialIndex = MaterialsData.getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex, materialIndex, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex += stride, materialIndex + 1, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex += stride, materialIndex + 2, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex += stride, materialIndex + 3, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex += stride, materialIndex + 4, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex += stride, materialIndex + 5, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex += stride, materialIndex + 6, paint, replaceAir, breakHeldOnly, heldMaterial, material);
                    storeMaterial(uncompressedMaterials, bitMap, bitmapIndex + stride, materialIndex + 7, paint, replaceAir, breakHeldOnly, heldMaterial, material);
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

    private static void storeMaterial(byte[] uncompressedMaterials, int materialIndex, byte material, boolean forceOverride) {
        if (!forceOverride && Properties.doesntHaveProperties(uncompressedMaterials[materialIndex], STRUCTURE_REPLACEABLE)
                && Properties.hasProperties(material, STRUCTURE_REPLACEABLE)) return;
        uncompressedMaterials[materialIndex] = material;
    }
}
