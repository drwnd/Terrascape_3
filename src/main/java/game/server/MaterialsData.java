package game.server;

import core.utils.ByteArrayList;

import game.player.rendering.MeshGenerator;
import game.server.generation.Structure;
import game.server.material.Material;
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


    public static int getUncompressedIndex(int inChunkX, int inChunkY, int inChunkZ) {
//        return (inChunkX << totalSizeBits | inChunkZ) << totalSizeBits | inChunkY;
        return Z_ORDER_3D_TABLE_X[inChunkX] | Z_ORDER_3D_TABLE_Y[inChunkY] | T_ORDER_3D_TABLE_Z[inChunkZ];
    }


    public static MaterialsData getCompressedMaterials(int sizeBits, byte[] uncompressedMaterials) {
        ByteArrayList dataList = new ByteArrayList(1000);
        compressMaterials(dataList, uncompressedMaterials, sizeBits);
        byte[] data = new byte[dataList.size()];
        dataList.copyInto(data, 0);
        return new MaterialsData(sizeBits, data);
    }

    public static void fillStructureMaterialsInto(byte[] uncompressedMaterials, Structure structure, byte transform, int lod,
                                                  Vector3i targetStart, Vector3i sourceStart, Vector3i size) {
        MaterialsData source = structure.materials();
        if ((transform & Structure.MIRROR_X) != 0) sourceStart.x = sourceStart.x + (1 << source.totalSizeBits) - structure.sizeX(transform);
        if (((transform & Structure.MIRROR_Z) == 0) == ((transform & Structure.ROTATE_90) != 0))
            sourceStart.z = sourceStart.z + (1 << source.totalSizeBits) - structure.sizeZ(transform);

        synchronized (source) {
            if (lod == 0)
                source.fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, source.totalSizeBits, 0, 0, 0, 0);
            else
                source.fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, source.totalSizeBits, 0, 0, 0, 0);
        }
    }

    public static void fillMaterialsInto(byte[] uncompressedMaterials, Structure structure, int lod,
                                         Vector3i targetStart, Vector3i sourceStart, Vector3i size) {
        MaterialsData source = structure.materials();
        synchronized (source) {
            source.fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, source.totalSizeBits, 0, 0, 0, 0);
        }
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

    public void fillSideLayerInto(ByteArrayList materials, int side) {
        synchronized (this) {
            switch (side) {
                case NORTH -> fillNorthLayerInto(materials, 0);
                case TOP -> fillTopLayerInto(materials, 0);
                case WEST -> fillWestLayerInto(materials, 0);
                case SOUTH -> fillSouthLayerInto(materials, 0);
                case BOTTOM -> fillBottomLayerInto(materials, 0);
                case EAST -> fillEastLayerInto(materials, 0);
            }
        }
    }

    public void fillUncompressedMaterialsInto(byte[] array,
                                              int destinationX, int destinationY, int destinationZ,
                                              int startX, int startY, int startZ,
                                              int lengthX, int lengthY, int lengthZ) {

        Vector3i targetStart = new Vector3i(destinationX, destinationY, destinationZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        synchronized (this) {
            fillUncompressedMaterialsInto(array, targetStart, sourceStart, size, totalSizeBits, 0, 0, 0, 0);
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
        fillStructureMaterialsInto(uncompressedMaterials, structure, transform, lod, targetStart, sourceStart, size);
        compressIntoData(uncompressedMaterials);
    }

    public void storeMaterials(int inChunkX, int inChunkY, int inChunkZ,
                               int startX, int startY, int startZ,
                               int lengthX, int lengthY, int lengthZ,
                               int lod, Structure structure) {
        byte[] uncompressedMaterials = new byte[1 << totalSizeBits * 3];
        Vector3i targetStart = new Vector3i(inChunkX, inChunkY, inChunkZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        fillUncompressedMaterialsInto(uncompressedMaterials);
        fillMaterialsInto(uncompressedMaterials, structure, lod, targetStart, sourceStart, size);
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

    public void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, byte[][] adjacentChunkLayers) {
        synchronized (this) {
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, totalSizeBits, 0, 0, 0, 0);
        }
    }

    public void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, ByteArrayList[] adjacentChunkLayers, int chunkX, int chunkY, int chunkZ) {
        int startIndex = startIndexOf(chunkX << CHUNK_SIZE_BITS, chunkY << CHUNK_SIZE_BITS, chunkZ << CHUNK_SIZE_BITS, CHUNK_SIZE_BITS);
        byte[][] adjacentChunkLayersData = {
                adjacentChunkLayers[NORTH].getData(), adjacentChunkLayers[TOP].getData(), adjacentChunkLayers[WEST].getData(),
                adjacentChunkLayers[SOUTH].getData(), adjacentChunkLayers[BOTTOM].getData(), adjacentChunkLayers[EAST].getData()};
        synchronized (this) {
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayersData, Math.min(CHUNK_SIZE_BITS, totalSizeBits), startIndex, 0, 0, 0);
        }
    }

    public byte[] getBytes() {
        return data;
    }

    public int getTotalSizeBits() {
        return totalSizeBits;
    }

    public boolean isHomogenous(byte material) {
        return data[0] == HOMOGENOUS && data[1] == material;
    }


    private void compressIntoData(byte[] uncompressedMaterials) {
        ByteArrayList dataList = new ByteArrayList(1000);
        compressMaterials(dataList, uncompressedMaterials, totalSizeBits);

        byte[] data = new byte[dataList.size()];
        dataList.copyInto(data, 0);
        synchronized (this) {
            this.data = data;
        }
    }

    private static void storeLowerLODChunk(Chunk chunk, byte[] uncompressedMaterials, int startX, int startY, int startZ) {
        if (chunk == null) return;

        Vector3i targetStart = new Vector3i(startX, startY, startZ);
        Vector3i sourceStart = new Vector3i(0, 0, 0);
        Vector3i size = new Vector3i(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);

        synchronized (chunk.getMaterials()) {
            chunk.getMaterials().fillUncompressedMaterialsInto(uncompressedMaterials, 1, targetStart, sourceStart, size, CHUNK_SIZE_BITS, 0, 0, 0, 0);
        }
    }

    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == HOMOGENOUS) {
            int size = 1 << sizeBits * 3;
            byte material = data[startIndex + 1];
            startIndex = getUncompressedIndex(inChunkX, inChunkY, inChunkZ);
            for (int index = startIndex; index < startIndex + size; index++) uncompressedMaterials[index] = material;
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

    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                               int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + SPLITTER_BYTE_SIZE, currentX, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 1), currentX, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 4), currentX, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 7), currentX, currentY + nextSize, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 10), currentX + nextSize, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 13), currentX + nextSize, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 16), currentX + nextSize, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 19), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
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
            for (int x = 0; x < lengthX; x++)
                for (int z = 0; z < lengthZ; z++)
                    for (int y = 0; y < lengthY; y++) {
                        int targetIndex = getUncompressedIndex(targetStartX + x, targetStartY + y, targetStartZ + z);
                        uncompressedMaterials[targetIndex] = material;
                    }
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x++)
            for (int z = 0; z < lengthZ; z++)
                for (int y = 0; y < lengthY; y++) {
                    int targetIndex = getUncompressedIndex(targetStartX + x, targetStartY + y, targetStartZ + z);
                    byte material = data[startIndex + getInDetailIndex(x, y, z)];
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                               int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(lod, sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + SPLITTER_BYTE_SIZE, currentX, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 1), currentX, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 4), currentX, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 7), currentX, currentY + nextSize, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 10), currentX + nextSize, currentY, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 13), currentX + nextSize, currentY, currentZ + nextSize);
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 16), currentX + nextSize, currentY + nextSize, currentZ);
            fillUncompressedMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 19), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
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
            for (int x = 0; x < lengthX; x += stepSize)
                for (int z = 0; z < lengthZ; z += stepSize)
                    for (int y = 0; y < lengthY; y += stepSize) {
                        int targetIndex = getUncompressedIndex(targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                        uncompressedMaterials[targetIndex] = material;
                    }
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x += stepSize)
            for (int z = 0; z < lengthZ; z += stepSize)
                for (int y = 0; y < lengthY; y += stepSize) {
                    int targetIndex = getUncompressedIndex(targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                    byte material = data[startIndex + getInDetailIndex((x >> lod), (y >> lod), (z >> lod))];
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillStructureMaterialsInto(byte[] uncompressedMaterials, byte transform, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                            int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b000), currentX, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b001), currentX, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b010), currentX, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b011), currentX, currentY + nextSize, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b100), currentX + nextSize, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b101), currentX + nextSize, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b110), currentX + nextSize, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b111), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
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
            for (int x = 0; x < lengthX; x++)
                for (int z = 0; z < lengthZ; z++)
                    for (int y = 0; y < lengthY; y++) {
                        int targetIndex = getUncompressedIndex(targetStartX + x, targetStartY + y, targetStartZ + z);
                        if ((Material.getMaterialProperties(uncompressedMaterials[targetIndex]) & STRUCTURE_REPLACEABLE) == 0) continue;
                        uncompressedMaterials[targetIndex] = material;
                    }
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x++)
            for (int z = 0; z < lengthZ; z++)
                for (int y = 0; y < lengthY; y++) {
                    int targetIndex = getUncompressedIndex(targetStartX + x, targetStartY + y, targetStartZ + z);
                    byte material = data[startIndex + getInDetailIndex(transform, x, y, z)];
                    if (material == AIR) continue;
                    if ((Material.getMaterialProperties(uncompressedMaterials[targetIndex]) & STRUCTURE_REPLACEABLE) == 0) continue;
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillStructureMaterialsInto(byte[] uncompressedMaterials, byte transform, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                            int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(lod, sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b000), currentX, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b001), currentX, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b010), currentX, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b011), currentX, currentY + nextSize, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b100), currentX + nextSize, currentY, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b101), currentX + nextSize, currentY, currentZ + nextSize);
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b110), currentX + nextSize, currentY + nextSize, currentZ);
            fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex, transform, 0b111), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
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
            for (int x = 0; x < lengthX; x += stepSize)
                for (int z = 0; z < lengthZ; z += stepSize)
                    for (int y = 0; y < lengthY; y += stepSize) {
                        int targetIndex = getUncompressedIndex(targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                        if ((Material.getMaterialProperties(uncompressedMaterials[targetIndex]) & STRUCTURE_REPLACEABLE) == 0) continue;
                        uncompressedMaterials[targetIndex] = material;
                    }
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x += stepSize)
            for (int z = 0; z < lengthZ; z += stepSize)
                for (int y = 0; y < lengthY; y += stepSize) {
                    int targetIndex = getUncompressedIndex(targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                    byte material = data[startIndex + getInDetailIndex(transform, x >> lod, y >> lod, z >> lod)];
                    if (material == AIR) continue;
                    if ((Material.getMaterialProperties(uncompressedMaterials[targetIndex]) & STRUCTURE_REPLACEABLE) == 0) continue;
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private void fillMaterialsInto(byte[] uncompressedMaterials, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                   int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(lod, sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + SPLITTER_BYTE_SIZE, currentX, currentY, currentZ);
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 1), currentX, currentY, currentZ + nextSize);
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 4), currentX, currentY + nextSize, currentZ);
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 7), currentX, currentY + nextSize, currentZ + nextSize);
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 10), currentX + nextSize, currentY, currentZ);
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 13), currentX + nextSize, currentY, currentZ + nextSize);
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 16), currentX + nextSize, currentY + nextSize, currentZ);
            fillMaterialsInto(uncompressedMaterials, lod, targetStart, sourceStart, size, sizeBits, startIndex + getOffset(startIndex + 19), currentX + nextSize, currentY + nextSize, currentZ + nextSize);
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
            for (int x = 0; x < lengthX; x += stepSize)
                for (int z = 0; z < lengthZ; z += stepSize)
                    for (int y = 0; y < lengthY; y += stepSize) {
                        int targetIndex = getUncompressedIndex(targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                        uncompressedMaterials[targetIndex] = material;
                    }
            return;
        }
