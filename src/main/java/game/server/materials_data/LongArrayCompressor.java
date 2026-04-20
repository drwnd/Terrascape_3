package game.server.materials_data;

import core.utils.ByteArrayList;

import java.nio.ByteBuffer;

import static game.server.materials_data.MaterialsData.*;

final class LongArrayCompressor {

    private LongArrayCompressor() {

    }

    static void compressMaterials(ByteArrayList data, byte[] uncompressedMaterials, int sizeBits) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(uncompressedMaterials);
        compressMaterials(data, byteBuffer, sizeBits, 0, 0, 0, 0);
    }

    private static int compressMaterials(ByteArrayList data, ByteBuffer uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        int homogeneity = getHomogeneity(inChunkX, inChunkY, inChunkZ, sizeBits, uncompressedMaterials);
        if (homogeneity == FULLY_HOMOGENOUS) return addHomogenous(data, uncompressedMaterials, inChunkX, inChunkY, inChunkZ);
        if (sizeBits <= 1) {
            long materials = uncompressedMaterials.getLong(getUncompressedIndex(inChunkX, inChunkY, inChunkZ));
            data.add((byte) (getTypes(materials) | DETAIL));
            data.add((byte) (materials >> 56));
            data.add((byte) (materials >> 40));
            data.add((byte) (materials >> 48));
            data.add((byte) (materials >> 32));
            data.add((byte) (materials >> 24));
            data.add((byte) (materials >> 8));
            data.add((byte) (materials >> 16));
            data.add((byte) materials);
            return DETAIL_BYTE_SIZE;
        }

        int nextSize = 1 << --sizeBits;
        int offset = SPLITTER_BYTE_SIZE, index = data.size();
        data.add(SPLITTER);
        data.pad(SPLITTER_BYTE_SIZE - 1);

        if ((homogeneity & 1) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 1);

        if ((homogeneity & 2) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ + nextSize);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 4);

        if ((homogeneity & 4) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 7);

        if ((homogeneity & 8) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 10);

        if ((homogeneity & 16) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX + nextSize, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 13);

        if ((homogeneity & 32) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 16);

        if ((homogeneity & 64) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 19);

        if ((homogeneity & 128) == 0)
            offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
        else offset += addHomogenous(data, uncompressedMaterials, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
        data.set((byte) (getSplitterTypes(data, index) | SPLITTER), index);
        return offset;
    }

    private static int getHomogeneity(int startX, int startY, int startZ, int sizeBits, ByteBuffer uncompressedMaterials) {
        int startIndex = getUncompressedIndex(startX, startY, startZ);
        if (sizeBits == 1) return isHomogenous(startIndex, 8, uncompressedMaterials) ? FULLY_HOMOGENOUS : 0;

        int homogeneity = 0;
        int length = (1 << --sizeBits * 3);

        if (isHomogenous(startIndex, length, uncompressedMaterials)) homogeneity |= 1;
        if (isHomogenous(startIndex += length, length, uncompressedMaterials)) homogeneity |= 2;
        if (isHomogenous(startIndex += length, length, uncompressedMaterials)) homogeneity |= 4;
        if (isHomogenous(startIndex += length, length, uncompressedMaterials)) homogeneity |= 8;
        if (isHomogenous(startIndex += length, length, uncompressedMaterials)) homogeneity |= 16;
        if (isHomogenous(startIndex += length, length, uncompressedMaterials)) homogeneity |= 32;
        if (isHomogenous(startIndex += length, length, uncompressedMaterials)) homogeneity |= 64;
        if (isHomogenous(startIndex + length, length, uncompressedMaterials)) homogeneity |= 128;

        if (homogeneity == 255) {
            int size = 1 << sizeBits * 3;
            startIndex = getUncompressedIndex(startX, startY, startZ);
            long materials = uncompressedMaterials.getLong(startIndex);
            if (uncompressedMaterials.getLong(startIndex += size) == materials
                    && uncompressedMaterials.getLong(startIndex += size) == materials
                    && uncompressedMaterials.getLong(startIndex += size) == materials
                    && uncompressedMaterials.getLong(startIndex += size) == materials
                    && uncompressedMaterials.getLong(startIndex += size) == materials
                    && uncompressedMaterials.getLong(startIndex += size) == materials
                    && uncompressedMaterials.getLong(startIndex + size) == materials) return FULLY_HOMOGENOUS;
        }
        return homogeneity;
    }

    private static boolean isHomogenous(int startIndex, int length, ByteBuffer uncompressedMaterials) {
        long target = uncompressedMaterials.getLong(startIndex);
        if (!isHomogenous(target)) return false;
        int endIndex = startIndex + length;
        for (int index = startIndex + 8; index < endIndex; index += 8)
            if (uncompressedMaterials.getLong(index) != target) return false;
        return true;
    }

    private static int addHomogenous(ByteArrayList data, ByteBuffer uncompressedMaterials, int inChunkX, int inChunkY, int inChunkZ) {
        int index = getUncompressedIndex(inChunkX, inChunkY, inChunkZ);
        byte material = (byte) (uncompressedMaterials.getLong(index) >> (index & 7) * 8);
        data.add((byte) (getType(material) | HOMOGENOUS));
        data.add(material);
        return HOMOGENOUS_BYTE_SIZE;
    }

    private static boolean isHomogenous(long materials) {
        byte material = (byte) materials;
        return (byte) (materials >> 8) == material
                && (byte) (materials >> 16) == material
                && (byte) (materials >> 24) == material
                && (byte) (materials >> 32) == material
                && (byte) (materials >> 40) == material
                && (byte) (materials >> 48) == material
                && (byte) (materials >> 56) == material;
    }

    private static int getTypes(long materials) {
        return getType((byte) materials)
                | getType((byte) (materials >> 8))
                | getType((byte) (materials >> 16))
                | getType((byte) (materials >> 24))
                | getType((byte) (materials >> 32))
                | getType((byte) (materials >> 40))
                | getType((byte) (materials >> 48))
                | getType((byte) (materials >> 56));
    }
}
