package game.server.materials_data;

import core.utils.ByteArrayList;
import core.utils.IntArrayList;
import core.utils.MathUtils;

import game.player.interaction.PlaceMode;
import game.player.interaction.ShapePlaceable;
import game.player.particles.ParticleCollector;
import game.player.rendering.AABB;
import game.player.rendering.MeshGenerator;
import game.server.Game;
import game.server.Chunk;
import game.server.generation.Structure;
import game.server.material.Material;
import game.server.material.Properties;
import game.settings.IntSettings;
import game.settings.OptionSettings;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.Arrays;

import static game.utils.Constants.*;

public final class MaterialsData {

    /**
     * Creates a homogeneous materials tree for one region of size {@code 1 << totalSizeBits}
     * in each axis.
     *
     * @param totalSizeBits log2 of the edge length of this materials volume in block coordinates
     * @param material material stored at every block position in this volume
     */
    public MaterialsData(int totalSizeBits, byte material) {
        data = new byte[]{(byte) (getType(material) | HOMOGENOUS), material};
        this.totalSizeBits = totalSizeBits;
    }

    /**
     * Creates a materials tree from already-compressed data.
     *
     * @param totalSizeBits log2 of the edge length of this materials volume in block coordinates
     * @param data compressed tree payload
     */
    public MaterialsData(int totalSizeBits, byte[] data) {
        this.data = data;
        this.totalSizeBits = totalSizeBits;
    }

    // Static API
    /**
     * Converts local chunk coordinates to the linear index used by the uncompressed
     * materials array.
     *
     * @param inChunkX local X coordinate inside a chunk or other {@code 2^n}-sized volume
     * @param inChunkY local Y coordinate inside a chunk or other {@code 2^n}-sized volume
     * @param inChunkZ local Z coordinate inside a chunk or other {@code 2^n}-sized volume
     * @return linear index in X/Z/Y order as defined by the lookup tables
     */
    public static int getUncompressedIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return Z_ORDER_3D_TABLE_X[inChunkX] | Z_ORDER_3D_TABLE_Y[inChunkY] | T_ORDER_3D_TABLE_Z[inChunkZ];
    }

    /**
     * Compresses a fully uncompressed materials array into a tree representation.
     *
     * @param sizeBits log2 of the edge length of the source volume in block coordinates
     * @param uncompressedMaterials materials indexed by {@link #getUncompressedIndex(int, int, int)}
     * @return compressed materials tree for the same volume
     */
    public static MaterialsData getCompressedMaterials(int sizeBits, byte[] uncompressedMaterials) {
        if (sizeBits == 0) return new MaterialsData(0, uncompressedMaterials[0]);
        ByteArrayList dataList = new ByteArrayList(1000);
        LongArrayCompressor.compressMaterials(dataList, uncompressedMaterials, sizeBits);
        return new MaterialsData(sizeBits, dataList.toArray());
    }

    /**
     * Compresses a bitmap-backed material selection into a tree representation.
     *
     * @param sizeBits log2 of the edge length of the source volume in block coordinates
     * @param bitMap occupancy bitmap in uncompressed local coordinates
     * @param material material to place wherever the bitmap is set
     * @return compressed materials tree for the same volume
     */
    public static MaterialsData getCompressedMaterials(int sizeBits, long[] bitMap, byte material) {
        if (sizeBits == 0) return new MaterialsData(0, material);
        ByteArrayList dataList = new ByteArrayList(1000);
        BitMapCompressor.compressMaterials(dataList, bitMap, material, sizeBits, 0, 0, 0, 0);
        return new MaterialsData(sizeBits, dataList.toArray());
    }

    /**
     * Copies a structure's materials into an uncompressed buffer.
     *
     * @param uncompressedMaterials destination buffer indexed with {@link #getUncompressedIndex(int, int, int)}
     * @param structure source structure whose materials are copied
     * @param transform structure transform applied while reading source coordinates
     * @param lod level of detail for the destination; one step equals {@code 1 << lod} source blocks
     * @param targetStart destination start position in local block coordinates
     * @param sourceStart source start position in structure-local block coordinates
     * @param size source region size in structure-local block coordinates
     */
/**
 * Fills structure materials into.
 *
 * @param uncompressedMaterials parameter
 * @param structure parameter
 * @param transform parameter
 * @param lod parameter
 * @param targetStart 3D vector in local block coordinates
 * @param sourceStart 3D vector in local block coordinates
 * @param size 3D vector in local block coordinates
 */
    public static void fillStructureMaterialsInto(byte[] uncompressedMaterials, Structure structure, byte transform, int lod,
                                                  Vector3i targetStart, Vector3i sourceStart, Vector3i size) {
        MaterialsData source = structure.materials();
        if ((transform & Structure.MIRROR_X) != 0) sourceStart.x = sourceStart.x + (1 << source.totalSizeBits) - structure.sizeX(transform);
        if (((transform & Structure.MIRROR_Z) == 0) == ((transform & Structure.ROTATE_90) != 0))
            sourceStart.z = sourceStart.z + (1 << source.totalSizeBits) - structure.sizeZ(transform);

        synchronized (source) {
            source.fillStructureMaterialsInto(uncompressedMaterials, transform, lod, targetStart, sourceStart, size, source.totalSizeBits, 0, 0, 0, 0);
        }
    }

    // Object API
    /**
     * Reads one material from this tree using local block coordinates.
     *
     * @param inChunkX local X coordinate inside the current volume
     * @param inChunkY local Y coordinate inside the current volume
     * @param inChunkZ local Z coordinate inside the current volume
     * @return material at the requested local block coordinate
     */
    public byte getMaterial(int inChunkX, int inChunkY, int inChunkZ) {
        int index = 0, sizeBits = totalSizeBits;
        synchronized (this) {
            while (true) { // Scary but should be fine
                byte identifier = getIdentifier(index);

                if (identifier == HOMOGENOUS) return data[index + 1];
                if (identifier == DETAIL) return data[index + getInDetailIndex(inChunkX, inChunkY, inChunkZ)];
//            if (identifier == SPLITTER)
                index += getOffset(index, inChunkX, inChunkY, inChunkZ, --sizeBits);
            }
        }
    }

    /**
     * Expands the full tree into an uncompressed buffer.
     *
     * @param array destination buffer indexed by local block coordinates
     */
    public void fillUncompressedMaterialsInto(byte[] array) {
        synchronized (this) {
            fillUncompressedMaterialsInto(array, totalSizeBits, 0, 0, 0, 0);
        }
    }

    /**
     * Extracts one face-aligned 2D layer from the volume.
     *
     * @param materials destination 2D materials buffer
     * @param side face constant such as {@code NORTH}, {@code SOUTH}, {@code TOP}, or {@code BOTTOM}
     */
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

    /**
     * Expands a source sub-volume into an uncompressed buffer.
     *
     * @param array destination buffer indexed by local block coordinates
     * @param destinationX destination start X in local block coordinates
     * @param destinationY destination start Y in local block coordinates
     * @param destinationZ destination start Z in local block coordinates
     * @param startX source start X in local block coordinates
     * @param startY source start Y in local block coordinates
     * @param startZ source start Z in local block coordinates
     * @param lengthX source width in local block coordinates
     * @param lengthY source height in local block coordinates
     * @param lengthZ source depth in local block coordinates
     */