//        if (identifier == DETAIL)
        for (int x = 0; x < lengthX; x += stepSize)
            for (int z = 0; z < lengthZ; z += stepSize)
                for (int y = 0; y < lengthY; y += stepSize) {
                    int targetIndex = getUncompressedIndex(targetStartX + (x >> lod), targetStartY + (y >> lod), targetStartZ + (z >> lod));
                    byte material = data[startIndex + getInDetailIndex(x >> lod, y >> lod, z >> lod)];
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    private int fillSouthLayerInto(ByteArrayList materials, int startIndex) {
        byte identifier = data[startIndex];
        materials.add(identifier);

        if (identifier == HOMOGENOUS) {
            materials.add(data[startIndex + 1]);
            return HOMOGENOUS_BYTE_SIZE_2D;
        }
        if (identifier == DETAIL) {
            materials.add(data[startIndex + 1]);
            materials.add(data[startIndex + 2]);
            materials.add(data[startIndex + 5]);
            materials.add(data[startIndex + 6]);
            return DETAIL_BYTE_SIZE_2D;
        }
//        if (identifier == SPLITTER)
        int index = materials.size() - 1;
        materials.pad(SPLITTER_BYTE_SIZE_2D - 1);
        int offset = SPLITTER_BYTE_SIZE_2D;

        offset += fillSouthLayerInto(materials, startIndex + SPLITTER_BYTE_SIZE);
        setOffset(materials, offset, index + 1);
        offset += fillSouthLayerInto(materials, startIndex + getOffset(startIndex + 4));
        setOffset(materials, offset, index + 4);
        offset += fillSouthLayerInto(materials, startIndex + getOffset(startIndex + 10));
        setOffset(materials, offset, index + 7);
        offset += fillSouthLayerInto(materials, startIndex + getOffset(startIndex + 16));
        return offset;
    }

    private int fillNorthLayerInto(ByteArrayList materials, int startIndex) {
        byte identifier = data[startIndex];
        materials.add(identifier);

        if (identifier == HOMOGENOUS) {
            materials.add(data[startIndex + 1]);
            return HOMOGENOUS_BYTE_SIZE_2D;
        }
        if (identifier == DETAIL) {
            materials.add(data[startIndex + 3]);
            materials.add(data[startIndex + 4]);
            materials.add(data[startIndex + 7]);
            materials.add(data[startIndex + 8]);
            return DETAIL_BYTE_SIZE_2D;
        }
//        if (identifier == SPLITTER)
        int index = materials.size() - 1;
        materials.pad(SPLITTER_BYTE_SIZE_2D - 1);
        int offset = SPLITTER_BYTE_SIZE_2D;

        offset += fillNorthLayerInto(materials, startIndex + getOffset(startIndex + 1));
        setOffset(materials, offset, index + 1);
        offset += fillNorthLayerInto(materials, startIndex + getOffset(startIndex + 7));
        setOffset(materials, offset, index + 4);
        offset += fillNorthLayerInto(materials, startIndex + getOffset(startIndex + 13));
        setOffset(materials, offset, index + 7);
        offset += fillNorthLayerInto(materials, startIndex + getOffset(startIndex + 19));
        return offset;
    }

    private int fillBottomLayerInto(ByteArrayList materials, int startIndex) {
        byte identifier = data[startIndex];
        materials.add(identifier);

        if (identifier == HOMOGENOUS) {
            materials.add(data[startIndex + 1]);
            return HOMOGENOUS_BYTE_SIZE_2D;
        }
        if (identifier == DETAIL) {
            materials.add(data[startIndex + 1]);
            materials.add(data[startIndex + 3]);
            materials.add(data[startIndex + 5]);
            materials.add(data[startIndex + 7]);
            return DETAIL_BYTE_SIZE_2D;
        }
//        if (identifier == SPLITTER)
        int index = materials.size() - 1;
        materials.pad(SPLITTER_BYTE_SIZE_2D - 1);
        int offset = SPLITTER_BYTE_SIZE_2D;

        offset += fillBottomLayerInto(materials, startIndex + SPLITTER_BYTE_SIZE);
        setOffset(materials, offset, index + 1);
        offset += fillBottomLayerInto(materials, startIndex + getOffset(startIndex + 1));
        setOffset(materials, offset, index + 4);
        offset += fillBottomLayerInto(materials, startIndex + getOffset(startIndex + 10));
        setOffset(materials, offset, index + 7);
        offset += fillBottomLayerInto(materials, startIndex + getOffset(startIndex + 13));
        return offset;
    }

    private int fillTopLayerInto(ByteArrayList materials, int startIndex) {
        byte identifier = data[startIndex];
        materials.add(identifier);

        if (identifier == HOMOGENOUS) {
            materials.add(data[startIndex + 1]);
            return HOMOGENOUS_BYTE_SIZE_2D;
        }
        if (identifier == DETAIL) {
            materials.add(data[startIndex + 2]);
            materials.add(data[startIndex + 4]);
            materials.add(data[startIndex + 6]);
            materials.add(data[startIndex + 8]);
            return DETAIL_BYTE_SIZE_2D;
        }
//        if (identifier == SPLITTER)
        int index = materials.size() - 1;
        materials.pad(SPLITTER_BYTE_SIZE_2D - 1);
        int offset = SPLITTER_BYTE_SIZE_2D;

        offset += fillTopLayerInto(materials, startIndex + getOffset(startIndex + 4));
        setOffset(materials, offset, index + 1);
        offset += fillTopLayerInto(materials, startIndex + getOffset(startIndex + 7));
        setOffset(materials, offset, index + 4);
        offset += fillTopLayerInto(materials, startIndex + getOffset(startIndex + 16));
        setOffset(materials, offset, index + 7);
        offset += fillTopLayerInto(materials, startIndex + getOffset(startIndex + 19));
        return offset;
    }

    private int fillEastLayerInto(ByteArrayList materials, int startIndex) {
        byte identifier = data[startIndex];
        materials.add(identifier);

        if (identifier == HOMOGENOUS) {
            materials.add(data[startIndex + 1]);
            return HOMOGENOUS_BYTE_SIZE_2D;
        }
        if (identifier == DETAIL) {
            materials.add(data[startIndex + 1]);
            materials.add(data[startIndex + 2]);
            materials.add(data[startIndex + 3]);
            materials.add(data[startIndex + 4]);
            return DETAIL_BYTE_SIZE_2D;
        }
//        if (identifier == SPLITTER)
        int index = materials.size() - 1;
        materials.pad(SPLITTER_BYTE_SIZE_2D - 1);
        int offset = SPLITTER_BYTE_SIZE_2D;

        offset += fillEastLayerInto(materials, startIndex + SPLITTER_BYTE_SIZE);
        setOffset(materials, offset, index + 1);
        offset += fillEastLayerInto(materials, startIndex + getOffset(startIndex + 4));
        setOffset(materials, offset, index + 4);
        offset += fillEastLayerInto(materials, startIndex + getOffset(startIndex + 1));
        setOffset(materials, offset, index + 7);
        offset += fillEastLayerInto(materials, startIndex + getOffset(startIndex + 7));
        return offset;
    }

    private int fillWestLayerInto(ByteArrayList materials, int startIndex) {
        byte identifier = data[startIndex];
        materials.add(identifier);

        if (identifier == HOMOGENOUS) {
            materials.add(data[startIndex + 1]);
            return HOMOGENOUS_BYTE_SIZE_2D;
        }
        if (identifier == DETAIL) {
            materials.add(data[startIndex + 5]);
            materials.add(data[startIndex + 6]);
            materials.add(data[startIndex + 7]);
            materials.add(data[startIndex + 8]);
            return DETAIL_BYTE_SIZE_2D;
        }
//        if (identifier == SPLITTER)
        int index = materials.size() - 1;
        materials.pad(SPLITTER_BYTE_SIZE_2D - 1);
        int offset = SPLITTER_BYTE_SIZE_2D;

        offset += fillWestLayerInto(materials, startIndex + getOffset(startIndex + 10));
        setOffset(materials, offset, index + 1);
        offset += fillWestLayerInto(materials, startIndex + getOffset(startIndex + 16));
        setOffset(materials, offset, index + 4);
        offset += fillWestLayerInto(materials, startIndex + getOffset(startIndex + 13));
        setOffset(materials, offset, index + 7);
        offset += fillWestLayerInto(materials, startIndex + getOffset(startIndex + 19));
        return offset;
    }

    private void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, byte[][] adjacentChunkLayers, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            if (material == AIR) return;
            int length = 1 << sizeBits;

            generateToMeshFacesHomogenousNorthLayer(toMeshFacesMaps[NORTH][inChunkZ + length - 1], adjacentChunkLayers, sizeBits, material, inChunkX, inChunkY, inChunkZ + length);
            generateToMeshFacesHomogenousTopLayer(toMeshFacesMaps[TOP][inChunkY + length - 1], adjacentChunkLayers, sizeBits, material, inChunkX, inChunkY + length, inChunkZ);
            generateToMeshFacesHomogenousWestLayer(toMeshFacesMaps[WEST][inChunkX + length - 1], adjacentChunkLayers, sizeBits, material, inChunkX + length, inChunkY, inChunkZ);
            generateToMeshFacesHomogenousSouthLayer(toMeshFacesMaps[SOUTH][inChunkZ], adjacentChunkLayers, sizeBits, material, inChunkX, inChunkY, inChunkZ - 1);
            generateToMeshFacesHomogenousBottomLayer(toMeshFacesMaps[BOTTOM][inChunkY], adjacentChunkLayers, sizeBits, material, inChunkX, inChunkY - 1, inChunkZ);
            generateToMeshFacesHomogenousEastLayer(toMeshFacesMaps[EAST][inChunkX], adjacentChunkLayers, sizeBits, material, inChunkX - 1, inChunkY, inChunkZ);
            return;
        }

