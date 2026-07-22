package game.server.generation;

import core.utils.MathUtils;
import core.utils.OpenSimplex2S;

import game.server.Chunk;
import game.server.materials_data.MaterialsData;
import game.server.biomes.Biome;

import org.joml.Vector3i;

import java.util.Arrays;

import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.*;

public final class GenerationData {

    public Biome biome;
    public double feature;
    public int height, specialHeight, floorMaterialDepth, floorMaterialDepthMod, undergroundRiverDepth;
    public byte steepness;
    public long totalX, totalY, totalZ;

    public long chunkX, chunkY, chunkZ;
    public final int LOD;

    /**
     * Initializes generation data for a specific chunk at a given level of detail (LOD).
     *
     * @param chunkX the x-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param chunkZ the z-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param lod the level of detail (LOD) for generation
     */
    public GenerationData(long chunkX, long chunkZ, int lod) {
        this.LOD = lod;

        chunkX &= MAX_CHUNKS_MASK >> lod;
        chunkZ &= MAX_CHUNKS_MASK >> lod;

        featureMap = featureMap(chunkX, chunkZ, lod);
        treeMap = treeMap(chunkX, chunkZ, lod);
        ChunkMapSamples samples = new ChunkMapSamples(chunkX, chunkZ, lod);

        containsUndergroundRiver = getMinRiver(samples) < UNDERGROUND_RIVER_THRESHOLD;

        resultingHeightMap = WorldGeneration.getResultingHeightMap(samples);
        biomeMap = WorldGeneration.getBiomes(resultingHeightMap, featureMap, samples);
        undergroundRiverDepthMap = containsUndergroundRiver ? WorldGeneration.getUndergroundRiverDepthMap(samples) : null;
        steepnessMap = steepnessMap(resultingHeightMap, lod);
        specialHeightMap = specialHeightMap(chunkX, chunkZ, lod, biomeMap);

        containsUndergroundRiver = isUndergroundRiverDominant(undergroundRiverDepthMap, resultingHeightMap);

        maxRiverDepth = containsUndergroundRiver ? getMax(undergroundRiverDepthMap) : Integer.MIN_VALUE;
        minHeight = getMinHeight(resultingHeightMap);
        maxHeight = getMax(resultingHeightMap);
        maxSpecialHeight = Math.max(maxHeight, getMaxSpecialHeight(resultingHeightMap, specialHeightMap));
    }

    /**
     * Sets the current chunk for which data is being generated.
     *
     * @param chunk the chunk being processed
     */
    public void setChunk(Chunk chunk) {
        chunkX = chunk.X;
        chunkY = chunk.Y;
        chunkZ = chunk.Z;

        Arrays.fill(cachedMaterials, AIR);
    }

    /**
     * Sets the current horizontal position within the chunk and updates relevant generation fields.
     *
     * @param inChunkX the x-coordinate in In-Chunk Block Coordinates [0, 63]
     * @param inChunkZ the z-coordinate in In-Chunk Block Coordinates [0, 63]
     */
    public void set(int inChunkX, int inChunkZ) {
        int index = inChunkX << CHUNK_SIZE_BITS | inChunkZ;
        int mapIndex = getMapIndex(inChunkX, inChunkZ);

        totalX = (chunkX << CHUNK_SIZE_BITS | inChunkX) << LOD;
        totalZ = (chunkZ << CHUNK_SIZE_BITS | inChunkZ) << LOD;

        undergroundRiverDepth = containsUndergroundRiver ? undergroundRiverDepthMap[mapIndex] : 0;
        feature = featureMap[index];
        steepness = steepnessMap[index];
        biome = biomeMap[index];
        specialHeight = specialHeightMap[index];
        height = resultingHeightMap[mapIndex];
        floorMaterialDepthMod = (int) (feature * 4.0F) - (steepness << 2);
        floorMaterialDepth = biome.getFloorMaterialDepth(this);
    }

    /**
     * Computes the absolute world y-coordinate for a given in-chunk y-coordinate.
     *
     * @param inChunkY the y-coordinate in In-Chunk Block Coordinates [0, 63]
     */
    public void computeTotalY(int inChunkY) {
        totalY = (chunkY << CHUNK_SIZE_BITS | inChunkY) << LOD;
    }