/**
 * Fills uncompressed materials into.
 *
 * @param array Y coordinate in local block coordinates
 * @param destinationX X coordinate in local block coordinates
 * @param destinationY Y coordinate in local block coordinates
 * @param destinationZ Z coordinate in local block coordinates
 * @param startX X coordinate in local block coordinates
 * @param startY Y coordinate in local block coordinates
 * @param startZ Z coordinate in local block coordinates
 * @param lengthX extent along the X axis in local block coordinates
 * @param lengthY extent along the Y axis in local block coordinates
 * @param lengthZ extent along the Z axis in local block coordinates
 */
    public void fillUncompressedMaterialsInto(byte[] array,
                                              int destinationX, int destinationY, int destinationZ,
                                              int startX, int startY, int startZ,
                                              int lengthX, int lengthY, int lengthZ) {

        Vector3i targetStart = new Vector3i(destinationX, destinationY, destinationZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        synchronized (this) {
            fillUncompressedMaterialsInto(array, 0, targetStart, sourceStart, size, totalSizeBits, 0, 0, 0, 0);
        }
    }

    /**
     * Paints or replaces the materials covered by a repeated placeable shape.
     *
     * @param inChunkX destination X in local block coordinates
     * @param inChunkY destination Y in local block coordinates
     * @param inChunkZ destination Z in local block coordinates
     * @param countX repetition count along local X
     * @param countY repetition count along local Y
     * @param countZ repetition count along local Z
     * @param lod level of detail; shape lengths are shifted right by this amount before placement
     * @param placeable shape definition and material to write
     */
/**
 * Stores material.
 *
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param countX extent along the X axis in local block coordinates
 * @param countY extent along the Y axis in local block coordinates
 * @param countZ extent along the Z axis in local block coordinates
 * @param lod parameter
 * @param placeable parameter
 */
    public void storeMaterial(int inChunkX, int inChunkY, int inChunkZ,
                              int countX, int countY, int countZ,
                              int lod, ShapePlaceable placeable) {
        if (countX <= 0 || countY <= 0 || countZ <= 0) return;
        byte[] uncompressedMaterials = new byte[1 << totalSizeBits * 3];
        fillUncompressedMaterialsInto(uncompressedMaterials);

        int lengthX = placeable.getLengthX();
        int lengthY = placeable.getLengthY();
        int lengthZ = placeable.getLengthZ();

        int startX = Math.max(0, -inChunkX / Math.max(1, lengthX >> lod));
        int startY = Math.max(0, -inChunkY / Math.max(1, lengthY >> lod));
        int startZ = Math.max(0, -inChunkZ / Math.max(1, lengthZ >> lod));

        for (int x = startX; x < countX && inChunkX + (x * lengthX >> lod) < 1 << totalSizeBits; x++)
            for (int y = startY; y < countY && inChunkY + (y * lengthY >> lod) < 1 << totalSizeBits; y++)
                for (int z = startZ; z < countZ && inChunkZ + (z * lengthZ >> lod) < 1 << totalSizeBits; z++) {
                    int shapeInChunkX = inChunkX + (x * lengthX >> lod);
                    int shapeInChunkY = inChunkY + (y * lengthY >> lod);
                    int shapeInChunkZ = inChunkZ + (z * lengthZ >> lod);
                    storeMaterial(shapeInChunkX, shapeInChunkY, shapeInChunkZ, uncompressedMaterials, lod, placeable);
                }

        compressIntoData(uncompressedMaterials);
    }

    /**
     * Copies a transformed structure into this materials volume.
     *
     * @param inChunkX destination X in local block coordinates
     * @param inChunkY destination Y in local block coordinates
     * @param inChunkZ destination Z in local block coordinates
     * @param startX source start X in structure-local block coordinates
     * @param startY source start Y in structure-local block coordinates
     * @param startZ source start Z in structure-local block coordinates
     * @param lengthX source width in structure-local block coordinates
     * @param lengthY source height in structure-local block coordinates
     * @param lengthZ source depth in structure-local block coordinates
     * @param lod level of detail; one destination step equals {@code 1 << lod} source blocks
     * @param structure source structure
     * @param transform structure transform applied to source coordinates
     */
/**
 * Stores structure materials.
 *
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param startX X coordinate in local block coordinates
 * @param startY Y coordinate in local block coordinates
 * @param startZ Z coordinate in local block coordinates
 * @param lengthX extent along the X axis in local block coordinates
 * @param lengthY extent along the Y axis in local block coordinates
 * @param lengthZ extent along the Z axis in local block coordinates
 * @param lod parameter
 * @param structure parameter
 * @param transform parameter
 */
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

    /**
     * Rebuilds the eight lower-LOD children into their parent chunk representation.
     *
     * @param chunk0 child chunk at offset {@code (0, 0, 0)}
     * @param chunk1 child chunk at offset {@code (0, 0, CHUNK_SIZE / 2)}
     * @param chunk2 child chunk at offset {@code (0, CHUNK_SIZE / 2, 0)}
     * @param chunk3 child chunk at offset {@code (0, CHUNK_SIZE / 2, CHUNK_SIZE / 2)}
     * @param chunk4 child chunk at offset {@code (CHUNK_SIZE / 2, 0, 0)}
     * @param chunk5 child chunk at offset {@code (CHUNK_SIZE / 2, 0, CHUNK_SIZE / 2)}
     * @param chunk6 child chunk at offset {@code (CHUNK_SIZE / 2, CHUNK_SIZE / 2, 0)}
     * @param chunk7 child chunk at offset {@code (CHUNK_SIZE / 2, CHUNK_SIZE / 2, CHUNK_SIZE / 2)}
     */
/**
 * Stores lower lod chunks.
 *
 * @param chunk0 parameter
 * @param chunk1 parameter
 * @param chunk2 parameter
 * @param chunk3 parameter
 * @param chunk4 parameter
 * @param chunk5 parameter
 * @param chunk6 parameter
 * @param chunk7 parameter
 */
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

    /**
     * Builds per-face visibility maps from this materials tree.
     *
     * @param toMeshFacesMaps destination face maps indexed by face direction, then by local chunk coordinates
     * @param uncompressedMaterials destination scratch buffer indexed by local block coordinates
     * @param adjacentChunkLayers 2D materials for touching neighbor chunk faces in face-order
     */
    public void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, byte[][] adjacentChunkLayers) {
        MaterialsData surfaceEquivalent = getSurfaceEquivalent();
        surfaceEquivalent.generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, adjacentChunkLayers, totalSizeBits, 0, 0, 0, 0);
    }

    /**
     * Builds per-face visibility maps for one chunk-sized region.
     *
     * @param toMeshFacesMaps destination face maps indexed by face direction, then by local chunk coordinates
     * @param uncompressedMaterials destination scratch buffer indexed by local block coordinates
     * @param chunkX chunk coordinate of the region origin along X
     * @param chunkY chunk coordinate of the region origin along Y
     * @param chunkZ chunk coordinate of the region origin along Z
     */
/**
 * Generates to mesh faces maps.
 *
 * @param toMeshFacesMaps parameter
 * @param uncompressedMaterials parameter
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 */
    public void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, int chunkX, int chunkY, int chunkZ) {
        synchronized (this) {
            int startIndex = startIndexOf(chunkX << CHUNK_SIZE_BITS, chunkY << CHUNK_SIZE_BITS, chunkZ << CHUNK_SIZE_BITS, CHUNK_SIZE_BITS);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, Math.min(CHUNK_SIZE_BITS, totalSizeBits), startIndex, 0, 0, 0);
        }
    }

    /**
     * Collects placement particles for every visible occupied block in the tree.
     *
     * @param collector particle sink
     * @param opaque particle bucket for non-glass materials
     * @param transparent particle bucket for glass-like materials
     * @param lengths local block dimensions used by the particle placement code
     * @param transform structure transform applied to emitted particle positions
     */
    public void addPlaceParticles(ParticleCollector collector, IntArrayList opaque, IntArrayList transparent, Vector3i lengths, byte transform) {
        addPlaceParticles(collector, getBitMap(), transform, lengths, opaque, transparent, totalSizeBits, 0, 0, 0, 0);
    }

    /**
     * Returns a bitmap describing which local block coordinates are non-air.
     *
     * @return bitmap indexed by local block coordinates
     */
    public long[] getBitMap() {
        long[] bitMap = new long[(1 << totalSizeBits * 3) / Long.SIZE];
        fillBitMap(bitMap, totalSizeBits, 0, 0, 0, 0);
        return bitMap;
    }

    /**
     * Returns the compressed backing data.
     *
     * @return compressed tree payload
     */
    public byte[] getBytes() {
        return data;
    }

    /**
     * Returns the log2 edge length of this materials volume.
     *
     * @return log2 of the edge length in block coordinates
     */
    public int getTotalSizeBits() {
        return totalSizeBits;
    }

    /**
     * Checks whether the root node is homogeneous and matches the requested material.
     *
     * @param material material to compare against the root node
     * @return {@code true} if the tree root is homogeneous and stores {@code material}
     */
    public boolean isHomogenous(byte material) {
        return getIdentifier(0) == HOMOGENOUS && data[1] == material;
    }

    /**
     * Computes a conservative occlusion volume for rendering and culling.
     *
     * @return an AABB in local block coordinates
     */
    public AABB getOccluder() {
        AABB method1 = AABB.newMaxChunkAABB();
        AABB method2 = new AABB(0, 0, 0, -1, -1, -1);
        synchronized (this) {
            getOccluder(method1, totalSizeBits, 0, 0, 0, 0);
            getLargestOpaqueAABB(method2, totalSizeBits, 0, 0, 0, 0);
            expand(method2);
        }
        if (method1.isEmpty()) method1.setEmpty();
        return method1.getHalfSurfaceArea() > method2.getHalfSurfaceArea() ? method1 : method2;
    }

    // Miscellaneous functions
    /**
     * Recompresses an uncompressed buffer back into this tree representation.
     *
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     */
    private void compressIntoData(byte[] uncompressedMaterials) {
        ByteArrayList dataList = new ByteArrayList(1000);
        LongArrayCompressor.compressMaterials(dataList, uncompressedMaterials, totalSizeBits);

        byte[] data = dataList.toArray();
        synchronized (this) {
            this.data = data;
        }
    }

    /**
     * Writes one lower-LOD chunk into the provided uncompressed parent buffer.
     *
     * @param chunk source chunk to copy from
     * @param uncompressedMaterials destination buffer indexed by local block coordinates
     * @param startX destination offset in local block coordinates
     * @param startY destination offset in local block coordinates
     * @param startZ destination offset in local block coordinates
     */
/**
 * Stores lower lod chunk.
 *
 * @param chunk parameter
 * @param uncompressedMaterials parameter
 * @param startX X coordinate in local block coordinates
 * @param startY Y coordinate in local block coordinates
 * @param startZ Z coordinate in local block coordinates
 */
    private static void storeLowerLODChunk(Chunk chunk, byte[] uncompressedMaterials, int startX, int startY, int startZ) {
        if (chunk == null) return;

        Vector3i targetStart = new Vector3i(startX, startY, startZ);
        Vector3i sourceStart = new Vector3i(0, 0, 0);
        Vector3i size = new Vector3i(CHUNK_SIZE, CHUNK_SIZE, CHUNK_SIZE);

        synchronized (chunk.getMaterials()) {
            chunk.getMaterials().fillUncompressedMaterialsInto(uncompressedMaterials, 1, targetStart, sourceStart, size, CHUNK_SIZE_BITS, 0, 0, 0, 0);
        }
    }

    /**
     * Paints one placeable into the uncompressed buffer at the given local position.
     *
     * @param inChunkX destination X in local block coordinates
     * @param inChunkY destination Y in local block coordinates
     * @param inChunkZ destination Z in local block coordinates
     * @param uncompressedMaterials destination buffer indexed by local block coordinates
     * @param lod level of detail; placeable dimensions are shifted right by this amount
     * @param placeable shape to stamp into the buffer
     */