//        if (identifier == DETAIL)
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY, inChunkZ + 1);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY + 1, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY + 1, inChunkZ + 1);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX + 1, inChunkY, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX + 1, inChunkY, inChunkZ + 1);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX + 1, inChunkY + 1, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, inChunkX + 1, inChunkY + 1, inChunkZ + 1);
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

    private int startIndexOf(int inChunkX, int inChunkY, int inChunkZ, int targetSizeBits) {
        int index = 0, sizeBits = totalSizeBits;
        synchronized (this) {
            while (true) { // Scary but should be fine
                byte identifier = data[index];
                if (sizeBits <= targetSizeBits || identifier == HOMOGENOUS || identifier == DETAIL) return index;
//            if (identifier == SPLITTER)
                index += getOffset(index, inChunkX, inChunkY, inChunkZ, --sizeBits);
            }
        }
    }


    private void generateToMeshFacesHomogenousNorthLayer(long[] toMeshFacesMap, byte[][] adjacentChunkLayers, int sizeBits, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkZ == CHUNK_SIZE) {
            byte[] adjacentChunkLayer = adjacentChunkLayers[NORTH];
            int startIndex = startIndexOf2D(adjacentChunkLayer, inChunkX, inChunkY, CHUNK_SIZE_BITS, sizeBits);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex, sizeBits, material, inChunkX, inChunkY);
            return;
        }

        int startIndex = startIndexOf(inChunkX, inChunkY, inChunkZ, sizeBits);
        generateToMeshFacesHomogenousNorthLayerInside(toMeshFacesMap, sizeBits, material, startIndex, inChunkX, inChunkY);
    }

    private void generateToMeshFacesHomogenousNorthLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkY) {
        byte identifier = data[startIndex];
        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesHomogenousNorthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY);
            generateToMeshFacesHomogenousNorthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize);
            generateToMeshFacesHomogenousNorthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY);
            generateToMeshFacesHomogenousNorthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            fillToMeshFacesMapHomogenous(toMeshFacesMap, sizeBits, material, data[startIndex + 1], inChunkX, inChunkY);
            return;
        }