    /**
     * Checks if the given absolute world y-coordinate is below the floor material level.
     *
     * @param totalY the absolute world y-coordinate (LOD 0)
     * @param floorMaterialDepth the depth of the floor material in blocks (LOD 0)
     * @return true if the position is below the floor material level
     */
    public boolean isBelowFloorMaterialLevel(long totalY, int floorMaterialDepth) {
        return totalY >> LOD < height - floorMaterialDepth >> LOD;
    }

    /**
     * Checks if the given absolute world y-coordinate is within the surface material level.
     *
     * @param totalY the absolute world y-coordinate (LOD 0)
     * @param surfaceMaterialDepth the depth of the surface material in blocks (LOD 0)
     * @return true if the position is within the surface material level
     */
    public boolean isInsideSurfaceMaterialLevel(long totalY, int surfaceMaterialDepth) {
        return totalY >> LOD >= height - surfaceMaterialDepth >> LOD;
    }

    /**
     * Checks if the given absolute world y-coordinate is above the surface height.
     *
     * @param totalY the absolute world y-coordinate (LOD 0)
     * @return true if the position is above the surface
     */
    public boolean isAboveSurface(long totalY) {
        return totalY >> LOD > height >> LOD;
    }

    public boolean hasTrees() {
        return treeMap != null;
    }

    /**
     * Maps in-chunk coordinates to an index for padded maps (like heightMap).
     *
     * @param mapX the x-coordinate (usually in-chunk x + 1 for padding)
     * @param mapZ the z-coordinate (usually in-chunk z + 1 for padding)
     * @return the index in the padded map array
     */
    public static int getMapIndex(int mapX, int mapZ) {
        return mapX * CHUNK_SIZE_PADDED + mapZ;
    }

    /**
     * Stores a material in the uncompressed buffer at the specified in-chunk position.
     *
     * @param inChunkX the x-coordinate in In-Chunk Block Coordinates [0, 63]
     * @param inChunkY the y-coordinate in In-Chunk Block Coordinates [0, 63]
     * @param inChunkZ the z-coordinate in In-Chunk Block Coordinates [0, 63]
     * @param material the material byte to store
     */
    public void store(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        uncompressedMaterials[MaterialsData.getUncompressedIndex(inChunkX, inChunkY, inChunkZ)] = material;
    }

    /**
     * Fills a range of the uncompressed materials buffer with a specific material.
     *
     * @param startIndex the starting index in the buffer
     * @param count the number of elements to fill
     * @param material the material byte to store
     */
    public void storeConsecutive(int startIndex, int count, byte material) {
        Arrays.fill(uncompressedMaterials, startIndex, startIndex + count, material);
    }

    /**
     * Fills the remainder of the chunk column above the specified position with air.
     *
     * @param inChunkX the x-coordinate in In-Chunk Block Coordinates [0, 63]
     * @param inChunkY the starting y-coordinate in In-Chunk Block Coordinates [0, 63]
     * @param inChunkZ the z-coordinate in In-Chunk Block Coordinates [0, 63]
     */
    public void fillAboveWithAir(int inChunkX, int inChunkY, int inChunkZ) {
        int xzIndex = MaterialsData.Z_ORDER_3D_TABLE_X[inChunkX] | MaterialsData.T_ORDER_3D_TABLE_Z[inChunkZ];
        for (; inChunkY < CHUNK_SIZE; inChunkY++) uncompressedMaterials[xzIndex | MaterialsData.Z_ORDER_3D_TABLE_Y[inChunkY]] = AIR;
    }