/**
 * Stores material.
 *
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param uncompressedMaterials parameter
 * @param lod parameter
 * @param placeable parameter
 */
    private void storeMaterial(int inChunkX, int inChunkY, int inChunkZ, byte[] uncompressedMaterials, int lod, ShapePlaceable placeable) {
        byte material = placeable.getMaterial();
        long[] bitMap = placeable.getBitMap();

        int inChunkAlign = Integer.numberOfTrailingZeros(inChunkX | inChunkY | inChunkZ);
        int align = MathUtils.min(totalSizeBits, inChunkAlign, Integer.numberOfTrailingZeros(placeable.getPreferredSizePowOf2()));
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
                    int materialStartIndex = getUncompressedIndex(inChunkX + x, inChunkY + y, inChunkZ + z);
                    int bitMapStartIndex = getUncompressedIndex(x << lod, y << lod, z << lod);
                    int endIndex = bitMapStartIndex + count, bitMapEndIndex = Math.max(bitMapStartIndex + count >> 6, (bitMapStartIndex >> 6) + 1);

                    storeMaterial(bitMap, uncompressedMaterials,
                            bitMapStartIndex, bitMapEndIndex, mask, endIndex, stride, materialStartIndex, shiftCount,
                            paint, replaceAir, breakHeldOnly,
                            heldMaterial, material);
                }
    }

    /**
     * Applies one bitmap-aligned material write to the destination buffer.
     *
     * @param bitMap source occupancy bitmap in local block coordinates
     * @param uncompressedMaterials destination buffer indexed by local block coordinates
     * @param bitMapStartIndex first bit to inspect in the source bitmap
     * @param bitMapEndIndex end of the covered bitmap word range
     * @param mask alignment mask for the current LOD step
     * @param endIndex exclusive end bit index in the source bitmap
     * @param stride bitmap step in bits for this LOD
     * @param materialStartIndex destination start index in the uncompressed buffer
     * @param shiftCount bit shift used to map bitmap indices to material indices
     * @param paint whether air-only painting mode is active
     * @param replaceAir whether replace-air mode is active
     * @param breakHeldOnly whether break-held-only mode is active
     * @param heldMaterial material required by break-held-only mode
     * @param material material to write
     */
/**
 * Stores material.
 *
 * @param bitMap parameter
 * @param uncompressedMaterials parameter
 * @param bitMapStartIndex X coordinate in local block coordinates
 * @param bitMapEndIndex X coordinate in local block coordinates
 * @param mask parameter
 * @param endIndex X coordinate in local block coordinates
 * @param stride parameter
 * @param materialStartIndex X coordinate in local block coordinates
 * @param shiftCount parameter
 * @param paint parameter
 * @param replaceAir parameter
 * @param breakHeldOnly Y coordinate in local block coordinates
 * @param heldMaterial parameter
 * @param material parameter
 */
    private static void storeMaterial(long[] bitMap, byte[] uncompressedMaterials,
                                      int bitMapStartIndex, int bitMapEndIndex, int mask, int endIndex, int stride, int materialStartIndex, int shiftCount,
                                      boolean paint, boolean replaceAir, boolean breakHeldOnly,
                                      byte heldMaterial, byte material) {
        for (int bitsIndex = bitMapStartIndex >> 6; bitsIndex < bitMapEndIndex; bitsIndex++)
            for (int index = Math.max((bitsIndex << 6) + Long.numberOfTrailingZeros(bitMap[bitsIndex]) & mask, bitMapStartIndex),
                 end = Math.min(bitsIndex + 1 << 6, endIndex); index < end; index += stride) {
                int materialIndex = materialStartIndex + (index - bitMapStartIndex >> shiftCount);
                if ((bitMap[bitsIndex] & 1L << index) == 0
                        || paint && uncompressedMaterials[materialIndex] == AIR
                        || replaceAir && uncompressedMaterials[materialIndex] != AIR
                        || breakHeldOnly && uncompressedMaterials[materialIndex] != heldMaterial) continue;
                uncompressedMaterials[materialIndex] = material;
            }
    }

    // Functions to store data into something
    /**
     * Expands a tree node into the destination buffer.
     *
     * @param uncompressedMaterials destination buffer indexed by local block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Fills uncompressed materials into.
 *
 * @param uncompressedMaterials parameter
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = getIdentifier(startIndex);

        if (identifier == HOMOGENOUS) {
            int size = 1 << sizeBits * 3;
            byte material = data[startIndex + 1];
            startIndex = getUncompressedIndex(inChunkX, inChunkY, inChunkZ);
            Arrays.fill(uncompressedMaterials, startIndex, startIndex + size, material);
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

    /**
     * Expands a clipped source region into the destination buffer, applying an LOD scale.
     *
     * @param uncompressedMaterials destination buffer indexed by local block coordinates
     * @param lod level of detail; one step equals {@code 1 << lod} source blocks
     * @param targetStart destination origin in local block coordinates
     * @param sourceStart source origin in source-local block coordinates
     * @param size size of the source region in source-local block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param currentX current node origin X in source-local block coordinates
     * @param currentY current node origin Y in source-local block coordinates
     * @param currentZ current node origin Z in source-local block coordinates
     */