//        if (identifier == DETAIL)
        if (MeshGenerator.isVisible(material, data[startIndex + 1])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 2])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkY + 1;
        if (MeshGenerator.isVisible(material, data[startIndex + 5])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 6])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkY + 1;
    }

    private void generateToMeshFacesHomogenousSouthLayer(long[] toMeshFacesMap, byte[][] adjacentChunkLayers, int sizeBits, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkZ == -1) {
            byte[] adjacentChunkLayer = adjacentChunkLayers[SOUTH];
            int startIndex = startIndexOf2D(adjacentChunkLayer, inChunkX, inChunkY, CHUNK_SIZE_BITS, sizeBits);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex, sizeBits, material, inChunkX, inChunkY);
            return;
        }

        int startIndex = startIndexOf(inChunkX, inChunkY, inChunkZ, sizeBits);
        generateToMeshFacesHomogenousSouthLayerInside(toMeshFacesMap, sizeBits, material, startIndex, inChunkX, inChunkY);
    }

    private void generateToMeshFacesHomogenousSouthLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkY) {
        byte identifier = data[startIndex];
        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesHomogenousSouthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY);
            generateToMeshFacesHomogenousSouthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize);
            generateToMeshFacesHomogenousSouthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY);
            generateToMeshFacesHomogenousSouthLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            fillToMeshFacesMapHomogenous(toMeshFacesMap, sizeBits, material, data[startIndex + 1], inChunkX, inChunkY);
            return;
        }
