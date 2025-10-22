package game.server;

import core.utils.ByteArrayList;
import game.server.generation.Structure;
import game.utils.Utils;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class MaterialsData {

    public MaterialsData(int totalSizeBits, byte material) {
        data = new byte[]{HOMOGENOUS, material};
        this.totalSizeBits = totalSizeBits;
    }

    public MaterialsData(int totalSizeBits, byte[] data) {
        this.data = data;
        this.totalSizeBits = totalSizeBits;
    }

    public static MaterialsData getCompressedMaterials(int sizeBits, byte[] uncompressedMaterials) {
        ByteArrayList dataList = new ByteArrayList(1000);
        compressMaterials(dataList, uncompressedMaterials, sizeBits, sizeBits, 0, 0, 0, 0);
        byte[] data = new byte[dataList.size()];
        dataList.copyInto(data, 0);
        return new MaterialsData(sizeBits, data);
    }

    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        int index = 0, sizeBits = totalSizeBits;
        synchronized (this) {
            while (true) { // Scary but should be fine
                byte identifier = data[index];

                if (identifier == HOMOGENOUS) return data[index + 1];
                if (identifier == DETAIL) return data[index + getInDetailIndex(inChunkX, inChunkY, inChunkZ)];
//            if (identifier == SPLITTER)
                index += getOffset(index, inChunkX, inChunkY, inChunkZ, --sizeBits);
            }
        }
    }

    public void fillUncompressedMaterialsInto(byte[] array) {
        synchronized (this) {
            fillUncompressedMaterialsInto(array, totalSizeBits, 0, 0, 0, 0);
        }
    }

    public void fillUncompressedSideLayerInto(byte[] array, int side) {
        synchronized (this) {
            switch (side) {
                case NORTH -> fillUncompressedNorthLayer(array, totalSizeBits, 0, 0, 0);
                case TOP -> fillUncompressedTopLayer(array, totalSizeBits, 0, 0, 0);
                case WEST -> fillUncompressedWestLayer(array, totalSizeBits, 0, 0, 0);
                case SOUTH -> fillUncompressedSouthLayer(array, totalSizeBits, 0, 0, 0);
                case BOTTOM -> fillUncompressedBottomLayer(array, totalSizeBits, 0, 0, 0);
                case EAST -> fillUncompressedEastLayer(array, totalSizeBits, 0, 0, 0);
            }
        }
    }

    public void fillUncompressedMaterialsInto(byte[] array, int arraySizeBits,
                                              int destinationX, int destinationY, int destinationZ,
                                              int startX, int startY, int startZ,
                                              int lengthX, int lengthY, int lengthZ) {

        Vector3i targetStart = new Vector3i(destinationX, destinationY, destinationZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        synchronized (this) {
            fillUncompressedMaterialsInto(array, arraySizeBits, targetStart, sourceStart, size, totalSizeBits, 0, 0, 0, 0);
        }
    }

    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte material, int sideLength) {
        byte[] uncompressedMaterials = new byte[1 << totalSizeBits * 3];
        fillUncompressedMaterialsInto(uncompressedMaterials);

        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++)
                for (int y = 0; y < sideLength; y++)
                    uncompressedMaterials[getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z)] = material;

        compressIntoData(uncompressedMaterials);
    }

    public void storeStructureMaterials(int inChunkX, int inChunkY, int inChunkZ,
                                        int startX, int startY, int startZ,
                                        int lengthX, int lengthY, int lengthZ,
                                        int lod, Structure structure, byte transform) {

        byte[] uncompressedMaterials = new byte[1 << totalSizeBits * 3];
        Vector3i targetStart = new Vector3i(inChunkX, inChunkY, inChunkZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        fillUncompressedMaterialsInto(uncompressedMaterials);
        fillStructureMaterialsInto(uncompressedMaterials, structure, totalSizeBits, transform, lod, targetStart, sourceStart, size);
        compressIntoData(uncompressedMaterials);
    }

    public static void fillStructureMaterialsInto(byte[] uncompressedMaterials, Structure structure, int targetSizeBits, byte transform, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size) {
        MaterialsData source = structure.materials();
        if ((transform & Structure.MIRROR_X) != 0) sourceStart.x = sourceStart.x + (1 << source.totalSizeBits) - structure.sizeX();
        if ((transform & Structure.MIRROR_Z) != 0) sourceStart.z = sourceStart.z + (1 << source.totalSizeBits) - structure.sizeZ();
        if ((transform & Structure.ROTATE_90) != 0) sourceStart.z = sourceStart.z + (1 << source.totalSizeBits) - structure.sizeZ();

        synchronized (source) {
            if (lod == 0)
                source.fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, source.totalSizeBits, 0, 0, 0, 0);
            else
                source.fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, source.totalSizeBits, 0, 0, 0, 0);
        }
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

    public byte[] getBytes() {
        return data;
    }

    public int getTotalSizeBits() {
        return totalSizeBits;
    }


    private void compressIntoData(byte[] uncompressedMaterials) {
        ByteArrayList dataList = new ByteArrayList(1000);
        compressMaterials(dataList, uncompressedMaterials, totalSizeBits, totalSizeBits, 0, 0, 0, 0);

        byte[] data = new byte[dataList.size()];
        dataList.copyInto(data, 0);
        synchronized (this) {
            this.data = data;
        }
    }

    private void storeLowerLODChunk(Chunk chunk, byte[] uncompressedMaterials, int startX, int startY, int startZ) {
        if (chunk == null) return;

        Vector3i targetStart = new Vector3i(startX, startY, startZ);
        Vector3i sourceStart = new Vector3i(0, 0, 0);
        Vector3i size = new Vector3i(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);

        synchronized (chunk.getMaterials()) {
            chunk.getMaterials().fillUncompressedMaterialsInto(uncompressedMaterials, CHUNK_SIZE_BITS, 1, targetStart, sourceStart, size, CHUNK_SIZE_BITS, 0, 0, 0, 0);
        }
    }

    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            int size = 1 << sizeBits;
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
        int nextSize = 1 << --sizeBits;
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        fillUncompressedMaterialsInto(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
    }

    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, int targetSizeBits, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                               int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + SPLITTER_BYTE_SIZE, currentX, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 1), currentX, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 4), currentX, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 7), currentX, currentY + nextSize, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 10), currentX + nextSize, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 13), currentX + nextSize, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 16), currentX + nextSize, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 19), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
            return;
        }

        int sourceStartX = Math.max(currentX, sourceStart.x);
        int sourceStartY = Math.max(currentY, sourceStart.y);
        int sourceStartZ = Math.max(currentZ, sourceStart.z);

        int lengthX = Math.min(currentX + length, sourceStart.x + size.x) - sourceStartX;
        int lengthY = Math.min(currentY + length, sourceStart.y + size.y) - sourceStartY;
        int lengthZ = Math.min(currentZ + length, sourceStart.z + size.z) - sourceStartZ;

        int targetStartX = targetStart.x + sourceStartX - sourceStart.x;
        int targetStartY = targetStart.y + sourceStartY - sourceStart.y;
        int targetStartZ = targetStart.z + sourceStartZ - sourceStart.z;

        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            fillUncompressedHomogenousArea(uncompressedMaterials, targetSizeBits, lengthX, lengthY, lengthZ, targetStartX, targetStartY, targetStartZ, material);
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x++)
            for (int z = 0; z < lengthZ; z++)
                for (int y = 0; y < lengthY; y++) {
                    int targetIndex = getUncompressedIndex(targetSizeBits, targetStartX + x, targetStartY + y, targetStartZ + z);
                    byte material = data[startIndex + getInDetailIndex(x, y, z)];
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, int targetSizeBits, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                               int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(lod, sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + SPLITTER_BYTE_SIZE, currentX, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 1), currentX, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 4), currentX, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 7), currentX, currentY + nextSize, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 10), currentX + nextSize, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 13), currentX + nextSize, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 16), currentX + nextSize, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetSizeBits, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 19), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
            return;
        }

        int sourceStartX = Math.max(currentX, sourceStart.x);
        int sourceStartY = Math.max(currentY, sourceStart.y);
        int sourceStartZ = Math.max(currentZ, sourceStart.z);

        int lengthX = Math.min(currentX + length, sourceStart.x + size.x) - sourceStartX;
        int lengthY = Math.min(currentY + length, sourceStart.y + size.y) - sourceStartY;
        int lengthZ = Math.min(currentZ + length, sourceStart.z + size.z) - sourceStartZ;

        int targetStartX = targetStart.x + (sourceStartX - sourceStart.x >> lod);
        int targetStartY = targetStart.y + (sourceStartY - sourceStart.y >> lod);
        int targetStartZ = targetStart.z + (sourceStartZ - sourceStart.z >> lod);

        int stepSize = 1 << lod;

        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            fillUncompressedHomogenousArea(uncompressedMaterials, targetSizeBits, lod, lengthX, lengthY, lengthZ, targetStartX, targetStartY, targetStartZ, stepSize, material);
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x += stepSize)
            for (int z = 0; z < lengthZ; z += stepSize)
                for (int y = 0; y < lengthY; y += stepSize) {
                    int targetIndex = getUncompressedIndex(targetSizeBits, targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                    byte material = data[startIndex + getInDetailIndex((x >> lod), (y >> lod), (z >> lod))];
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillStructureMaterialsInto(byte[] uncompressedMaterials, int targetSizeBits, byte transform, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                            int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b000), currentX, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b001), currentX, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b010), currentX, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b011), currentX, currentY + nextSize, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b100), currentX + nextSize, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b101), currentX + nextSize, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b110), currentX + nextSize, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b111), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
            return;
        }

        int sourceStartX = Math.max(currentX, sourceStart.x);
        int sourceStartY = Math.max(currentY, sourceStart.y);
        int sourceStartZ = Math.max(currentZ, sourceStart.z);

        int lengthX = Math.min(currentX + length, sourceStart.x + size.x) - sourceStartX;
        int lengthY = Math.min(currentY + length, sourceStart.y + size.y) - sourceStartY;
        int lengthZ = Math.min(currentZ + length, sourceStart.z + size.z) - sourceStartZ;

        int targetStartX = targetStart.x + sourceStartX - sourceStart.x;
        int targetStartY = targetStart.y + sourceStartY - sourceStart.y;
        int targetStartZ = targetStart.z + sourceStartZ - sourceStart.z;

        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            if (material == AIR) return;
            fillUncompressedHomogenousArea(uncompressedMaterials, targetSizeBits, lengthX, lengthY, lengthZ, targetStartX, targetStartY, targetStartZ, material);
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x++)
            for (int z = 0; z < lengthZ; z++)
                for (int y = 0; y < lengthY; y++) {
                    int targetIndex = getUncompressedIndex(targetSizeBits, targetStartX + x, targetStartY + y, targetStartZ + z);
                    byte material = data[startIndex + getInDetailIndex(transform, x, y, z)];
                    if (material == AIR) continue;
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillStructureMaterialsInto(byte[] uncompressedMaterials, int targetSizeBits, byte transform, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                            int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(lod, sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b000), currentX, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b001), currentX, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b010), currentX, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b011), currentX, currentY + nextSize, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b100), currentX + nextSize, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b101), currentX + nextSize, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b110), currentX + nextSize, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, targetSizeBits, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b111), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
            return;
        }

        int sourceStartX = Math.max(currentX, sourceStart.x);
        int sourceStartY = Math.max(currentY, sourceStart.y);
        int sourceStartZ = Math.max(currentZ, sourceStart.z);

        int lengthX = Math.min(currentX + length, sourceStart.x + size.x) - sourceStartX;
        int lengthY = Math.min(currentY + length, sourceStart.y + size.y) - sourceStartY;
        int lengthZ = Math.min(currentZ + length, sourceStart.z + size.z) - sourceStartZ;

        int targetStartX = targetStart.x + (sourceStartX - sourceStart.x >> lod);
        int targetStartY = targetStart.y + (sourceStartY - sourceStart.y >> lod);
        int targetStartZ = targetStart.z + (sourceStartZ - sourceStart.z >> lod);

        int stepSize = 1 << lod;

        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            if (material == AIR) return;
            fillUncompressedHomogenousArea(uncompressedMaterials, targetSizeBits, lod, lengthX, lengthY, lengthZ, targetStartX, targetStartY, targetStartZ, stepSize, material);
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x += stepSize)
            for (int z = 0; z < lengthZ; z += stepSize)
                for (int y = 0; y < lengthY; y += stepSize) {
                    int targetIndex = getUncompressedIndex(targetSizeBits, targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                    byte material = data[startIndex + getInDetailIndex(transform, (x >> lod), (y >> lod), (z >> lod))];
                    if (material == AIR) continue;
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillUncompressedSouthLayer(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            fillUncompressedHomogenousLayer(uncompressedMaterials, sizeBits, startIndex, inChunkX, inChunkY);
            return;
        }
        if (identifier == DETAIL) {
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY)] = data[startIndex + 1];
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY + 1)] = data[startIndex + 2];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY)] = data[startIndex + 5];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY + 1)] = data[startIndex + 6];
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        fillUncompressedSouthLayer(uncompressedMaterials, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY);
        fillUncompressedSouthLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize);
        fillUncompressedSouthLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY);
        fillUncompressedSouthLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize);
    }

    private void fillUncompressedNorthLayer(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            fillUncompressedHomogenousLayer(uncompressedMaterials, sizeBits, startIndex, inChunkX, inChunkY);
            return;
        }
        if (identifier == DETAIL) {
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY)] = data[startIndex + 3];
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY + 1)] = data[startIndex + 4];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY)] = data[startIndex + 7];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkY + 1)] = data[startIndex + 8];
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        fillUncompressedNorthLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY);
        fillUncompressedNorthLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize);
        fillUncompressedNorthLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY);
        fillUncompressedNorthLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize);
    }

    private void fillUncompressedBottomLayer(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            fillUncompressedHomogenousLayer(uncompressedMaterials, sizeBits, startIndex, inChunkX, inChunkZ);
            return;
        }
        if (identifier == DETAIL) {
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkZ)] = data[startIndex + 1];
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkZ + 1)] = data[startIndex + 3];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkZ)] = data[startIndex + 5];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkZ + 1)] = data[startIndex + 7];
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        fillUncompressedBottomLayer(uncompressedMaterials, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkZ);
        fillUncompressedBottomLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkZ + nextSize);
        fillUncompressedBottomLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkZ);
        fillUncompressedBottomLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkZ + nextSize);
    }

    private void fillUncompressedTopLayer(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            fillUncompressedHomogenousLayer(uncompressedMaterials, sizeBits, startIndex, inChunkX, inChunkZ);
            return;
        }
        if (identifier == DETAIL) {
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkZ)] = data[startIndex + 2];
            uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkZ + 1)] = data[startIndex + 4];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkZ)] = data[startIndex + 6];
            uncompressedMaterials[getUncompressedIndex(inChunkX + 1, inChunkZ + 1)] = data[startIndex + 8];
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        fillUncompressedTopLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkZ);
        fillUncompressedTopLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkZ + nextSize);
        fillUncompressedTopLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkZ);
        fillUncompressedTopLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkZ + nextSize);
    }

    private void fillUncompressedEastLayer(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            fillUncompressedHomogenousLayer(uncompressedMaterials, sizeBits, startIndex, inChunkZ, inChunkY);
            return;
        }
        if (identifier == DETAIL) {
            uncompressedMaterials[getUncompressedIndex(inChunkZ, inChunkY)] = data[startIndex + 1];
            uncompressedMaterials[getUncompressedIndex(inChunkZ, inChunkY + 1)] = data[startIndex + 2];
            uncompressedMaterials[getUncompressedIndex(inChunkZ + 1, inChunkY)] = data[startIndex + 3];
            uncompressedMaterials[getUncompressedIndex(inChunkZ + 1, inChunkY + 1)] = data[startIndex + 4];
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        fillUncompressedEastLayer(uncompressedMaterials, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkY, inChunkZ);
        fillUncompressedEastLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 1), inChunkY, inChunkZ + nextSize);
        fillUncompressedEastLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 4), inChunkY + nextSize, inChunkZ);
        fillUncompressedEastLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 7), inChunkY + nextSize, inChunkZ + nextSize);
    }

    private void fillUncompressedWestLayer(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            fillUncompressedHomogenousLayer(uncompressedMaterials, sizeBits, startIndex, inChunkZ, inChunkY);
            return;
        }
        if (identifier == DETAIL) {
            uncompressedMaterials[getUncompressedIndex(inChunkZ, inChunkY)] = data[startIndex + 5];
            uncompressedMaterials[getUncompressedIndex(inChunkZ, inChunkY + 1)] = data[startIndex + 6];
            uncompressedMaterials[getUncompressedIndex(inChunkZ + 1, inChunkY)] = data[startIndex + 7];
            uncompressedMaterials[getUncompressedIndex(inChunkZ + 1, inChunkY + 1)] = data[startIndex + 8];
            return;
        }