/**
 * Fills uncompressed materials into.
 *
 * @param uncompressedMaterials parameter
 * @param lod parameter
 * @param targetStart 3D vector in local block coordinates
 * @param sourceStart 3D vector in local block coordinates
 * @param size 3D vector in local block coordinates
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param currentX X coordinate in local block coordinates
 * @param currentY Y coordinate in local block coordinates
 * @param currentZ Z coordinate in local block coordinates
 */
    private void fillUncompressedMaterialsInto(byte[] uncompressedMaterials, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                               int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(lod, sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = getIdentifier(startIndex);

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

        int lengthX = Math.max(1, Math.min(currentX + length, sourceStart.x + size.x) - sourceStartX >> lod);
        int lengthY = Math.max(1, Math.min(currentY + length, sourceStart.y + size.y) - sourceStartY >> lod);
        int lengthZ = Math.max(1, Math.min(currentZ + length, sourceStart.z + size.z) - sourceStartZ >> lod);

        int targetStartX = targetStart.x + (sourceStartX - sourceStart.x >> lod);
        int targetStartY = targetStart.y + (sourceStartY - sourceStart.y >> lod);
        int targetStartZ = targetStart.z + (sourceStartZ - sourceStart.z >> lod);

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

    /**
     * Expands a transformed structure region into the destination buffer.
     *
     * @param uncompressedMaterials destination buffer indexed by local block coordinates
     * @param transform structure transform applied to source coordinates
     * @param lod level of detail; one step equals {@code 1 << lod} source blocks
     * @param targetStart destination origin in local block coordinates
     * @param sourceStart source origin in structure-local block coordinates
     * @param size source region size in structure-local block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param currentX current node origin X in structure-local block coordinates
     * @param currentY current node origin Y in structure-local block coordinates
     * @param currentZ current node origin Z in structure-local block coordinates
     */
/**
 * Fills structure materials into.
 *
 * @param uncompressedMaterials parameter
 * @param transform parameter
 * @param lod parameter
 * @param targetStart 3D vector in local block coordinates
 * @param sourceStart 3D vector in local block coordinates
 * @param size 3D vector in local block coordinates
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param currentX X coordinate in local block coordinates
 * @param currentY Y coordinate in local block coordinates
 * @param currentZ Z coordinate in local block coordinates
 */
    private void fillStructureMaterialsInto(byte[] uncompressedMaterials, byte transform, int lod, Vector3i targetStart, Vector3i sourceStart, Vector3i size,
                                            int sizeBits, int startIndex, int currentX, int currentY, int currentZ) {
        int length = 1 << sizeBits;
        if (isInValidCoordinate(lod, sourceStart, size, currentX, currentY, currentZ, length)) return;
        byte identifier = getIdentifier(startIndex);

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

        int lengthX = Math.max(1, Math.min(currentX + length, sourceStart.x + size.x) - sourceStartX >> lod);
        int lengthY = Math.max(1, Math.min(currentY + length, sourceStart.y + size.y) - sourceStartY >> lod);
        int lengthZ = Math.max(1, Math.min(currentZ + length, sourceStart.z + size.z) - sourceStartZ >> lod);

        int targetStartX = targetStart.x + (sourceStartX - sourceStart.x >> lod);
        int targetStartY = targetStart.y + (sourceStartY - sourceStart.y >> lod);
        int targetStartZ = targetStart.z + (sourceStartZ - sourceStart.z >> lod);

        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            if (material == AIR) return;
            for (int x = 0; x < lengthX; x++)
                for (int z = 0; z < lengthZ; z++)
                    for (int y = 0; y < lengthY; y++) {
                        int targetIndex = getUncompressedIndex(targetStartX + x, targetStartY + y, targetStartZ + z);
                        if ((Material.getProperties(uncompressedMaterials[targetIndex]) & STRUCTURE_REPLACEABLE) == 0) continue;
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
                    if ((Material.getProperties(uncompressedMaterials[targetIndex]) & STRUCTURE_REPLACEABLE) == 0) continue;
                    uncompressedMaterials[targetIndex] = material;
                }
    }

    /**
     * Extracts the south-facing 2D surface representation from the subtree.
     *
     * @param materials destination 2D payload
     * @param startIndex node offset inside {@link #data}
     * @return number of bytes written to {@code materials}
     */
    private int fillSouthLayerInto(ByteArrayList materials, int startIndex) {
        byte types = getTypes(startIndex);

        if (addSurfaceEquivalentHomogenous(materials, startIndex, types)) return HOMOGENOUS_BYTE_SIZE_2D;
        if (getIdentifier(startIndex) == DETAIL) {
            materials.add(DETAIL);
            materials.add(data[startIndex + 1]);
            materials.add(data[startIndex + 2]);
            materials.add(data[startIndex + 5]);
            materials.add(data[startIndex + 6]);
            return DETAIL_BYTE_SIZE_2D;
        }

        materials.add(SPLITTER);
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

    /**
     * Extracts the north-facing 2D surface representation from the subtree.
     *
     * @param materials destination 2D payload
     * @param startIndex node offset inside {@link #data}
     * @return number of bytes written to {@code materials}
     */
    private int fillNorthLayerInto(ByteArrayList materials, int startIndex) {
        byte types = getTypes(startIndex);

        if (addSurfaceEquivalentHomogenous(materials, startIndex, types)) return HOMOGENOUS_BYTE_SIZE_2D;
        if (getIdentifier(startIndex) == DETAIL) {
            materials.add(DETAIL);
            materials.add(data[startIndex + 3]);
            materials.add(data[startIndex + 4]);
            materials.add(data[startIndex + 7]);
            materials.add(data[startIndex + 8]);
            return DETAIL_BYTE_SIZE_2D;
        }

        materials.add(SPLITTER);
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

    /**
     * Extracts the bottom-facing 2D surface representation from the subtree.
     *
     * @param materials destination 2D payload
     * @param startIndex node offset inside {@link #data}
     * @return number of bytes written to {@code materials}
     */
    private int fillBottomLayerInto(ByteArrayList materials, int startIndex) {
        byte types = getTypes(startIndex);

        if (addSurfaceEquivalentHomogenous(materials, startIndex, types)) return HOMOGENOUS_BYTE_SIZE_2D;
        if (getIdentifier(startIndex) == DETAIL) {
            materials.add(DETAIL);
            materials.add(data[startIndex + 1]);
            materials.add(data[startIndex + 3]);
            materials.add(data[startIndex + 5]);
            materials.add(data[startIndex + 7]);
            return DETAIL_BYTE_SIZE_2D;
        }

        materials.add(SPLITTER);
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

    /**
     * Extracts the top-facing 2D surface representation from the subtree.
     *
     * @param materials destination 2D payload
     * @param startIndex node offset inside {@link #data}
     * @return number of bytes written to {@code materials}
     */
    private int fillTopLayerInto(ByteArrayList materials, int startIndex) {
        byte types = getTypes(startIndex);

        if (addSurfaceEquivalentHomogenous(materials, startIndex, types)) return HOMOGENOUS_BYTE_SIZE_2D;
        if (getIdentifier(startIndex) == DETAIL) {
            materials.add(DETAIL);
            materials.add(data[startIndex + 2]);
            materials.add(data[startIndex + 4]);
            materials.add(data[startIndex + 6]);
            materials.add(data[startIndex + 8]);
            return DETAIL_BYTE_SIZE_2D;
        }

        materials.add(SPLITTER);
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

    /**
     * Extracts the east-facing 2D surface representation from the subtree.
     *
     * @param materials destination 2D payload
     * @param startIndex node offset inside {@link #data}
     * @return number of bytes written to {@code materials}
     */
    private int fillEastLayerInto(ByteArrayList materials, int startIndex) {
        byte types = getTypes(startIndex);

        if (addSurfaceEquivalentHomogenous(materials, startIndex, types)) return HOMOGENOUS_BYTE_SIZE_2D;
        if (getIdentifier(startIndex) == DETAIL) {
            materials.add(DETAIL);
            materials.add(data[startIndex + 1]);
            materials.add(data[startIndex + 2]);
            materials.add(data[startIndex + 3]);
            materials.add(data[startIndex + 4]);
            return DETAIL_BYTE_SIZE_2D;
        }

        materials.add(SPLITTER);
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

    /**
     * Extracts the west-facing 2D surface representation from the subtree.
     *
     * @param materials destination 2D payload
     * @param startIndex node offset inside {@link #data}
     * @return number of bytes written to {@code materials}
     */
    private int fillWestLayerInto(ByteArrayList materials, int startIndex) {
        byte types = getTypes(startIndex);

        if (addSurfaceEquivalentHomogenous(materials, startIndex, types)) return HOMOGENOUS_BYTE_SIZE_2D;
        if (getIdentifier(startIndex) == DETAIL) {
            materials.add(DETAIL);
            materials.add(data[startIndex + 5]);
            materials.add(data[startIndex + 6]);
            materials.add(data[startIndex + 7]);
            materials.add(data[startIndex + 8]);
            return DETAIL_BYTE_SIZE_2D;
        }

        materials.add(SPLITTER);
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

    /**
     * Emits placement particles for one subtree node.
     *
     * @param collector particle sink
     * @param bitMap occupancy bitmap in local block coordinates
     * @param transform structure transform applied to emitted particle positions
     * @param lengths local block dimensions used by the particle placement code
     * @param opaque particle bucket for non-glass materials
     * @param transparent particle bucket for glass-like materials
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Adds place particles.
 *
 * @param collector parameter
 * @param bitMap parameter
 * @param transform parameter
 * @param lengths 3D vector in local block coordinates
 * @param opaque parameter
 * @param transparent parameter
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void addPlaceParticles(ParticleCollector collector, long[] bitMap, byte transform, Vector3i lengths, IntArrayList opaque, IntArrayList transparent,
                                   int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        int identifier = getIdentifier(startIndex);

        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            if (material == AIR) return;

            IntArrayList materialList = Material.isGlass(material) ? transparent : opaque;
            int size = 1 << sizeBits;
            int stepLength = IntSettings.PLACE_PARTICLE_STEP_LENGTH.value();

            for (int xOffset = inChunkX; xOffset < inChunkX + size; xOffset += stepLength)
                for (int yOffset = inChunkY; yOffset < inChunkY + size; yOffset += stepLength)
                    for (int zOffset = inChunkZ; zOffset < inChunkZ + size; zOffset += stepLength) {
                        collector.addPlaceParticle(materialList, bitMap,
                                lengths.x, lengths.y, lengths.z,
                                xOffset, yOffset, zOffset, material, transform);
                    }
            return;
        }

        if (identifier == DETAIL) {
            int stepLength = IntSettings.PLACE_PARTICLE_STEP_LENGTH.value() == 1 ? 1 : 8;
            for (int inDetailIndex = 0; inDetailIndex < 8; inDetailIndex += stepLength) {
                byte material = data[startIndex + 1 + inDetailIndex];
                if (material == AIR) continue;
                collector.addPlaceParticle(Material.isGlass(material) ? transparent : opaque, bitMap,
                        lengths.x, lengths.y, lengths.z,
                        inChunkX + (inDetailIndex >> 2 & 1), inChunkY + (inDetailIndex >> 1 & 1), inChunkZ + (inDetailIndex & 1),
                        material, transform);
            }
            return;
        }

//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        addPlaceParticles(collector, bitMap, transform, lengths, opaque, transparent, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
    }

    /**
     * Marks all occupied coordinates of one subtree node in the bitmap.
     *
     * @param bitMap destination bitmap in local block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Fills bit map.
 *
 * @param bitMap parameter
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void fillBitMap(long[] bitMap, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte types = getTypes(startIndex);
        if (types == CONTAINS_TRANSPARENT) return;

        if ((types & CONTAINS_TRANSPARENT) == 0) {
            int bitMapStartIndex = getUncompressedIndex(inChunkX, inChunkY, inChunkZ);
            if (sizeBits > 2) {
                int count = 1 << (sizeBits - 2) * 3;
                Arrays.fill(bitMap, bitMapStartIndex >> 6, (bitMapStartIndex >> 6) + count, -1L);
            } else {
                long mask = getMask(1 << sizeBits * 3, bitMapStartIndex);
                bitMap[bitMapStartIndex >> 6] |= mask;
            }
            return;
        }

        byte identifier = getIdentifier(startIndex);
        if (identifier == HOMOGENOUS) return;

        if (identifier == DETAIL) {
            if (data[startIndex + 1] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 0, inChunkY + 0, inChunkZ + 0));
            if (data[startIndex + 2] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 0, inChunkY + 0, inChunkZ + 1));
            if (data[startIndex + 3] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 0, inChunkY + 1, inChunkZ + 0));
            if (data[startIndex + 4] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 0, inChunkY + 1, inChunkZ + 1));
            if (data[startIndex + 5] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 1, inChunkY + 0, inChunkZ + 0));
            if (data[startIndex + 6] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 1, inChunkY + 0, inChunkZ + 1));
            if (data[startIndex + 7] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 1, inChunkY + 1, inChunkZ + 0));
            if (data[startIndex + 8] != AIR) setBit(bitMap, getUncompressedIndex(inChunkX + 1, inChunkY + 1, inChunkZ + 1));
            return;
        }

//        if (identifier == SPLITTER)
        int nextSize = 1 << --sizeBits;
        fillBitMap(bitMap, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
        fillBitMap(bitMap, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
        fillBitMap(bitMap, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
        fillBitMap(bitMap, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
        fillBitMap(bitMap, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
        fillBitMap(bitMap, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
        fillBitMap(bitMap, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
        fillBitMap(bitMap, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
    }

    // Helper functions
    /**
     * Reads a 24-bit offset from the compressed 3D payload stored in this instance.
     *
     * @param index first byte of the stored offset inside {@link #data}
     * @return decoded byte offset
     */
    private int getOffset(int index) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 | data[index + 2] & 0xFF;
    }

    /**
     * Resolves a child offset after applying the structure transform.
     *
     * @param startIndex node offset inside {@link #data}
     * @param transform structure transform bit mask
     * @param intend child selector bits in local node coordinates
     * @return byte offset to the selected child node
     */
    private int getOffset(int startIndex, byte transform, int intend) {
        if ((transform & Structure.MIRROR_X) != 0) intend ^= 0b100;
        if ((transform & Structure.MIRROR_Z) != 0) intend ^= 0b001;
        if ((transform & Structure.ROTATE_90) != 0) intend = (~intend & 0b001) << 2 | intend & 0b010 | intend >> 2;
        if (intend == 0) return SPLITTER_BYTE_SIZE;
        return getOffset(startIndex - 2 + 3 * intend);
    }

    /**
     * Resolves a child offset for a 3D position inside a splitter node.
     *
     * @param splitterIndex node offset inside {@link #data}
     * @param inChunkX local X coordinate inside the node
     * @param inChunkY local Y coordinate inside the node
     * @param inChunkZ local Z coordinate inside the node
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return byte offset to the selected child node
     */
/**
 * Returns the offset.
 *
 * @param splitterIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param sizeBits parameter
 * @return result
 */
    private int getOffset(int splitterIndex, int inChunkX, int inChunkY, int inChunkZ, int sizeBits) {
        int inSplitterIndex = getInSplitterIndex(inChunkX, inChunkY, inChunkZ, sizeBits);
        if (inSplitterIndex == 0) return SPLITTER_BYTE_SIZE;
        return getOffset(splitterIndex + inSplitterIndex - 2);
    }

    /**
     * Locates the smallest node that covers a local block position at the requested target size.
     *
     * @param inChunkX local X coordinate inside the current volume
     * @param inChunkY local Y coordinate inside the current volume
     * @param inChunkZ local Z coordinate inside the current volume
     * @param targetSizeBits minimum node size to descend to
     * @return byte offset of the selected node
     */
/**
 * Performs start index of.
 *
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param targetSizeBits parameter
 * @return result
 */
    private int startIndexOf(int inChunkX, int inChunkY, int inChunkZ, int targetSizeBits) {
        int index = 0, sizeBits = totalSizeBits;
        while (true) { // Scary but should be fine
            byte identifier = getIdentifier(index);
            if (sizeBits <= targetSizeBits || identifier == HOMOGENOUS || identifier == DETAIL) return index;
//            if (identifier == SPLITTER)
            index += getOffset(index, inChunkX, inChunkY, inChunkZ, --sizeBits);
        }
    }

    /**
     * Reads the low 4 bits of a node header as the node identifier.
     *
     * @param startIndex node offset inside {@link #data}
     * @return node identifier
     */
    private byte getIdentifier(int startIndex) {
        return (byte) (data[startIndex] & IDENTIFIER_MASK);
    }

    /**
     * Reads the high 4 bits of a node header as type flags.
     *
     * @param startIndex node offset inside {@link #data}
     * @return node type flags
     */
    private byte getTypes(int startIndex) {
        return (byte) (data[startIndex] & TYPE_MASK);
    }

    // Mesh generation for chunks
    /**
     * Collapses this tree to a surface-only equivalent used for chunk mesh generation.
     *
     * @return compressed tree where interior air/solid regions are replaced by surface-equivalent materials
     */
    private MaterialsData getSurfaceEquivalent() {
        ByteArrayList dataList = new ByteArrayList(1000);
        synchronized (this) {
            getSurfaceEquivalent(dataList, totalSizeBits, 0);
        }
        return new MaterialsData(totalSizeBits, dataList.toArray());
    }

    /**
     * Writes the surface-equivalent form of one subtree node.
     *
     * @param materials destination payload
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @return number of bytes written to {@code materials}
     */
    private int getSurfaceEquivalent(ByteArrayList materials, int sizeBits, int startIndex) {
        byte types = getTypes(startIndex);

        if (addSurfaceEquivalentHomogenous(materials, startIndex, types)) return HOMOGENOUS_BYTE_SIZE;
        if (sizeBits == 1) {
            materials.add(DETAIL);
            materials.add(data[startIndex + 1]);
            materials.add(data[startIndex + 2]);
            materials.add(data[startIndex + 3]);
            materials.add(data[startIndex + 4]);
            materials.add(data[startIndex + 5]);
            materials.add(data[startIndex + 6]);
            materials.add(data[startIndex + 7]);
            materials.add(data[startIndex + 8]);
            return DETAIL_BYTE_SIZE;
        }

        sizeBits--;
        int offset = SPLITTER_BYTE_SIZE, size = materials.size();
        materials.add(SPLITTER);
        materials.pad(SPLITTER_BYTE_SIZE - 1);

        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + SPLITTER_BYTE_SIZE);
        setOffset(materials, offset, size + 1);
        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + getOffset(startIndex + 1));
        setOffset(materials, offset, size + 4);
        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + getOffset(startIndex + 4));
        setOffset(materials, offset, size + 7);
        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + getOffset(startIndex + 7));
        setOffset(materials, offset, size + 10);
        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + getOffset(startIndex + 10));
        setOffset(materials, offset, size + 13);
        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + getOffset(startIndex + 13));
        setOffset(materials, offset, size + 16);
        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + getOffset(startIndex + 16));
        setOffset(materials, offset, size + 19);
        offset += getSurfaceEquivalent(materials, sizeBits, startIndex + getOffset(startIndex + 19));
        return offset;
    }

    /**
     * Emits a homogeneous surface-equivalent node when the node type allows it.
     *
     * @param materials destination payload
     * @param startIndex node offset inside {@link #data}
     * @param types cached type flags for the node
     * @return {@code true} if a homogeneous representation was written
     */
    private boolean addSurfaceEquivalentHomogenous(ByteArrayList materials, int startIndex, byte types) {
        if (types == CONTAINS_TRANSPARENT) {
            materials.add(HOMOGENOUS);
            materials.add(AIR);
            return true;
        }
        if (types == CONTAINS_OPAQUE) {
            materials.add(HOMOGENOUS);
            materials.add(MeshGenerator.OPAQUE);
            return true;
        }
        if (types == CONTAINS_SELF_OCCLUDING && getIdentifier(startIndex) == HOMOGENOUS) {
            materials.add(HOMOGENOUS);
            materials.add(data[startIndex + 1]);
            return true;
        }
        return false;
    }

    /**
     * Builds face visibility maps for one subtree node using adjacent chunk layers.
     *
     * @param toMeshFacesMaps destination face maps indexed by face direction
     * @param uncompressedMaterials destination scratch buffer indexed by local block coordinates
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces maps.
 *
 * @param toMeshFacesMaps parameter
 * @param uncompressedMaterials parameter
 * @param adjacentChunkLayers parameter
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, byte[][] adjacentChunkLayers, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = getIdentifier(startIndex);

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

    /**
     * Builds the north-facing face mask for one subtree node, including adjacent chunk lookups.
     *
     * @param toMeshFacesMap destination face map
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous north layer.
 *
 * @param toMeshFacesMap parameter
 * @param adjacentChunkLayers parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
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

    /**
     * Builds the north-facing mask for a subtree node fully inside the current chunk.
     *
     * @param toMeshFacesMap destination face map
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     */
/**
 * Generates to mesh faces homogenous north layer inside.
 *
 * @param toMeshFacesMap parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 */
    private void generateToMeshFacesHomogenousNorthLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkY) {
        byte identifier = getIdentifier(startIndex);
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

    /**
     * Builds the south-facing face mask for one subtree node, including adjacent chunk lookups.
     *
     * @param toMeshFacesMap destination face map
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous south layer.
 *
 * @param toMeshFacesMap parameter
 * @param adjacentChunkLayers parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
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

    /**
     * Builds the south-facing mask for a subtree node fully inside the current chunk.
     *
     * @param toMeshFacesMap destination face map
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     */
/**
 * Generates to mesh faces homogenous south layer inside.
 *
 * @param toMeshFacesMap parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 */
    private void generateToMeshFacesHomogenousSouthLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkY) {
        byte identifier = getIdentifier(startIndex);
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

    /**
     * Builds the top-facing face mask for one subtree node, including adjacent chunk lookups.
     *
     * @param toMeshFacesMap destination face map
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous top layer.
 *
 * @param toMeshFacesMap parameter
 * @param adjacentChunkLayers parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
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

    /**
     * Builds the top-facing mask for a subtree node fully inside the current chunk.
     *
     * @param toMeshFacesMap destination face map
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous top layer inside.
 *
 * @param toMeshFacesMap parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void generateToMeshFacesHomogenousTopLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkZ) {
        byte identifier = getIdentifier(startIndex);
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

    /**
     * Builds the bottom-facing face mask for one subtree node, including adjacent chunk lookups.
     *
     * @param toMeshFacesMap destination face map
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous bottom layer.
 *
 * @param toMeshFacesMap parameter
 * @param adjacentChunkLayers parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
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

    /**
     * Builds the bottom-facing mask for a subtree node fully inside the current chunk.
     *
     * @param toMeshFacesMap destination face map
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous bottom layer inside.
 *
 * @param toMeshFacesMap parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void generateToMeshFacesHomogenousBottomLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkX, int inChunkZ) {
        byte identifier = getIdentifier(startIndex);
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

    /**
     * Builds the west-facing face mask for one subtree node, including adjacent chunk lookups.
     *
     * @param toMeshFacesMap destination face map
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous west layer.
 *
 * @param toMeshFacesMap parameter
 * @param adjacentChunkLayers parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
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

    /**
     * Builds the west-facing mask for a subtree node fully inside the current chunk.
     *
     * @param toMeshFacesMap destination face map
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param startIndex node offset inside {@link #data}
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous west layer inside.
 *
 * @param toMeshFacesMap parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void generateToMeshFacesHomogenousWestLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkY, int inChunkZ) {
        byte identifier = getIdentifier(startIndex);
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

    /**
     * Builds the east-facing face mask for one subtree node, including adjacent chunk lookups.
     *
     * @param toMeshFacesMap destination face map
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous east layer.
 *
 * @param toMeshFacesMap parameter
 * @param adjacentChunkLayers parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
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

    /**
     * Builds the east-facing mask for a subtree node fully inside the current chunk.
     *
     * @param toMeshFacesMap destination face map
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param startIndex node offset inside {@link #data}
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous east layer inside.
 *
 * @param toMeshFacesMap parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void generateToMeshFacesHomogenousEastLayerInside(long[] toMeshFacesMap, int sizeBits, byte material, int startIndex, int inChunkY, int inChunkZ) {
        byte identifier = getIdentifier(startIndex);
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

    /**
     * Builds a row/column face mask for a homogeneous node against an adjacent layer.
     *
     * @param toMeshFacesMap destination face map
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param occludingMaterial adjacent material used for visibility testing
     * @param inChunkA first local coordinate axis
     * @param inChunkB second local coordinate axis
     */
/**
 * Fills to mesh faces map homogenous.
 *
 * @param toMeshFacesMap parameter
 * @param sizeBits parameter
 * @param material parameter
 * @param occludingMaterial parameter
 * @param inChunkA parameter
 * @param inChunkB parameter
 */
    private static void fillToMeshFacesMapHomogenous(long[] toMeshFacesMap, int sizeBits, byte material, byte occludingMaterial, int inChunkA, int inChunkB) {
        if (!MeshGenerator.isVisible(material, occludingMaterial)) return;
        int length = 1 << sizeBits;
        long mask = getMask(length, inChunkB);
        for (int a = inChunkA; a < inChunkA + length; a++) toMeshFacesMap[a] |= mask;
    }

    /**
     * Builds face visibility maps for an eight-cell leaf node.
     *
     * @param toMeshFacesMap destination face maps indexed by face direction
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param inChunkX local X coordinate inside the current chunk
     * @param inChunkY local Y coordinate inside the current chunk
     * @param inChunkZ local Z coordinate inside the current chunk
     */
/**
 * Generates to mesh faces detail.
 *
 * @param toMeshFacesMap parameter
 * @param uncompressedMaterials parameter
 * @param adjacentChunkLayers parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
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

    /**
     * Reads a material from either the local buffer or one adjacent chunk face.
     *
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param adjacentChunkLayers 2D materials for adjacent chunk faces in face-order
     * @param inChunkX local X coordinate, or {@code -1}/{@code CHUNK_SIZE} for east/west lookups
     * @param inChunkY local Y coordinate, or {@code -1}/{@code CHUNK_SIZE} for bottom/top lookups
     * @param inChunkZ local Z coordinate, or {@code -1}/{@code CHUNK_SIZE} for south/north lookups
     * @return material at the requested local or adjacent coordinate
     */
/**
 * Returns the material.
 *
 * @param uncompressedMaterials parameter
 * @param adjacentChunkLayers parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @return result
 */
    private static byte getMaterial(byte[] uncompressedMaterials, byte[][] adjacentChunkLayers, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkX == -1) return getMaterial2D(adjacentChunkLayers[EAST], inChunkZ, inChunkY);
        if (inChunkX == CHUNK_SIZE) return getMaterial2D(adjacentChunkLayers[WEST], inChunkZ, inChunkY);
        if (inChunkY == -1) return getMaterial2D(adjacentChunkLayers[BOTTOM], inChunkX, inChunkZ);
        if (inChunkY == CHUNK_SIZE) return getMaterial2D(adjacentChunkLayers[TOP], inChunkX, inChunkZ);
        if (inChunkZ == -1) return getMaterial2D(adjacentChunkLayers[SOUTH], inChunkX, inChunkY);
        if (inChunkZ == CHUNK_SIZE) return getMaterial2D(adjacentChunkLayers[NORTH], inChunkX, inChunkY);

        return uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)];
    }

    /**
     * Builds a face mask for a 2D layer stored in an adjacent chunk.
     *
     * @param toMeshFacesMap destination face map
     * @param adjacentChunkLayer compressed 2D layer payload
     * @param startIndex node offset inside {@code adjacentChunkLayer}
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param material source material in the current node
     * @param inChunkA first local coordinate axis
     * @param inChunkB second local coordinate axis
     */