//        if (identifier == DETAIL)
        if (MeshGenerator.isVisible(material, data[startIndex + 3])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 4])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkY + 1;
        if (MeshGenerator.isVisible(material, data[startIndex + 7])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 8])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkY + 1;
    }

    private void generateToMeshFacesHomogenousTopLayer(long[] toMeshFacesMap, byte[][] adjacentChunkLayers, int sizeBits, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkY == CHUNK_SIZE) {
            byte[] adjacentChunkLayer = adjacentChunkLayers[TOP];
            int startIndex = startIndexOf2D(adjacentChunkLayer, inChunkX, inChunkZ, CHUNK_SIZE_BITS, sizeBits);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex, sizeBits, material, inChunkX, inChunkZ);
            return;
        }

        int startIndex = startIndexOf(inChunkX, inChunkY, inChunkZ, sizeBits);
        generateToMeshFacesHomogenousTopLayerInside(toMeshFacesMap, sizeBits, material, startIndex, inChunkX, inChunkZ);
    }

    private void generateToMeshFacesHomogenousTopLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkZ) {
        byte identifier = data[startIndex];
        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesHomogenousTopLayerInside(toMeshFacesMap, sizeBits, material, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkZ);
            generateToMeshFacesHomogenousTopLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 1), inChunkX, inChunkZ + nextSize);
            generateToMeshFacesHomogenousTopLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkZ);
            generateToMeshFacesHomogenousTopLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkZ + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            fillToMeshFacesMapHomogenous(toMeshFacesMap, sizeBits, material, data[startIndex + 1], inChunkX, inChunkZ);
            return;
        }
