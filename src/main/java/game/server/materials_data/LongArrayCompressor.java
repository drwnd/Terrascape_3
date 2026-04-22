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
        if (isHomogenous(MaterialsData.getUncompressedIndex(inChunkX, inChunkY, inChunkZ), 1 << sizeBits * 3, uncompressedMaterials))
            return addHomogenous(data, uncompressedMaterials, inChunkX, inChunkY, inChunkZ);
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

        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 1);
        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 4);
        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 7);
        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 10);
        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 13);
        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 16);
        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 19);
        offset += compressMaterials(data, uncompressedMaterials, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);

        data.set((byte) (getSplitterTypes(data, index) | SPLITTER), index);
        return offset;
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