/**
 * Generates to mesh faces homogenous side layer.
 *
 * @param toMeshFacesMap parameter
 * @param adjacentChunkLayer parameter
 * @param startIndex X coordinate in local block coordinates
 * @param sizeBits parameter
 * @param material parameter
 * @param inChunkA parameter
 * @param inChunkB parameter
 */
    private static void generateToMeshFacesHomogenousSideLayer(long[] toMeshFacesMap, byte[] adjacentChunkLayer, int startIndex, int sizeBits, byte material, int inChunkA, int inChunkB) {
        byte identifier = (byte) (adjacentChunkLayer[startIndex] & IDENTIFIER_MASK);
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

    // Mesh generation for structures
    /**
     * Builds face visibility maps for a structure subtree.
     *
     * @param toMeshFacesMaps destination face maps indexed by face direction
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces maps.
 *
 * @param toMeshFacesMaps parameter
 * @param uncompressedMaterials parameter
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void generateToMeshFacesMaps(long[][][] toMeshFacesMaps, byte[] uncompressedMaterials, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        byte identifier = getIdentifier(startIndex);

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
            generateToMeshFacesMaps(toMeshFacesMaps, uncompressedMaterials, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            if (material == AIR) return;
            int length = 1 << sizeBits;

            generateToMeshFacesHomogenousNorthSouthLayer(toMeshFacesMaps[NORTH][inChunkZ + length - 1], uncompressedMaterials, length, material, inChunkX, inChunkY, inChunkZ + length);
            generateToMeshFacesHomogenousTopBottomLayer(toMeshFacesMaps[TOP][inChunkY + length - 1], uncompressedMaterials, length, material, inChunkX, inChunkY + length, inChunkZ);
            generateToMeshFacesHomogenousWestEastLayer(toMeshFacesMaps[WEST][inChunkX + length - 1], uncompressedMaterials, length, material, inChunkX + length, inChunkY, inChunkZ);
            generateToMeshFacesHomogenousNorthSouthLayer(toMeshFacesMaps[SOUTH][inChunkZ], uncompressedMaterials, length, material, inChunkX, inChunkY, inChunkZ - 1);
            generateToMeshFacesHomogenousTopBottomLayer(toMeshFacesMaps[BOTTOM][inChunkY], uncompressedMaterials, length, material, inChunkX, inChunkY - 1, inChunkZ);
            generateToMeshFacesHomogenousWestEastLayer(toMeshFacesMaps[EAST][inChunkX], uncompressedMaterials, length, material, inChunkX - 1, inChunkY, inChunkZ);
            return;
        }

//        if (identifier == DETAIL)
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX, inChunkY, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX, inChunkY, inChunkZ + 1);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX, inChunkY + 1, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX, inChunkY + 1, inChunkZ + 1);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX + 1, inChunkY, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX + 1, inChunkY, inChunkZ + 1);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX + 1, inChunkY + 1, inChunkZ);
        generateToMeshFacesDetail(toMeshFacesMaps, uncompressedMaterials, inChunkX + 1, inChunkY + 1, inChunkZ + 1);
    }

    /**
     * Builds the north/south face mask for a homogeneous node.
     *
     * @param toMeshFacesMap destination face map indexed by the orthogonal axis
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param length edge length of the node in block coordinates
     * @param material source material in the node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ face coordinate in local block coordinates
     */
/**
 * Generates to mesh faces homogenous north south layer.
 *
 * @param toMeshFacesMap parameter
 * @param uncompressedMaterials parameter
 * @param length parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private static void generateToMeshFacesHomogenousNorthSouthLayer(long[] toMeshFacesMap, byte[] uncompressedMaterials, int length, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        for (int x = inChunkX; x < inChunkX + length; x++) {
            long map = toMeshFacesMap[x];
            for (int y = inChunkY; y < inChunkY + length; y++) {
                byte occludingMaterial = getMaterial(uncompressedMaterials, x, y, inChunkZ);
                if (MeshGenerator.isVisible(material, occludingMaterial)) map |= 1L << y;
            }
            toMeshFacesMap[x] = map;
        }
    }

    /**
     * Builds the top/bottom face mask for a homogeneous node.
     *
     * @param toMeshFacesMap destination face map indexed by the orthogonal axis
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param length edge length of the node in block coordinates
     * @param material source material in the node
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY face coordinate in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous top bottom layer.
 *
 * @param toMeshFacesMap parameter
 * @param uncompressedMaterials parameter
 * @param length parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private static void generateToMeshFacesHomogenousTopBottomLayer(long[] toMeshFacesMap, byte[] uncompressedMaterials, int length, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        for (int x = inChunkX; x < inChunkX + length; x++) {
            long map = toMeshFacesMap[x];
            for (int z = inChunkZ; z < inChunkZ + length; z++) {
                byte occludingMaterial = getMaterial(uncompressedMaterials, x, inChunkY, z);
                if (MeshGenerator.isVisible(material, occludingMaterial)) map |= 1L << z;
            }
            toMeshFacesMap[x] = map;
        }
    }

    /**
     * Builds the west/east face mask for a homogeneous node.
     *
     * @param toMeshFacesMap destination face map indexed by the orthogonal axis
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param length edge length of the node in block coordinates
     * @param material source material in the node
     * @param inChunkX face coordinate in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Generates to mesh faces homogenous west east layer.
 *
 * @param toMeshFacesMap parameter
 * @param uncompressedMaterials parameter
 * @param length parameter
 * @param material parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private static void generateToMeshFacesHomogenousWestEastLayer(long[] toMeshFacesMap, byte[] uncompressedMaterials, int length, byte material, int inChunkX, int inChunkY, int inChunkZ) {
        for (int z = inChunkZ; z < inChunkZ + length; z++) {
            long map = toMeshFacesMap[z];
            for (int y = inChunkY; y < inChunkY + length; y++) {
                byte occludingMaterial = getMaterial(uncompressedMaterials, inChunkX, y, z);
                if (MeshGenerator.isVisible(material, occludingMaterial)) map |= 1L << y;
            }
            toMeshFacesMap[z] = map;
        }
    }

    /**
     * Builds face visibility for one detailed leaf cell.
     *
     * @param toMeshFacesMap destination face maps indexed by face direction
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param inChunkX local X coordinate inside the current chunk
     * @param inChunkY local Y coordinate inside the current chunk
     * @param inChunkZ local Z coordinate inside the current chunk
     */
/**
 * Generates to mesh faces detail.
 *
 * @param toMeshFacesMap parameter
 * @param uncompressedMaterials parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private static void generateToMeshFacesDetail(long[][][] toMeshFacesMap, byte[] uncompressedMaterials, int inChunkX, int inChunkY, int inChunkZ) {
        byte material = uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)];
        if (material == AIR) return;

        byte northMaterial = getMaterial(uncompressedMaterials, inChunkX, inChunkY, inChunkZ + 1);
        byte topMaterial = getMaterial(uncompressedMaterials, inChunkX, inChunkY + 1, inChunkZ);
        byte westMaterial = getMaterial(uncompressedMaterials, inChunkX + 1, inChunkY, inChunkZ);
        byte southMaterial = getMaterial(uncompressedMaterials, inChunkX, inChunkY, inChunkZ - 1);
        byte bottomMaterial = getMaterial(uncompressedMaterials, inChunkX, inChunkY - 1, inChunkZ);
        byte eastMaterial = getMaterial(uncompressedMaterials, inChunkX - 1, inChunkY, inChunkZ);

        if (MeshGenerator.isVisible(material, northMaterial)) toMeshFacesMap[NORTH][inChunkZ][inChunkX] |= 1L << inChunkY;
        if (MeshGenerator.isVisible(material, topMaterial)) toMeshFacesMap[TOP][inChunkY][inChunkX] |= 1L << inChunkZ;
        if (MeshGenerator.isVisible(material, westMaterial)) toMeshFacesMap[WEST][inChunkX][inChunkZ] |= 1L << inChunkY;
        if (MeshGenerator.isVisible(material, southMaterial)) toMeshFacesMap[SOUTH][inChunkZ][inChunkX] |= 1L << inChunkY;
        if (MeshGenerator.isVisible(material, bottomMaterial)) toMeshFacesMap[BOTTOM][inChunkY][inChunkX] |= 1L << inChunkZ;
        if (MeshGenerator.isVisible(material, eastMaterial)) toMeshFacesMap[EAST][inChunkX][inChunkZ] |= 1L << inChunkY;
    }

    /**
     * Reads a material from local or adjacent chunk coordinates.
     *
     * @param uncompressedMaterials source buffer indexed by local block coordinates
     * @param inChunkX local X coordinate, or one cell outside the chunk for neighbor lookups
     * @param inChunkY local Y coordinate, or one cell outside the chunk for neighbor lookups
     * @param inChunkZ local Z coordinate, or one cell outside the chunk for neighbor lookups
     * @return material at the requested coordinate, or air when outside the chunk bounds
     */
/**
 * Returns the material.
 *
 * @param uncompressedMaterials parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @return result
 */
    private static byte getMaterial(byte[] uncompressedMaterials, int inChunkX, int inChunkY, int inChunkZ) {
        if (inChunkX == -1 || inChunkX == CHUNK_SIZE || inChunkY == -1 || inChunkY == CHUNK_SIZE || inChunkZ == -1 || inChunkZ == CHUNK_SIZE) return AIR;

        return uncompressedMaterials[getUncompressedIndex(inChunkX, inChunkY, inChunkZ)];
    }

    // AABB generation
    /**
     * Expands a candidate occluder AABB by descending the tree where it still intersects.
     *
     * @param aabb candidate box in local block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Returns the occluder.
 *
 * @param aabb parameter
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void getOccluder(AABB aabb, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        int size = 1 << sizeBits;
        if (!aabb.intersects(inChunkX, inChunkY, inChunkZ, inChunkX + size, inChunkY + size, inChunkZ + size)) return;
        byte identifier = getIdentifier(startIndex);

        if (identifier == SPLITTER) {
            int nextSize = 1 << --sizeBits;
            getOccluder(aabb, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
            getOccluder(aabb, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
            getOccluder(aabb, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
            getOccluder(aabb, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
            getOccluder(aabb, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
            getOccluder(aabb, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
            getOccluder(aabb, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
            getOccluder(aabb, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
            return;
        }
        if (identifier == HOMOGENOUS) {
            byte material = data[startIndex + 1];
            if (Properties.doesntHaveProperties(material, TRANSPARENT)) return;
            aabb.excludeMaximizeSurfaceArea(inChunkX, inChunkY, inChunkZ, size);
        }
    }

    /**
     * Tracks the largest fully opaque AABB reachable from the current subtree.
     *
     * @param aabb best box found so far, in local block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @param startIndex node offset inside {@link #data}
     * @param inChunkX node origin X in local block coordinates
     * @param inChunkY node origin Y in local block coordinates
     * @param inChunkZ node origin Z in local block coordinates
     */
/**
 * Returns the largest opaque aabb.
 *
 * @param aabb parameter
 * @param sizeBits parameter
 * @param startIndex X coordinate in local block coordinates
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 */
    private void getLargestOpaqueAABB(AABB aabb, int sizeBits, int startIndex, int inChunkX, int inChunkY, int inChunkZ) {
        int size = 1 << sizeBits;
        if (size <= aabb.maxX - aabb.minX) return;
        byte types = getTypes(startIndex);
        if ((types & CONTAINS_OPAQUE) == 0) return;

        if (types == CONTAINS_OPAQUE) {
            aabb.set(inChunkX, inChunkY, inChunkZ, inChunkX + size, inChunkY + size, inChunkZ + size);
            return;
        }

        byte identifier = getIdentifier(startIndex);
        int nextSize = 1 << --sizeBits;
        if (identifier == SPLITTER) {
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + SPLITTER_BYTE_SIZE, inChunkX, inChunkY, inChunkZ);
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + getOffset(startIndex + 1), inChunkX, inChunkY, inChunkZ + nextSize);
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + getOffset(startIndex + 4), inChunkX, inChunkY + nextSize, inChunkZ);
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + getOffset(startIndex + 7), inChunkX, inChunkY + nextSize, inChunkZ + nextSize);
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + getOffset(startIndex + 10), inChunkX + nextSize, inChunkY, inChunkZ);
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + getOffset(startIndex + 13), inChunkX + nextSize, inChunkY, inChunkZ + nextSize);
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + getOffset(startIndex + 16), inChunkX + nextSize, inChunkY + nextSize, inChunkZ);
            getLargestOpaqueAABB(aabb, sizeBits, startIndex + getOffset(startIndex + 19), inChunkX + nextSize, inChunkY + nextSize, inChunkZ + nextSize);
        }
    }

    /**
     * Expands the final AABB candidate in all three axes.
     *
     * @param aabb box in local block coordinates
     */
    private void expand(AABB aabb) {
        if (aabb.isEmpty() || aabb.isMaxChunk()) return;
        int size = aabb.maxX - aabb.minX;

        expandY(aabb, size);
        expandX(aabb, size);
        expandZ(aabb, size);
    }

    /**
     * Expands the box along Y while every touched slab remains fully opaque.
     *
     * @param aabb box in local block coordinates
     * @param size expansion step in block coordinates
     */
    private void expandY(AABB aabb, int size) {
        int sizeBits = Integer.numberOfTrailingZeros(size);

        while (aabb.maxY < CHUNK_SIZE && getTypes(startIndexOf(aabb.minX, aabb.maxY, aabb.minZ, sizeBits)) == CONTAINS_OPAQUE)
            aabb.maxY += size;
        while (aabb.minY > 0 && getTypes(startIndexOf(aabb.minX, aabb.minY - size, aabb.minZ, sizeBits)) == CONTAINS_OPAQUE)
            aabb.minY -= size;
    }

    /**
     * Expands the box along X while every touched slab remains fully opaque.
     *
     * @param aabb box in local block coordinates
     * @param size expansion step in block coordinates
     */
    private void expandX(AABB aabb, int size) {
        int sizeBits = Integer.numberOfTrailingZeros(size);

        while (aabb.maxX < CHUNK_SIZE && canExpandPosX(aabb, size, sizeBits))
            aabb.maxX += size;
        while (aabb.minX > 0 && canExpandNegX(aabb, size, sizeBits))
            aabb.minX -= size;
    }

    /**
     * Checks whether the box can grow in the positive X direction.
     *
     * @param aabb box in local block coordinates
     * @param size expansion step in block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return {@code true} if the positive X face can move outward
     */
    private boolean canExpandPosX(AABB aabb, int size, int sizeBits) {
        for (int y = aabb.minY; y < aabb.maxY; y += size)
            if (getTypes((startIndexOf(aabb.maxX, y, aabb.minZ, sizeBits))) != CONTAINS_OPAQUE) return false;
        return true;
    }

    /**
     * Checks whether the box can grow in the negative X direction.
     *
     * @param aabb box in local block coordinates
     * @param size expansion step in block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return {@code true} if the negative X face can move outward
     */
    private boolean canExpandNegX(AABB aabb, int size, int sizeBits) {
        for (int y = aabb.minY; y < aabb.maxY; y += size)
            if (getTypes((startIndexOf(aabb.minX - size, y, aabb.minZ, sizeBits))) != CONTAINS_OPAQUE) return false;
        return true;
    }

    /**
     * Expands the box along Z while every touched slab remains fully opaque.
     *
     * @param aabb box in local block coordinates
     * @param size expansion step in block coordinates
     */
    private void expandZ(AABB aabb, int size) {
        int sizeBits = Integer.numberOfTrailingZeros(size);

        while (aabb.maxZ < CHUNK_SIZE && canExpandPosZ(aabb, size, sizeBits))
            aabb.maxZ += size;
        while (aabb.minZ > 0 && canExpandNegZ(aabb, size, sizeBits))
            aabb.minZ -= size;
    }

    /**
     * Checks whether the box can grow in the positive Z direction.
     *
     * @param aabb box in local block coordinates
     * @param size expansion step in block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return {@code true} if the positive Z face can move outward
     */
    private boolean canExpandPosZ(AABB aabb, int size, int sizeBits) {
        for (int x = aabb.minX; x < aabb.maxX; x += size)
            for (int y = aabb.minY; y < aabb.maxY; y += size)
                if (getTypes((startIndexOf(x, y, aabb.maxZ, sizeBits))) != CONTAINS_OPAQUE) return false;
        return true;
    }

    /**
     * Checks whether the box can grow in the negative Z direction.
     *
     * @param aabb box in local block coordinates
     * @param size expansion step in block coordinates
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return {@code true} if the negative Z face can move outward
     */
    private boolean canExpandNegZ(AABB aabb, int size, int sizeBits) {
        for (int x = aabb.minX; x < aabb.maxX; x += size)
            for (int y = aabb.minY; y < aabb.maxY; y += size)
                if (getTypes((startIndexOf(x, y, aabb.minZ - size, sizeBits))) != CONTAINS_OPAQUE) return false;
        return true;
    }

    // Helper functions
    /**
     * Writes a 24-bit offset into a compressed byte array.
     *
     * @param data destination byte array
     * @param offset offset value to store
     * @param index first byte index where the offset is written
     */
    static void setOffset(ByteArrayList data, int offset, int index) {
        data.set((byte) (offset >> 16 & 0xFF), index);
        data.set((byte) (offset >> 8 & 0xFF), index + 1);
        data.set((byte) (offset & 0xFF), index + 2);
    }

    /**
     * Classifies a material into its occlusion type flags.
     *
     * @param material material identifier
     * @return occlusion type flags for that material
     */
    static byte getType(byte material) {
        if (material == AIR) return CONTAINS_TRANSPARENT;
        int properties = Material.getProperties(material);
        return (properties & OCCLUDES_SELF_ONLY) != 0 ? CONTAINS_SELF_OCCLUDING : CONTAINS_OPAQUE;
    }

    /**
     * Computes the combined type flags of the eight children of a splitter node.
     *
     * @param data compressed payload
     * @param startIndex node offset inside {@code data}
     * @return combined child type flags
     */
    static byte getSplitterTypes(ByteArrayList data, int startIndex) {
        byte[] array = data.getData();
        return (byte) ((array[startIndex + SPLITTER_BYTE_SIZE]
                | array[startIndex + getOffset(array, startIndex + 1)]
                | array[startIndex + getOffset(array, startIndex + 4)]
                | array[startIndex + getOffset(array, startIndex + 7)]
                | array[startIndex + getOffset(array, startIndex + 10)]
                | array[startIndex + getOffset(array, startIndex + 13)]
                | array[startIndex + getOffset(array, startIndex + 16)]
                | array[startIndex + getOffset(array, startIndex + 19)]) & TYPE_MASK);
    }


    /**
     * Rejects coordinates that fall outside the requested clipped region or do not satisfy the LOD alignment.
     *
     * @param lod required level-of-detail alignment
     * @param sourceStart source-region origin in source-local block coordinates
     * @param size source-region size in source-local block coordinates
     * @param currentX current node origin X in source-local block coordinates
     * @param currentY current node origin Y in source-local block coordinates
     * @param currentZ current node origin Z in source-local block coordinates
     * @param length current node edge length in source-local block coordinates
     * @return {@code true} if the node does not contribute to the requested region
     */