//        if (identifier == DETAIL)
        if (MeshGenerator.isVisible(material, data[startIndex + 1])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkZ + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 3])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkZ + 1;
        if (MeshGenerator.isVisible(material, data[startIndex + 5])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkZ + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 7])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkZ + 1;
    }

    private void generateToMeshFacesHomogenousBottomLayer(long[] toMeshFacesMap, byte[][] adjacentChunkLayers, int sizeBits, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkY == -1) {
            byte[] adjacentChunkLayer = adjacentChunkLayers[BOTTOM];
            int startIndex = startIndexOf2D(adjacentChunkLayer, inChunkX, inChunkZ, CHUNK_SIZE_BITS, sizeBits);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex, sizeBits, material, inChunkX, inChunkZ);
            return;
        }

        int startIndex = startIndexOf(inChunkX, inChunkY, inChunkZ, sizeBits);
        generateToMeshFacesHomogenousBottomLayerInside(toMeshFacesMap, sizeBits, material, startIndex, inChunkX, inChunkZ);
    }

    private void generateToMeshFacesHomogenousBottomLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkZ) {
        byte identifier = data[startIndex];
        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesHomogenousBottomLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 4), inChunkX, inChunkZ);
            generateToMeshFacesHomogenousBottomLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 7), inChunkX, inChunkZ + nextSize);
            generateToMeshFacesHomogenousBottomLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkZ);
            generateToMeshFacesHomogenousBottomLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkZ + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            fillToMeshFacesMapHomogenous(toMeshFacesMap, sizeBits, material, data[startIndex + 1], inChunkX, inChunkZ);
            return;
        }
//        if (identifier == DETAIL)
        if (MeshGenerator.isVisible(material, data[startIndex + 2])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkZ + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 4])) toMeshFacesMap[inChunkX + 0] |= 1L << inChunkZ + 1;
        if (MeshGenerator.isVisible(material, data[startIndex + 6])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkZ + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 8])) toMeshFacesMap[inChunkX + 1] |= 1L << inChunkZ + 1;
    }

    private void generateToMeshFacesHomogenousWestLayer(long[] toMeshFacesMap, byte[][] adjacentChunkLayers, int sizeBits, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkX == CHUNK_SIZE) {
            byte[] adjacentChunkLayer = adjacentChunkLayers[WEST];
            int startIndex = startIndexOf2D(adjacentChunkLayer, inChunkZ, inChunkY, CHUNK_SIZE_BITS, sizeBits);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex, sizeBits, material, inChunkZ, inChunkY);
            return;
        }

        int startIndex = startIndexOf(inChunkX, inChunkY, inChunkZ, sizeBits);
        generateToMeshFacesHomogenousWestLayerInside(toMeshFacesMap, sizeBits, material, startIndex, inChunkY, inChunkZ);
    }

    private void generateToMeshFacesHomogenousWestLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];
        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesHomogenousWestLayerInside(toMeshFacesMap, sizeBits, material, startIndex + SPLITTER_BYTE_SIZE, inChunkY, inChunkZ);
            generateToMeshFacesHomogenousWestLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 1), inChunkY, inChunkZ + nextSize);
            generateToMeshFacesHomogenousWestLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 4), inChunkY + nextSize, inChunkZ);
            generateToMeshFacesHomogenousWestLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 7), inChunkY + nextSize, inChunkZ + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            fillToMeshFacesMapHomogenous(toMeshFacesMap, sizeBits, material, data[startIndex + 1], inChunkZ, inChunkY);
            return;
        }
//        if (identifier == DETAIL)
        if (MeshGenerator.isVisible(material, data[startIndex + 1])) toMeshFacesMap[inChunkZ + 0] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 2])) toMeshFacesMap[inChunkZ + 0] |= 1L << inChunkY + 1;
        if (MeshGenerator.isVisible(material, data[startIndex + 3])) toMeshFacesMap[inChunkZ + 1] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 4])) toMeshFacesMap[inChunkZ + 1] |= 1L << inChunkY + 1;
    }

    private void generateToMeshFacesHomogenousEastLayer(long[] toMeshFacesMap, byte[][] adjacentChunkLayers, int sizeBits, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkX == -1) {
            byte[] adjacentChunkLayer = adjacentChunkLayers[EAST];
            int startIndex = startIndexOf2D(adjacentChunkLayer, inChunkZ, inChunkY, CHUNK_SIZE_BITS, sizeBits);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex, sizeBits, material, inChunkZ, inChunkY);
            return;
        }

        int startIndex = startIndexOf(inChunkX, inChunkY, inChunkZ, sizeBits);
        generateToMeshFacesHomogenousEastLayerInside(toMeshFacesMap, sizeBits, material, startIndex, inChunkY, inChunkZ);
    }

    private void generateToMeshFacesHomogenousEastLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkY, int inChunkZ) {
        byte identifier = data[startIndex];
        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesHomogenousEastLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 10), inChunkY, inChunkZ);
            generateToMeshFacesHomogenousEastLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 13), inChunkY, inChunkZ + nextSize);
            generateToMeshFacesHomogenousEastLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 16), inChunkY + nextSize, inChunkZ);
            generateToMeshFacesHomogenousEastLayerInside(toMeshFacesMap, sizeBits, material, startIndex + getOffset(startIndex + 19), inChunkY + nextSize, inChunkZ + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            fillToMeshFacesMapHomogenous(toMeshFacesMap, sizeBits, material, data[startIndex + 1], inChunkZ, inChunkY);
            return;
        }
