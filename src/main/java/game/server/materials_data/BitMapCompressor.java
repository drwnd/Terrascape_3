package game.server.materials_data;

import core.utils.ByteArrayList;

import static game.utils.Constants.*;
import static game.server.materials_data.MaterialsData.*;

final class BitMapCompressor {

    private BitMapCompressor() {

    }

    static int compressMaterials(ByteArrayList data, long[] bitMap, byte material, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        int homogeneity = getHomogeneity(inChunkX, inChunkY, inChunkZ, sizeBits, bitMap);
        if (homogeneity == FULLY_HOMOGENOUS) return addHomogenous(data, bitMap, material, inChunkX, inChunkY, inChunkZ);
        if (sizeBits <= 1) {
            int uncompressedIndex = getUncompressedIndex(inChunkX, inChunkY, inChunkZ);
            byte target = getBitMapByte(bitMap, uncompressedIndex >> 3);
            data.add((byte) (getType(material) | DETAIL));
            data.add((target & 1 << 0) == 0 ? AIR : material);
            data.add((target & 1 << 2) == 0 ? AIR : material);
            data.add((target & 1 << 1) == 0 ? AIR : material);
            data.add((target & 1 << 3) == 0 ? AIR : material);
            data.add((target & 1 << 4) == 0 ? AIR : material);
            data.add((target & 1 << 6) == 0 ? AIR : material);
            data.add((target & 1 << 5) == 0 ? AIR : material);
            data.add((target & 1 << 7) == 0 ? AIR : material);
            return DETAIL_BYTE_SIZE;
        }

        int nextSize = 1 << --sizeBits;
        int offset = SPLITTER_BYTE_SIZE, index = data.size();
        data.add(SPLITTER);
        data.pad(SPLITTER_BYTE_SIZE - 1);

        if ((homogeneity & 1) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ);
        else offset += addHomogenous(data, bitMap, material, inChunkX, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 1);

        if ((homogeneity & 2) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ + nextSize);
        else offset += addHomogenous(data, bitMap, material, inChunkX, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 4);

        if ((homogeneity & 4) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ);
        else offset += addHomogenous(data, bitMap, material, inChunkX, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 7);

        if ((homogeneity & 8) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        else offset += addHomogenous(data, bitMap, material, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 10);

        if ((homogeneity & 16) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ);
        else offset += addHomogenous(data, bitMap, material, inChunkX + nextSize, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 13);

        if ((homogeneity & 32) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        else offset += addHomogenous(data, bitMap, material, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 16);

        if ((homogeneity & 64) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        else offset += addHomogenous(data, bitMap, material, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 19);

        if ((homogeneity & 128) == 0)
            offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
        else offset += addHomogenous(data, bitMap, material, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
        data.set((byte) (getSplitterTypes(data, index) | SPLITTER), index);
        return offset;
    }


    private static int addHomogenous(ByteArrayList data, long[] bitMap, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        material = getBitMapByte(bitMap, getUncompressedIndex(inChunkX, inChunkY, inChunkZ) >> 3) == -1 ? material : AIR;
        data.add((byte) (getType(material) | HOMOGENOUS));
        data.add(material);
        return HOMOGENOUS_BYTE_SIZE;
    }


    private static int getHomogeneity(int startX, int startY, int startZ, int sizeBits, long[] bitMap) {
        int startIndex = getUncompressedIndex(startX, startY, startZ);
        if (sizeBits == 1) return isHomogenous(startIndex, 8, bitMap) ? FULLY_HOMOGENOUS : 0;

        int homogeneity = 0;
        int length = (1 << --sizeBits * 3);

        if (isHomogenous(startIndex, length, bitMap)) homogeneity |= 1;
        if (isHomogenous(startIndex += length, length, bitMap)) homogeneity |= 2;
        if (isHomogenous(startIndex += length, length, bitMap)) homogeneity |= 4;
        if (isHomogenous(startIndex += length, length, bitMap)) homogeneity |= 8;
        if (isHomogenous(startIndex += length, length, bitMap)) homogeneity |= 16;
        if (isHomogenous(startIndex += length, length, bitMap)) homogeneity |= 32;
        if (isHomogenous(startIndex += length, length, bitMap)) homogeneity |= 64;
        if (isHomogenous(startIndex + length, length, bitMap)) homogeneity |= 128;

        if (homogeneity == 255) {
            int size = (1 << sizeBits * 3) >> 3;
            startIndex = getUncompressedIndex(startX, startY, startZ);
            byte target = getBitMapByte(bitMap, startIndex >>= 3);
            if (getBitMapByte(bitMap, startIndex += size) == target
                    && getBitMapByte(bitMap, startIndex += size) == target
                    && getBitMapByte(bitMap, startIndex += size) == target
                    && getBitMapByte(bitMap, startIndex += size) == target
                    && getBitMapByte(bitMap, startIndex += size) == target
                    && getBitMapByte(bitMap, startIndex += size) == target
                    && getBitMapByte(bitMap, startIndex + size) == target) return FULLY_HOMOGENOUS;
        }
        return homogeneity;
    }


    private static boolean isHomogenous(int startIndex, int length, long[] bitMap) {
        byte target = getBitMapByte(bitMap, startIndex >> 3);
        if (target != 0 && target != -1) return false;
        int endIndex = startIndex + length;
        for (int index = startIndex + 8; index < endIndex; index += 8)
            if (getBitMapByte(bitMap, index >> 3) != target) return false;
        return true;
    }

    private static byte getBitMapByte(long[] bitMap, int byteIndex) {
        return (byte) (bitMap[byteIndex >> 3] >> (byteIndex & 7) * 8 & 0xFF);
    }
}