/**
 * Checks whether in valid coordinate.
 *
 * @param lod parameter
 * @param sourceStart 3D vector in local block coordinates
 * @param size 3D vector in local block coordinates
 * @param currentX X coordinate in local block coordinates
 * @param currentY Y coordinate in local block coordinates
 * @param currentZ Z coordinate in local block coordinates
 * @param length parameter
 * @return true if the condition holds
 */
    private static boolean isInValidCoordinate(int lod, Vector3i sourceStart, Vector3i size, int currentX, int currentY, int currentZ, int length) {
        return Integer.numberOfTrailingZeros(currentX | currentY | currentZ) < lod
                || currentX + length <= sourceStart.x || sourceStart.x + size.x <= currentX
                || currentY + length <= sourceStart.y || sourceStart.y + size.y <= currentY
                || currentZ + length <= sourceStart.z || sourceStart.z + size.z <= currentZ;
    }

    /**
     * Maps local block coordinates to the 8-cell detail index inside a node.
     *
     * @param inChunkX local X coordinate inside the node
     * @param inChunkY local Y coordinate inside the node
     * @param inChunkZ local Z coordinate inside the node
     * @return detail-table index in the range {@code 1..8}
     */
    private static int getInDetailIndex(int inChunkX, int inChunkY, int inChunkZ) {
        return ((inChunkX & 1) << 2 | (inChunkZ & 1) << 1 | (inChunkY & 1)) + 1;
    }

    /**
     * Maps transformed local block coordinates to the 8-cell detail index inside a node.
     *
     * @param transform structure transform bit mask
     * @param inChunkX local X coordinate inside the transformed node
     * @param inChunkY local Y coordinate inside the transformed node
     * @param inChunkZ local Z coordinate inside the transformed node
     * @return detail-table index in the range {@code 1..8}
     */