//        if (identifier == DETAIL)
        if (MeshGenerator.isVisible(material, data[startIndex + 5])) toMeshFacesMap[inChunkZ + 0] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 6])) toMeshFacesMap[inChunkZ + 0] |= 1L << inChunkY + 1;
        if (MeshGenerator.isVisible(material, data[startIndex + 7])) toMeshFacesMap[inChunkZ + 1] |= 1L << inChunkY + 0;
        if (MeshGenerator.isVisible(material, data[startIndex + 8])) toMeshFacesMap[inChunkZ + 1] |= 1L << inChunkY + 1;
    }

    private static void fillToMeshFacesMapHomogenous(long[] toMeshFacesMap, int sizeBits, byte material, byte occludingMaterial, int inChunkA, int inChunkB) {
        if (!MeshGenerator.isVisible(material, occludingMaterial)) return;
        int length = 1 << sizeBits;
        long mask = getMask(length, inChunkB);
        for (int a = inChunkA; a < inChunkA + length; a++) toMeshFacesMap[a] |= mask;
    }

    private static void generateToMeshFacesDetail(long[][][] toMeshFacesMap, byte[] uncompressedMaterials, byte[][] adjacentChunkLayers, int inChunkX, int inChunkY, int inChunkZ) {
        byte material = uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)];
        if (material == AIR) return;

        byte northMaterial = getMaterial(uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY, inChunkZ + 1);
        byte topMaterial = getMaterial(uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY + 1, inChunkZ);
        byte westMaterial = getMaterial(uncompressedMaterials, adjacentChunkLayers, inChunkX + 1, inChunkY, inChunkZ);
        byte southMaterial = getMaterial(uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY, inChunkZ - 1);
        byte bottomMaterial = getMaterial(uncompressedMaterials, adjacentChunkLayers, inChunkX, inChunkY - 1, inChunkZ);
        byte eastMaterial = getMaterial(uncompressedMaterials, adjacentChunkLayers, inChunkX - 1, inChunkY, inChunkZ);

        if (MeshGenerator.isVisible(material, northMaterial)) toMeshFacesMap[NORTH][inChunkZ][inChunkX] |= 1L << inChunkY;
        if (MeshGenerator.isVisible(material, topMaterial)) toMeshFacesMap[TOP][inChunkY][inChunkX] |= 1L << inChunkZ;
        if (MeshGenerator.isVisible(material, westMaterial)) toMeshFacesMap[WEST][inChunkX][inChunkZ] |= 1L << inChunkY;
        if (MeshGenerator.isVisible(material, southMaterial)) toMeshFacesMap[SOUTH][inChunkZ][inChunkX] |= 1L << inChunkY;
        if (MeshGenerator.isVisible(material, bottomMaterial)) toMeshFacesMap[BOTTOM][inChunkY][inChunkX] |= 1L << inChunkZ;
        if (MeshGenerator.isVisible(material, eastMaterial)) toMeshFacesMap[EAST][inChunkX][inChunkZ] |= 1L << inChunkY;
    }

    private static byte getMaterial(byte[] uncompressedMaterials, byte[][] adjacentChunkLayers, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkX == -1) return getMaterial2D(adjacentChunkLayers[EAST], inChunkZ, inChunkY);
        if (inChunkX == CHUNK_SIZE) return getMaterial2D(adjacentChunkLayers[WEST], inChunkZ, inChunkY);
        if (inChunkY == -1) return getMaterial2D(adjacentChunkLayers[BOTTOM], inChunkX, inChunkZ);
        if (inChunkY == CHUNK_SIZE) return getMaterial2D(adjacentChunkLayers[TOP], inChunkX, inChunkZ);
        if (inChunkZ == -1) return getMaterial2D(adjacentChunkLayers[SOUTH], inChunkX, inChunkY);
        if (inChunkZ == CHUNK_SIZE) return getMaterial2D(adjacentChunkLayers[NORTH], inChunkX, inChunkY);

        return uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)];
    }

    private static void generateToMeshFacesHomogenousSideLayer(long[] toMeshFacesMap, byte[] adjacentChunkLayer, int startIndex, int sizeBits, byte material, int inChunkA, int inChunkB) {
        byte identifier = adjacentChunkLayer[startIndex];
        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex + SPLITTER_BYTE_SIZE_2D, sizeBits, material, inChunkA, inChunkB);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex + getOffset2D(adjacentChunkLayer, startIndex + 1), sizeBits, material, inChunkA, inChunkB + nextSize);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex + getOffset2D(adjacentChunkLayer, startIndex + 4), sizeBits, material, inChunkA + nextSize, inChunkB);
            generateToMeshFacesHomogenousSideLayer(toMeshFacesMap, adjacentChunkLayer, startIndex + getOffset2D(adjacentChunkLayer, startIndex + 7), sizeBits, material, inChunkA + nextSize, inChunkB + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            fillToMeshFacesMapHomogenous(toMeshFacesMap, sizeBits, material, adjacentChunkLayer[startIndex + 1], inChunkA, inChunkB);
            return;
        }