//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        fillUncompressedWestLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 10), inChunkY, inChunkZ);
        fillUncompressedWestLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 13), inChunkY, inChunkZ + nextSize);
        fillUncompressedWestLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 16), inChunkY + nextSize, inChunkZ);
        fillUncompressedWestLayer(uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 19), inChunkY + nextSize, inChunkZ + nextSize);
    }

    private void fillUncompressedHomogenousLayer(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkA, int inChunkB) {
        int size = 1 << sizeBits;
        byte material = data[startIndex + 1];
        for (int a = 0; a < size; a++)
            for (int b = 0; b < size; b++)
                uncompressedMaterials[getUncompressedIndex(inChunkA + a, inChunkB + b)] = material;
    }

    private void fillUncompressedHomogenousArea(byte[] uncompressedMaterials, int targetSizeBits,
                                                int lengthX, int lengthY, int lengthZ,
                                                int targetStartX, int targetStartY, int targetStartZ,
                                                byte material) {
        for (int x = 0; x < lengthX; x++)
            for (int z = 0; z < lengthZ; z++)
                for (int y = 0; y < lengthY; y++) {
                    int targetIndex = getUncompressedIndex(targetSizeBits, targetStartX + x, targetStartY + y, targetStartZ + z);
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillUncompressedHomogenousArea(byte[] uncompressedMaterials, int targetSizeBits, int lod,
                                                int lengthX, int lengthY, int lengthZ,
                                                int targetStartX, int targetStartY, int targetStartZ,
                                                int stepSize, byte material) {
        for (int x = 0; x < lengthX; x += stepSize)
            for (int z = 0; z < lengthZ; z += stepSize)
                for (int y = 0; y < lengthY; y += stepSize) {
                    int targetIndex = getUncompressedIndex(targetSizeBits, targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private static boolean isInValidCoordinate(int lod, Vector3i sourceStart, Vector3i size, int currentX, int currentY, int currentZ, int length) {
        return Utils.min(Integer.numberOfTrailingZeros(currentX), Integer.numberOfTrailingZeros(currentY), Integer.numberOfTrailingZeros(currentZ)) < lod
                || currentX + length <= sourceStart.x || sourceStart.x + size.x <= currentX
                || currentY + length <= sourceStart.y || sourceStart.y + size.y <= currentY
                || currentZ + length <= sourceStart.z || sourceStart.z + size.z <= currentZ;
    }

    private static boolean isInValidCoordinate(Vector3i sourceStart, Vector3i size, int currentX, int currentY, int currentZ, int length) {
        return currentX + length <= sourceStart.x || sourceStart.x + size.x <= currentX
                || currentY + length <= sourceStart.y || sourceStart.y + size.y <= currentY
                || currentZ + length <= sourceStart.z || sourceStart.z + size.z <= currentZ;
    }

    private int getOffset(int index) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 | data[index + 2] & 0xFF;
    }

    private int getOffset(int startIndex, byte transform, int intend) {
        if ((transform & Structure.MIRROR_X) != 0) intend ^= 0b100;
        if ((transform & Structure.MIRROR_Z) != 0) intend ^= 0b001;
        if ((transform & Structure.ROTATE_90) != 0) intend = (~intend & 0b001) << 2 | intend & 0b010 | intend >> 2;
        if (intend == 0) return SPLITTER_BYTE_SIZE;
        return getOffset(startIndex - 2 + 3 * intend);
    }

    private int getOffset(int splitterIndex, int inChunkX, int inChunkY, int inChunkZ, int sizeBits) {
        int inSplitterIndex = getInSplitterIndex(inChunkX, inChunkY, inChunkZ, sizeBits);
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

    private static int getInDetailIndex(byte transform, int inChunkX, int inChunkY, int inChunkZ) {
        if ((transform & Structure.MIRROR_X) != 0) inChunkX = ~inChunkX;
        if ((transform & Structure.MIRROR_Z) != 0) inChunkZ = ~inChunkZ;
        if ((transform & Structure.ROTATE_90) != 0) {
            int inChunkXCopy = inChunkX;
            inChunkX = ~inChunkZ;
            inChunkZ = inChunkXCopy;
        }
        return ((inChunkX & 1) << 2 | (inChunkZ & 1) << 1 | (inChunkY & 1)) + 1;
    }

    private static int getInSplitterIndex(int inChunkX, int inChunkY, int inChunkZ, int sizeBits) {
        return 3 * ((inChunkX >> sizeBits & 1) << 2 | (inChunkY >> sizeBits & 1) << 1 | (inChunkZ >> sizeBits & 1));
    }

    private static int compressMaterials(ByteArrayList data, byte[] uncompressedMaterials, int totalSizeBits, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        if (isHomogenous(inChunkX, inChunkY, inChunkZ, sizeBits, uncompressedMaterials, totalSizeBits)) {
            data.add(HOMOGENOUS);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX, inChunkY, inChunkZ)]);
            return HOMOGENOUS_BYTE_SIZE;
        }
        if (sizeBits <= 1) {
            data.add(DETAIL);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX, inChunkY, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX, inChunkY + 1, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX, inChunkY, inChunkZ + 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX, inChunkY + 1, inChunkZ + 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX + 1, inChunkY, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX + 1, inChunkY + 1, inChunkZ)]);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX + 1, inChunkY, inChunkZ + 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(totalSizeBits, inChunkX + 1, inChunkY + 1, inChunkZ + 1)]);
            return DETAIL_BYTE_SIZE;
        }

        int nextSize = 1 << --sizeBits;
        data.add(SPLITTER);
        data.pad(SPLITTER_BYTE_SIZE - 1);
        int offset = SPLITTER_BYTE_SIZE;

        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 1);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 4);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 7);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 10);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ);
        setOffset(data, offset, startIndex + 13);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        setOffset(data, offset, startIndex + 16);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        setOffset(data, offset, startIndex + 19);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits, sizeBits, startIndex + offset, inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);

        return offset;
    }

    private static boolean isHomogenous(final int startX, final int startY, final int startZ, int sizeBits, byte[] uncompressedMaterials, int totalSizeBits) {
        final int size = 1 << sizeBits;
        byte material = uncompressedMaterials[getUncompressedIndex(totalSizeBits, startX, startY, startZ)];
        for (int inChunkX = startX; inChunkX < startX + size; inChunkX++)
            for (int inChunkZ = startZ; inChunkZ < startZ + size; inChunkZ++) {
                int xzIndex = (inChunkX << totalSizeBits | inChunkZ) << totalSizeBits;
                for (int inChunkY = startY; inChunkY < startY + size; inChunkY++)
                    if (uncompressedMaterials[xzIndex | inChunkY] != material) return false;
            }
        return true;
    }

    private static int getUncompressedIndex(int sizeBits, int x, int y, int z) {
        return (x << sizeBits | z) << sizeBits | y;
    }

    private int getUncompressedIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return (inChunkX << totalSizeBits | inChunkZ) << totalSizeBits | inChunkY;
    }

    private int getUncompressedIndex(int inChunkA, int inChunkB) {
        return inChunkA << totalSizeBits | inChunkB;
    }


    private static final byte HOMOGENOUS = 0;
    private static final byte DETAIL = 1;
    private static final byte SPLITTER = 2;

    private static final byte HOMOGENOUS_BYTE_SIZE = 2;
    private static final byte DETAIL_BYTE_SIZE = 9;
    private static final byte SPLITTER_BYTE_SIZE = 22;

    private byte[] data;
    private final int totalSizeBits;
}