/**
 * Returns the in detail index.
 *
 * @param transform parameter
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @return result
 */
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

    /**
     * Maps local block coordinates to the child offset table of a 3D splitter node.
     *
     * @param inChunkX local X coordinate inside the node
     * @param inChunkY local Y coordinate inside the node
     * @param inChunkZ local Z coordinate inside the node
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return child offset index used inside the compressed byte array
     */
    private static int getInSplitterIndex(int inChunkX, int inChunkY, int inChunkZ, int sizeBits) {
        return 3 * ((inChunkX >> sizeBits & 1) << 2 | (inChunkY >> sizeBits & 1) << 1 | (inChunkZ >> sizeBits & 1));
    }

    /**
     * Maps local coordinates to the child offset table of a 2D splitter node.
     *
     * @param inChunkA first 2D axis coordinate
     * @param inChunkB second 2D axis coordinate
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return child offset index used inside the compressed byte array
     */
    private static int getInSplitterIndex2D(int inChunkA, int inChunkB, int sizeBits) {
        return 3 * ((inChunkA >> sizeBits & 1) << 1 | (inChunkB >> sizeBits & 1));
    }

    /**
     * Maps local 2D coordinates to the detail table inside a 2D node.
     *
     * @param inChunkA first 2D axis coordinate
     * @param inChunkB second 2D axis coordinate
     * @return detail-table index in the range {@code 1..4}
     */
    private static int getInDetailIndex2D(int inChunkA, int inChunkB) {
        return ((inChunkA & 1) << 1 | (inChunkB & 1)) + 1;
    }

    /**
     * Resolves the compressed offset for a 2D child selection.
     *
     * @param data compressed payload
     * @param splitterIndex offset of the current splitter node
     * @param inChunkA first 2D axis coordinate
     * @param inChunkB second 2D axis coordinate
     * @param sizeBits log2 of the current node edge length in block coordinates
     * @return byte offset to the selected child node
     */
