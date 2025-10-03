package game.server;

import core.utils.ByteArrayList;

import static game.utils.Constants.*;

public final class MaterialsData {

    public MaterialsData(byte material) {
        data = new byte[]{HOMOGENOUS, material};
    }

    public MaterialsData(byte[] data) {
        this.data = data;
    }

    public static MaterialsData getCompressedMaterials(byte[] uncompressedMaterials) {
        ByteArrayList data = new ByteArrayList(1000);
        compressMaterials(data, uncompressedMaterials, CHUNK_SIZE_BITS - 1, 0, 0, 0, 0);
        byte[] dataArray = new byte[data.size()];
        data.copyInto(dataArray, 0);
        return new MaterialsData(dataArray);
    }

    public static int getUncompressedIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY;
    }


    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        int index = 0, depth = CHUNK_SIZE_BITS - 1;
        synchronized (this) {
            while (true) { // Scary but should be fine
                byte identifier = data[index];

                if (identifier == HOMOGENOUS) return data[index + 1];
                if (identifier == DETAIL) return data[index + getInDetailIndex(inChunkX, inChunkY, inChunkZ)];
//            if (identifier == SPLITTER)
                index += getOffset(index, inChunkX, inChunkY, inChunkZ, depth);
                depth--;
            }
        }
    }

    public void fillUncompressedMaterialsInto(byte[] array) {
        synchronized (this) {
            fillUncompressedMaterials(array, CHUNK_SIZE_BITS - 1, 0, 0, 0, 0);
        }
    }

    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int sideLength) {
        byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
        fillUncompressedMaterialsInto(uncompressedMaterials);

        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++)
                for (int y = 0; y < sideLength; y++)
                    uncompressedMaterials[getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z)] = material;

        compressIntoData(uncompressedMaterials);
    }

    public void storeLowerLODChunks(Chunk chunk0, Chunk chunk1, Chunk chunk2, Chunk chunk3,
                                    Chunk chunk4, Chunk chunk5, Chunk chunk6, Chunk chunk7) {

        byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];
        fillUncompressedMaterialsInto(uncompressedMaterials);

        storeLowerLODChunk(chunk0, uncompressedMaterials, 0, 0, 0);
        storeLowerLODChunk(chunk1, uncompressedMaterials, 0, 0, CHUNK_SIZE / 2);
        storeLowerLODChunk(chunk2, uncompressedMaterials, 0, CHUNK_SIZE / 2, 0);
        storeLowerLODChunk(chunk3, uncompressedMaterials, 0, CHUNK_SIZE / 2, CHUNK_SIZE / 2);
        storeLowerLODChunk(chunk4, uncompressedMaterials, CHUNK_SIZE / 2, 0, 0);
        storeLowerLODChunk(chunk5, uncompressedMaterials, CHUNK_SIZE / 2, 0, CHUNK_SIZE / 2);
        storeLowerLODChunk(chunk6, uncompressedMaterials, CHUNK_SIZE / 2, CHUNK_SIZE / 2, 0);
        storeLowerLODChunk(chunk7, uncompressedMaterials, CHUNK_SIZE / 2, CHUNK_SIZE / 2, CHUNK_SIZE / 2);

        compressIntoData(uncompressedMaterials);
    }

    public int getByteSize() {
        return data.length;
    }

    public byte[] getBytes() {
        return data;
    }


    private void compressIntoData(byte[] uncompressedMaterials) {
        ByteArrayList dataList = new ByteArrayList(1000);
        compressMaterials(dataList, uncompressedMaterials, CHUNK_SIZE_BITS - 1, 0, 0, 0, 0);

        byte[] dataArray = new byte[dataList.size()];
        dataList.copyInto(dataArray, 0);
        synchronized (this) {
            data = dataArray;
        }
    }

    private void storeLowerLODChunk(Chunk chunk, byte[] uncompressedMaterials, int startX, int startY, int startZ) {
        if (chunk == null) return;
        for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX += 2)
            for (int inChunkY = 0; inChunkY < CHUNK_SIZE; inChunkY += 2)
                for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ += 2) {
                    byte material = chunk.getSaveMaterial(inChunkX, inChunkY, inChunkZ);

                    int thisChunkInChunkX = (inChunkX >> 1) + startX;
                    int thisChunkInChunkY = (inChunkY >> 1) + startY;
                    int thisChunkInChunkZ = (inChunkZ >> 1) + startZ;
                    uncompressedMaterials[getUncompressedIndex(thisChunkInChunkX, thisChunkInChunkY, thisChunkInChunkZ)] = material;
                }
    }

    private void fillUncompressedMaterials(byte[] uncompressedMaterials, int depth, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            int size = 1 << depth + 1;
            byte material = data[startIndex + 1];
            for (int x = 0; x < size; x++)
                for (int z = 0; z < size; z++)
                    for (int y = 0; y < size; y++)
                        uncompressedMaterials[getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z)] = material;
            return;
        }
        if (identifier == DETAIL) {
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)] = data[startIndex + 1];
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY + 1, inChunkZ)] = data[startIndex + 2];
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ + 1)] = data[startIndex + 3];
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY + 1, inChunkZ + 1)] = data[startIndex + 4];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY, inChunkZ)] = data[startIndex + 5];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY + 1, inChunkZ)] = data[startIndex + 6];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY, inChunkZ + 1)] = data[startIndex + 7];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY + 1, inChunkZ + 1)] = data[startIndex + 8];
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << depth;
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        fillUncompressedMaterials(uncompressedMaterials, depth - 1, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
    }

    private int getOffset(int index) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 | data[index + 2] & 0xFF;
    }

    private int getOffset(int splitterIndex, int inChunkX, int inChunkY, int inChunkZ, int depth) {
        int inSplitterIndex = getInSplitterIndex(inChunkX, inChunkY, inChunkZ, depth);
        if (inSplitterIndex == 0) return SPLITTER_BYTE_SIZE;
        return getOffset(splitterIndex + inSplitterIndex - 2);
    }


    private static void setOffset(ByteArrayList data, int offset, int index) {
        data.set((byte) (offset >> 16 & 0xFF), index);
        data.set((byte) (offset >> 8 & 0xFF), index + 1);
        data.set((byte) (offset & 0xFF), index + 2);
    }

    private static int getInDetailIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return ((inChunkX & 1) << 2 | (inChunkZ & 1) << 1 | (inChunkY & 1)) + 1;
    }

    private static int getInSplitterIndex(int inChunkX, int inChunkY, int inChunkZ, int depth) {
        return 3 * ((inChunkX >> depth & 1) << 2 | (inChunkY >> depth & 1) << 1 | (inChunkZ >> depth & 1));
    }

    private static int compressMaterials(ByteArrayList data, byte[] uncompressedMaterials, int depth, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        if (isHomogenous(inChunkX, inChunkY, inChunkZ, depth, uncompressedMaterials)) {
            data.add(HOMOGENOUS);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)]);
            return HOMOGENOUS_BYTE_SIZE;
        }
        if (depth < 1) {
            data.add(DETAIL);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY + 1, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ + 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY + 1, inChunkZ + 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY + 1, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY, inChunkZ + 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY + 1, inChunkZ + 1)]);
            return DETAIL_BYTE_SIZE;
        }
        int nextSize = 1 << depth;
        data.add(SPLITTER);
        data.pad(SPLITTER_BYTE_SIZE - 1);
        int offset = SPLITTER_BYTE_SIZE;

        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 1);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 4);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 7);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 10);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 13);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 16);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 19);
        offset += compressMaterials(data, uncompressedMaterials, depth - 1, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);

        return offset;
    }

    private static boolean isHomogenous(final int startX, final int startY, final int startZ, int depth, byte[] uncompressedMaterials) {
        final int size = 1 << depth + 1;
        byte material = uncompressedMaterials[getUncompressedIndex(startX, startY, startZ)];
        for (int inChunkX = startX; inChunkX < startX + size; inChunkX++)
            for (int inChunkZ = startZ; inChunkZ < startZ + size; inChunkZ++)
                for (int inChunkY = startY; inChunkY < startY + size; inChunkY++)
                    if (uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)] != material) return false;
        return true;
    }


    private static final byte HOMOGENOUS = 0;
    private static final byte DETAIL = 1;
    private static final byte SPLITTER = 2;

    private static final byte HOMOGENOUS_BYTE_SIZE = 2;
    private static final byte DETAIL_BYTE_SIZE = 9;
    private static final byte SPLITTER_BYTE_SIZE = 22;

    private byte[] data;
}