    /**
     * Places a tree into the uncompressed materials buffer if it intersects with the current chunk.
     *
     * @param tree the tree structure to place
     * @param clearBeforeGenerating whether to clear the chunk with air before placement
     * @return true if the tree was successfully placed (intersects with the chunk)
     */
    public boolean storeTree(Tree tree, boolean clearBeforeGenerating) {
        long chunkStartX = chunkX << CHUNK_SIZE_BITS + LOD;
        long chunkStartY = chunkY << CHUNK_SIZE_BITS + LOD;
        long chunkStartZ = chunkZ << CHUNK_SIZE_BITS + LOD;

        long chunkMaxY = chunkY + 1 << CHUNK_SIZE_BITS + LOD;
        if (chunkStartY > tree.getMaxY() || chunkMaxY < tree.getMinY()) return false;

        int inChunkX = (int) Math.max(chunkStartX, tree.getMinX()) >> LOD & CHUNK_SIZE_MASK;
        int inChunkY = (int) Math.max(chunkStartY, tree.getMinY()) >> LOD & CHUNK_SIZE_MASK;
        int inChunkZ = (int) Math.max(chunkStartZ, tree.getMinZ()) >> LOD & CHUNK_SIZE_MASK;

        int startX = (int) (chunkStartX + (inChunkX << LOD) - tree.getMinX());
        int startY = (int) (chunkStartY + (inChunkY << LOD) - tree.getMinY());
        int startZ = (int) (chunkStartZ + (inChunkZ << LOD) - tree.getMinZ());

        int lengthX = MathUtils.min(tree.sizeX() - startX, CHUNK_SIZE - inChunkX << LOD, tree.sizeX());
        int lengthY = MathUtils.min(tree.sizeY() - startY, CHUNK_SIZE - inChunkY << LOD, tree.sizeY());
        int lengthZ = MathUtils.min(tree.sizeZ() - startZ, CHUNK_SIZE - inChunkZ << LOD, tree.sizeZ());
        if (lengthX <= 0 || lengthY <= 0 || lengthZ <= 0) return false;

        Vector3i targetStart = new Vector3i(inChunkX, inChunkY, inChunkZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        if (clearBeforeGenerating) fillUncompressedMaterialsWithAir();
        MaterialsData.fillStructureMaterialsInto(uncompressedMaterials, tree.structure(), tree.transform(), LOD, targetStart, sourceStart, size);
        return true;
    }

    public void fillUncompressedMaterialsWithAir() {
        Arrays.fill(uncompressedMaterials, AIR);
    }

    public MaterialsData getCompressedMaterials() {
        return MaterialsData.getCompressedMaterials(CHUNK_SIZE_BITS, uncompressedMaterials);
    }

    public Tree treeMapValue(int index) {
        return treeMap[index];
    }

    public boolean chunkContainsGround() {
        long chunkStartY = chunkY << CHUNK_SIZE_BITS + LOD;
        return chunkStartY < maxHeight;
    }

    public boolean chunkContainsBiome() {
        long chunkStartY = chunkY << CHUNK_SIZE_BITS + LOD;
        long chunkEndY = chunkY + 1 << CHUNK_SIZE_BITS + LOD;
        return chunkStartY < maxSpecialHeight && chunkEndY > minHeight - WorldGeneration.MAX_SURFACE_MATERIALS_DEPTH;
    }

    public boolean containsUndergroundRiver() {
        long chunkStartY = chunkY << CHUNK_SIZE_BITS + LOD;
        long chunkEndY = chunkY + 1 << CHUNK_SIZE_BITS + LOD;
        return containsUndergroundRiver && chunkStartY < maxRiverDepth && chunkEndY > -maxRiverDepth;
    }


    /**
     * Determines the stone type at a given world position using noise.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the material byte for the stone type
     */
    public static byte getGeneratingStoneType(long x, long y, long z) {
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x1FCA4F81678D9EFEL, x * STONE_TYPE_FREQUENCY, y * STONE_TYPE_FREQUENCY, z * STONE_TYPE_FREQUENCY);
        if (Math.abs(noise) < ANDESITE_THRESHOLD) return ANDESITE;
        else if (noise > SLATE_THRESHOLD) return SLATE;
        else if (noise < BLACKSTONE_THRESHOLD) return BLACKSTONE;
        else return STONE;
    }