/**
 * Returns the offset2d.
 *
 * @param data parameter
 * @param splitterIndex X coordinate in local block coordinates
 * @param inChunkA parameter
 * @param inChunkB parameter
 * @param sizeBits parameter
 * @return result
 */
    private static int getOffset2D(byte[] data, int splitterIndex, int inChunkA, int inChunkB, int sizeBits) {
        int inSplitterIndex = getInSplitterIndex2D(inChunkA, inChunkB, sizeBits);
        if (inSplitterIndex == 0) return SPLITTER_BYTE_SIZE_2D;
        return getOffset2D(data, splitterIndex + inSplitterIndex - 2);
    }

    /**
     * Reads a 24-bit offset from a 2D compressed payload.
     *
     * @param data compressed payload
     * @param index first byte of the stored offset
     * @return decoded byte offset
     */
    private static int getOffset2D(byte[] data, int index) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 | data[index + 2] & 0xFF;
    }

    /**
     * Reads a 24-bit offset from a 3D compressed payload.
     *
     * @param data compressed payload
     * @param index first byte of the stored offset
     * @return decoded byte offset
     */
    private static int getOffset(byte[] data, int index) {
        return (data[index] & 0xFF) << 16 | (data[index + 1] & 0xFF) << 8 | data[index + 2] & 0xFF;
    }

    /**
     * Reads a material from a 2D compressed layer using local layer coordinates.
     *
     * @param data compressed 2D layer payload
     * @param inChunkA first 2D axis coordinate
     * @param inChunkB second 2D axis coordinate
     * @return material at the requested layer coordinate
     */
    private static byte getMaterial2D(byte[] data, int inChunkA, int inChunkB) {
        int index = 0, sizeBits = CHUNK_SIZE_BITS;

        while (true) { // Scary but should be fine
            byte identifier = (byte) (data[index] & IDENTIFIER_MASK);

            if (identifier == HOMOGENOUS) return data[index + 1];
            if (identifier == DETAIL) return data[index + getInDetailIndex2D(inChunkA, inChunkB)];
//            if (identifier == SPLITTER)
            index += getOffset2D(data, index, inChunkA, inChunkB, --sizeBits);
        }
    }

    /**
     * Locates the smallest node that covers a 2D position at the requested target size.
     *
     * @param data compressed 2D layer payload
     * @param inChunkA first 2D axis coordinate
     * @param inChunkB second 2D axis coordinate
     * @param sizeBits log2 of the source node edge length in block coordinates
     * @param targetSizeBits minimum node size to descend to
     * @return byte offset of the selected node
     */
/**
 * Performs start index of2d.
 *
 * @param data parameter
 * @param inChunkA parameter
 * @param inChunkB parameter
 * @param sizeBits parameter
 * @param targetSizeBits parameter
 * @return result
 */
    private static int startIndexOf2D(byte[] data, int inChunkA, int inChunkB, int sizeBits, int targetSizeBits) {
        int index = 0;
        while (true) { // Scary but should be fine
            byte identifier = (byte) (data[index] & IDENTIFIER_MASK);
            if (sizeBits <= targetSizeBits || identifier == HOMOGENOUS || identifier == DETAIL) return index;
//            if (identifier == SPLITTER)
            index += getOffset2D(data, index, inChunkA, inChunkB, --sizeBits);
        }
    }

    /**
     * Builds a contiguous bit mask for a run of set bits starting at {@code offset}.
     *
     * @param length number of bits to include
     * @param offset destination bit offset in the containing long
     * @return bit mask with {@code length} consecutive bits set
     */
    private static long getMask(int length, int offset) {
        return length == CHUNK_SIZE ? -1L : (1L << length) - 1 << offset;
    }

    /**
     * Sets a single bit in a bitmap indexed by local block coordinates.
     *
     * @param bitMap destination bitmap
     * @param bitIndex linear bit index in local block coordinates
     */
    private static void setBit(long[] bitMap, int bitIndex) {
        bitMap[bitIndex >> 6] |= 1L << bitIndex;
    }


    public static final int[] Z_ORDER_3D_TABLE_X = Utils.zOrderCurveLookupTable(MAX_STRUCTURE_SIZE, 3, 2);
    public static final int[] Z_ORDER_3D_TABLE_Y = Utils.zOrderCurveLookupTable(MAX_STRUCTURE_SIZE, 3, 1);
    public static final int[] T_ORDER_3D_TABLE_Z = Utils.zOrderCurveLookupTable(MAX_STRUCTURE_SIZE, 3, 0);

    static final byte HOMOGENOUS = 0;
    static final byte DETAIL = 1;
    static final byte SPLITTER = 2;
    static final byte IDENTIFIER_MASK = 0xF;
    static final int FULLY_HOMOGENOUS = 256;

    static final byte HOMOGENOUS_BYTE_SIZE = 2;
    static final byte DETAIL_BYTE_SIZE = 9;
    static final byte SPLITTER_BYTE_SIZE = 22;

    static final byte HOMOGENOUS_BYTE_SIZE_2D = 2;
    static final byte DETAIL_BYTE_SIZE_2D = 5;
    static final byte SPLITTER_BYTE_SIZE_2D = 10;

    static final byte CONTAINS_OPAQUE = -128;
    static final byte CONTAINS_TRANSPARENT = 64;
    static final byte CONTAINS_SELF_OCCLUDING = 32;
    static final byte TYPE_MASK = (byte) 0xF0;

    private byte[] data;
    private final int totalSizeBits;
}