//        if (identifier == DETAIL)
        if (MeshGenerator.isVisible(material, adjacentChunkLayer[startIndex + 1])) toMeshFacesMap[inChunkA + 0] |= 1L << inChunkB + 0;
        if (MeshGenerator.isVisible(material, adjacentChunkLayer[startIndex + 2])) toMeshFacesMap[inChunkA + 0] |= 1L << inChunkB + 1;
        if (MeshGenerator.isVisible(material, adjacentChunkLayer[startIndex + 3])) toMeshFacesMap[inChunkA + 1] |= 1L << inChunkB + 0;
        if (MeshGenerator.isVisible(material, adjacentChunkLayer[startIndex + 4])) toMeshFacesMap[inChunkA + 1] |= 1L << inChunkB + 1;
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

    private static int getInSplitterIndex2D(int inChunkA, int inChunkB, int sizeBits) {
        return 3 * ((inChunkA >> sizeBits & 1) << 1 | (inChunkB >> sizeBits & 1));
    }

    private static int getInDetailIndex2D(int inChunkA, int inChunkB) {
        return ((inChunkA & 1) << 1 | (inChunkB & 1)) + 1;
    }

    private static int getOffset2D(byte[] data, int splitterIndex, int inChunkA, int inChunkB, int sizeBits) {
        int inSplitterIndex = getInSplitterIndex2D(inChunkA, inChunkB, sizeBits);
        if (inSplitterIndex == 0) return SPLITTER_BYTE_SIZE_2D;
        return getOffset2D(data, splitterIndex + inSplitterIndex - 2);
    }

    private static int getOffset2D(byte[] data, int index) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 | data[index + 2] & 0xFF;
    }

    private static byte getMaterial2D(byte[] data, int inChunkA, int inChunkB) {
        int index = 0, sizeBits = CHUNK_SIZE_BITS;

        while (true) { // Scary but should be fine
            byte identifier = data[index];

            if (identifier == HOMOGENOUS) return data[index + 1];
            if (identifier == DETAIL) return data[index + getInDetailIndex2D(inChunkA, inChunkB)];
//            if (identifier == SPLITTER)
            index += getOffset2D(data, index, inChunkA, inChunkB, --sizeBits);
        }
    }

    private static int startIndexOf2D(byte[] data, int inChunkA, int inChunkB, int sizeBits, int targetSizeBits) {
        int index = 0;
        while (true) { // Scary but should be fine
            byte identifier = data[index];
            if (sizeBits <= targetSizeBits || identifier == HOMOGENOUS || identifier == DETAIL) return index;
//            if (identifier == SPLITTER)
            index += getOffset2D(data, index, inChunkA, inChunkB, --sizeBits);
        }
    }

    private static long getMask(int length, int offset) {
        return length == CHUNK_SIZE ? -1L : (1L << length) - 1 << offset;
    }

    private static void compressMaterials(ByteArrayList data, byte[] uncompressedMaterials, int totalSizeBits) {
        if (isHomogenous(uncompressedMaterials)) {
            data.add(HOMOGENOUS);
            data.add(uncompressedMaterials[0]);
            return;
        }

        if (totalSizeBits <= 1) {
            data.add(DETAIL);
            data.add(uncompressedMaterials[getUncompressedIndex(0, 0, 0)]);
            data.add(uncompressedMaterials[getUncompressedIndex(0, 1, 0)]);
            data.add(uncompressedMaterials[getUncompressedIndex(0, 0, 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(0, 1, 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(1, 0, 0)]);
            data.add(uncompressedMaterials[getUncompressedIndex(1, 1, 0)]);
            data.add(uncompressedMaterials[getUncompressedIndex(1, 0, 1)]);
            data.add(uncompressedMaterials[getUncompressedIndex(1, 1, 1)]);
            return;
        }

        int nextSize = 1 << totalSizeBits - 1;
        data.add(SPLITTER);
        data.pad(SPLITTER_BYTE_SIZE - 1);
        int offset = SPLITTER_BYTE_SIZE;

        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, 0, 0, 0);
        setOffset(data, offset, 1);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, 0, 0, nextSize);
        setOffset(data, offset, 4);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, 0, nextSize, 0);
        setOffset(data, offset, 7);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, 0, nextSize, nextSize);
        setOffset(data, offset, 10);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, nextSize, 0, 0);
        setOffset(data, offset, 13);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, nextSize, 0, nextSize);
        setOffset(data, offset, 16);
        offset += compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, nextSize, nextSize, 0);
        setOffset(data, offset, 19);
        compressMaterials(data, uncompressedMaterials, totalSizeBits - 1, offset, nextSize, nextSize, nextSize);
    }

    private static int compressMaterials(ByteArrayList data, byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        if (isHomogenous(inChunkX, inChunkY, inChunkZ, sizeBits, uncompressedMaterials)) {
            data.add(HOMOGENOUS);
            data.add(uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)]);
            return HOMOGENOUS_BYTE_SIZE;
        }
        if (sizeBits <= 1) {
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

        int nextSize = 1 << --sizeBits;
        data.add(SPLITTER);
        data.pad(SPLITTER_BYTE_SIZE - 1);
        int offset = SPLITTER_BYTE_SIZE;

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

        return offset;
    }

    private static boolean isHomogenous(final int startX, final int startY, final int startZ, int sizeBits, byte[] uncompressedMaterials) {
        int startIndex = getUncompressedIndex(startX, startY, startZ);
        byte material = uncompressedMaterials[startIndex];
        int endIndex = startIndex + (1 << sizeBits * 3);
        for (int index = startIndex; index < endIndex; index++) if (uncompressedMaterials[index] != material) return false;
        return true;
    }

    private static boolean isHomogenous(byte[] uncompressedMaterials) {
        byte reference = uncompressedMaterials[0];
        for (byte material : uncompressedMaterials) if (material != reference) return false;
        return true;
    }


    public static final int[] Z_ORDER_3D_TABLE_X = Utils.zOrderCurveLookupTable(256, 3, 2);
    public static final int[] Z_ORDER_3D_TABLE_Y = Utils.zOrderCurveLookupTable(256, 3, 1);
    public static final int[] T_ORDER_3D_TABLE_Z = Utils.zOrderCurveLookupTable(256, 3, 0);

    private static final byte HOMOGENOUS = 0;
    private static final byte DETAIL = 1;
    private static final byte SPLITTER = 2;

    private static final byte HOMOGENOUS_BYTE_SIZE = 2;
    private static final byte DETAIL_BYTE_SIZE = 9;
    private static final byte SPLITTER_BYTE_SIZE = 22;

    private static final byte HOMOGENOUS_BYTE_SIZE_2D = 2;
    private static final byte DETAIL_BYTE_SIZE_2D = 5;
    private static final byte SPLITTER_BYTE_SIZE_2D = 10;

    private byte[] data;
    private final int totalSizeBits;
}
