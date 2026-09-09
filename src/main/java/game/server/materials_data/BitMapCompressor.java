package game.server.materials_data;

import core.utils.ByteArrayList;

import static game.utils.Constants.*;
import static game.server.materials_data.MaterialsData.*;

final class BitMapCompressor {

    private BitMapCompressor() {

    }

    static void compressMaterials(ByteArrayList data, long[] bitMap, byte material, int sizeBits) {
        if (sizeBits <= 0 || bitMap.length * Long.SIZE != 1 << sizeBits * 3) {
            material = (bitMap[0] & 1) == 0 ? AIR : material;
            data.add((byte) (getType(material) | HOMOGENOUS));
            data.add(material);
            return;
        }
        compressMaterials(data, bitMap, material, sizeBits, 0, 0, 0, 0);
    }


    private static int compressMaterials(ByteArrayList data, long[] bitMap, byte material, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        if (isHomogenous(MaterialsData.getUncompressedIndex(inChunkX, inChunkY, inChunkZ), 1 << sizeBits * 3, bitMap))
            return addHomogenous(data, bitMap, material, inChunkX, inChunkY, inChunkZ);
        if (sizeBits <= 1) {
            int uncompressedIndex = getUncompressedIndex(inChunkX, inChunkY, inChunkZ);
            byte target = getBitMapByte(bitMap, uncompressedIndex >> 3);
            data.add((byte) (getType(material) | CONTAINS_TRANSPARENT | DETAIL));
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

        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 1);
        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 4);
        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 7);
        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 10);
        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 13);
        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 16);
        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 19);
        offset += compressMaterials(data, bitMap, material, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);

        data.set((byte) (getSplitterTypes(data, index) | SPLITTER), index);
        return offset;
    }

    private static int addHomogenous(ByteArrayList data, long[] bitMap, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        material = getBitMapByte(bitMap, getUncompressedIndex(inChunkX, inChunkY, inChunkZ) >> 3) == -1 ? material : AIR;
        data.add((byte) (getType(material) | HOMOGENOUS));
        data.add(material);
        return HOMOGENOUS_BYTE_SIZE;
    }

    private static boolean isHomogenous(int startIndex, int length, long[] bitMap) {
        if (length <= 8) {
            byte bitMapByte = getBitMapByte(bitMap, startIndex >> 3);
            return bitMapByte == 0 || bitMapByte == -1;
        }

        long target = bitMap[startIndex >> 6];
        if (target != 0 && target != -1) return false;
        int endIndex = startIndex + length >> 6;

        for (int index = startIndex + 64 >> 6; index < endIndex; index++)
            if (bitMap[index] != target) return false;
        return true;
    }

    private static byte getBitMapByte(long[] bitMap, int byteIndex) {
        return (byte) (bitMap[byteIndex >> 3] >> (byteIndex & 7) * 8 & 0xFF);
    }
}