    /**
     * Determines the ocean floor material at a given world position using noise.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the material byte for the ocean floor
     */
    public byte getOceanFloorMaterial(long x, long y, long z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x30CD70827706B4C0L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        noise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF09AE67E544680FDL, x * MUD_TYPE_FREQUENCY * 10, y * MUD_TYPE_FREQUENCY * 10, z * MUD_TYPE_FREQUENCY * 10) * 0.1;
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < SAND_THRESHOLD) material = SAND;
        else material = MUD;

        cachedMaterials[index] = material;
        return material;
    }

    /**
     * Determines the warm ocean floor material at a given world position using noise.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the material byte for the warm ocean floor
     */
    public byte getWarmOceanFloorMaterial(long x, long y, long z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEB26D0A3459AAA03L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        noise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x795680A262E2D7BBL, x * MUD_TYPE_FREQUENCY * 10, y * MUD_TYPE_FREQUENCY * 10, z * MUD_TYPE_FREQUENCY * 10) * 0.1;
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < MUD_THRESHOLD) material = MUD;
        else material = SAND;

        cachedMaterials[index] = material;
        return material;
    }

    /**
     * Determines the cold ocean floor material at a given world position using noise.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the material byte for the cold ocean floor
     */
    public byte getColdOceanFloorMaterial(long x, long y, long z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7A182AB93793E000L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        noise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xDC676EC767E50725L, x * MUD_TYPE_FREQUENCY * 10, y * MUD_TYPE_FREQUENCY * 10, z * MUD_TYPE_FREQUENCY * 10) * 0.1;
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < MUD_THRESHOLD) material = MUD;
        else material = GRAVEL;

        cachedMaterials[index] = material;
        return material;
    }

    /**
     * Determines the dirt type at a given world position using noise.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the material byte for the dirt type
     */
    public byte getGeneratingDirtType(long x, long y, long z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF88966EA665D953EL, x * DIRT_TYPE_FREQUENCY, y * DIRT_TYPE_FREQUENCY, z * DIRT_TYPE_FREQUENCY);
        noise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x16A476D46322A4F5L, x * DIRT_TYPE_FREQUENCY * 15, y * DIRT_TYPE_FREQUENCY * 15, z * DIRT_TYPE_FREQUENCY * 15) * 0.1;
        if (Math.abs(noise) < COURSE_DIRT_THRESHOLD) material = COURSE_DIRT;
        else material = DIRT;

        cachedMaterials[index] = material;
        return material;
    }

    /**
     * Determines the ice type at a given world position using noise.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the material byte for the ice type
     */
    public byte getGeneratingIceType(long x, long y, long z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xD6744EFC8D01AEFCL, x * ICE_TYPE_FREQUENCY, y * ICE_TYPE_FREQUENCY, z * ICE_TYPE_FREQUENCY);
        noise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xB4A5FBFC95B28C81L, x * ICE_TYPE_FREQUENCY * 20, y * ICE_TYPE_FREQUENCY * 20, z * ICE_TYPE_FREQUENCY * 20) * 0.05;
        if (noise > HEAVY_ICE_THRESHOLD) material = HEAVY_ICE;
        else material = ICE;

        cachedMaterials[index] = material;
        return material;
    }

    /**
     * Determines the grass type at a given world position using noise.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the material byte for the grass type
     */
    public byte getGeneratingGrassType(long x, long y, long z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEFB13EFD3B5AC7A7L, x * GRASS_TYPE_FREQUENCY, y * GRASS_TYPE_FREQUENCY, z * GRASS_TYPE_FREQUENCY);
        noise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x72FFEA6B7F992167L, x * GRASS_TYPE_FREQUENCY * 2, y * GRASS_TYPE_FREQUENCY * 2, z * GRASS_TYPE_FREQUENCY * 2) * 0.05;
        noise += feature * 0.4 - 0.2;
        if (Math.abs(noise) < MOSS_THRESHOLD) material = MOSS;
        else material = GRASS;

        cachedMaterials[index] = material;
        return material;
    }


    /**
     * Generates a feature noise map for the current chunk.
     *
     * @param chunkX the x-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param chunkZ the z-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param lod the level of detail (LOD)
     * @return an array of noise values for each column in the chunk
     */
    private static double[] featureMap(long chunkX, long chunkZ, int lod) {
        double[] featureMap = new double[CHUNK_SIZE * CHUNK_SIZE];
        double inverseMaxValue = 1.0 / Integer.MAX_VALUE;

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                long totalX = (chunkX << CHUNK_SIZE_BITS | mapX) << lod;
                long totalZ = (chunkZ << CHUNK_SIZE_BITS | mapZ) << lod;
                featureMap[mapX << CHUNK_SIZE_BITS | mapZ] = MathUtils.hash((int) totalX, (int) totalZ, (int) SEED ^ 0x5C34A7B3) * inverseMaxValue;
            }

        return featureMap;
    }

    /**
     * Generates a steepness map based on the height differences between adjacent blocks.
     *
     * @param heightMapPadded a padded height map (LOD 0)
     * @param lod the level of detail (LOD)
     * @return an array of steepness values for each column in the chunk
     */
    private static byte[] steepnessMap(int[] heightMapPadded, int lod) {
        byte[] steepnessMap = new byte[CHUNK_SIZE * CHUNK_SIZE];

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int height = heightMapPadded[getMapIndex(mapX + 1, mapZ + 1)];
                int steepnessX = Math.abs(height - heightMapPadded[getMapIndex(mapX, mapZ + 1)]);
                int steepnessZ = Math.abs(height - heightMapPadded[getMapIndex(mapX + 1, mapZ)]);
                steepnessMap[mapX << CHUNK_SIZE_BITS | mapZ] = (byte) Math.max(steepnessX >> lod, steepnessZ >> lod);
            }

        return steepnessMap;
    }

    /**
     * Generates a special height map (e.g., for biome-specific features like trees or structures).
     *
     * @param chunkX the x-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param chunkZ the z-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param lod the level of detail (LOD)
     * @param biomeMap the array of biomes for each column in the chunk
     * @return an array of special height values for each column in the chunk
     */
    private static int[] specialHeightMap(long chunkX, long chunkZ, int lod, Biome[] biomeMap) {
        int[] specialHeightMap = new int[CHUNK_SIZE * CHUNK_SIZE];
        long chunkStartX = chunkX << CHUNK_SIZE_BITS + lod;
        long chunkStartZ = chunkZ << CHUNK_SIZE_BITS + lod;

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int index = mapX << CHUNK_SIZE_BITS | mapZ;
                int height = biomeMap[index].getSpecialHeight(chunkStartX + ((long) mapX << lod), chunkStartZ + ((long) mapZ << lod));
                specialHeightMap[index] = height;
            }
        return specialHeightMap;
    }

    /**
     * Generates a tree map for the current chunk and its immediate surroundings.
     *
     * @param chunkX the x-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param chunkZ the z-coordinate of the chunk in Chunk Coordinates at the specified LOD
     * @param lod the level of detail (LOD)
     * @return an array of trees for the chunk area
     */
    private static Tree[] treeMap(long chunkX, long chunkZ, int lod) {
        if (lod > MAX_TREE_LOD) return null;

        int sideLength = (1 << lod) + 2;
        Tree[] treeMap = new Tree[sideLength * sideLength];

        long treeStartX = (chunkX << CHUNK_SIZE_BITS + lod) - CHUNK_SIZE / 2;
        long treeStartZ = (chunkZ << CHUNK_SIZE_BITS + lod) - CHUNK_SIZE / 2;

        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++) {
                long totalX = treeStartX + ((long) x << CHUNK_SIZE_BITS);
                long totalZ = treeStartZ + ((long) z << CHUNK_SIZE_BITS);

                treeMap[x * sideLength + z] = treeMapValue(totalX, totalZ);
            }
        return treeMap;
    }

    /**
     * Determines if a tree should be placed at the specified world position.
     *
     * @param totalX the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param totalZ the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the tree to be placed, or null if no tree
     */
    private static Tree treeMapValue(long totalX, long totalZ) {
        MapSample sample = new MapSample(totalX, totalZ, true, true);

        int resultingHeight = WorldGeneration.getResultingHeight(sample);
        int heightPlusX = WorldGeneration.getResultingHeight(totalX + 1, totalZ);
        int heightPlusZ = WorldGeneration.getResultingHeight(totalX, totalZ + 1);
        int steepness = Math.max(Math.abs(resultingHeight - heightPlusX), Math.abs(resultingHeight - heightPlusZ));
        int riverDepth = WorldGeneration.getRiverDepth(sample.river());
        if (steepness != 0 || riverDepth >= resultingHeight - 16) return null;

        Biome biome = WorldGeneration.getBiome(sample, resultingHeight, 0);

        if ((MathUtils.hash((int) totalX, (int) totalZ, (int) (SEED ^ 0x264F6E393FE89AAFL)) & biome.getRequiredTreeZeroBits()) != 0) return null;
        return biome.getGeneratingTree(totalX, resultingHeight, totalZ);
    }

    private static int getMinHeight(int[] resultingHeightMap) {
        int min = Integer.MAX_VALUE;
        for (int height : resultingHeightMap) min = Math.min(min, height);
        return min;
    }

    private static int getMax(int[] values) {
        int max = Integer.MIN_VALUE;
        for (int value : values) max = Math.max(max, value);
        return max;
    }

    private static float getMinRiver(ChunkMapSamples samples) {
        float min = Float.POSITIVE_INFINITY;
        for (float riverValue : samples.riverMap()) min = Math.min(min, riverValue);
        return min;
    }

    private static int getMaxSpecialHeight(int[] resultingHeightMap, int[] specialHeightMap) {
        int max = Integer.MIN_VALUE;
        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int height = Math.max(resultingHeightMap[getMapIndex(mapX, mapZ)], WATER_LEVEL);
                max = Math.max(max, height + specialHeightMap[mapX << CHUNK_SIZE_BITS | mapZ]);
            }
        return max;
    }

    private static boolean isUndergroundRiverDominant(int[] riverDepthMap, int[] resultingHeightMap) {
        if (riverDepthMap == null) return false;
        for (int index = 0; index < riverDepthMap.length; index++)
            if (-riverDepthMap[index] < resultingHeightMap[index]) return true;
        return false;
    }


    /**
     * Calculates the compressed index for cached materials based on world coordinates.
     *
     * @param x the x-coordinate in Absolute World Coordinates (LOD 0)
     * @param y the y-coordinate in Absolute World Coordinates (LOD 0)
     * @param z the z-coordinate in Absolute World Coordinates (LOD 0)
     * @return the index in the cachedMaterials array
     */
    private int getCompressedIndex(long x, long y, long z) {
        // >> 2 for compression and performance improvement
        int compressedX = (int) (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (int) (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (int) (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        return compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
    }

    private final int minHeight, maxHeight, maxSpecialHeight, maxRiverDepth;
    private boolean containsUndergroundRiver;
    private final Tree[] treeMap;
    private final double[] featureMap;
    private final Biome[] biomeMap;

    private final int[] undergroundRiverDepthMap;
    private final int[] resultingHeightMap;
    private final int[] specialHeightMap;
    private final byte[] steepnessMap;
    private final byte[] cachedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE >> 6];

    private final byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];


    private static final double STONE_TYPE_FREQUENCY = 1 / 800.0;
    private static final double ANDESITE_THRESHOLD = 0.1;
    private static final double SLATE_THRESHOLD = 0.6;
    private static final double BLACKSTONE_THRESHOLD = -0.6;

    private static final double MUD_TYPE_FREQUENCY = 1 / 400.0;
    private static final double GRAVEL_THRESHOLD = 0.1;
    private static final double CLAY_THRESHOLD = 0.5;
    private static final double SAND_THRESHOLD = -0.5;
    private static final double MUD_THRESHOLD = -0.5;

    private static final double DIRT_TYPE_FREQUENCY = 1 / 320.0;
    private static final double COURSE_DIRT_THRESHOLD = 0.15;

    private static final double GRASS_TYPE_FREQUENCY = 1 / 640.0;
    private static final double MOSS_THRESHOLD = 0.3;

    private static final double ICE_TYPE_FREQUENCY = 1 / 200.0;
    private static final double HEAVY_ICE_THRESHOLD = 0.6;
}
