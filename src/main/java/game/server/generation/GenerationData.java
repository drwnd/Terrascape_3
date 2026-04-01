package game.server.generation;

import core.utils.MathUtils;
import core.utils.OpenSimplex2S;

import game.server.Chunk;
import game.server.MaterialsData;
import game.server.biomes.Biome;

import org.joml.Vector3i;

import java.util.Arrays;

import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.*;

public final class GenerationData {

    public Biome biome;
    public double feature;
    public int height, specialHeight, floorMaterialDepth, floorMaterialDepthMod;
    public byte steepness;
    public long totalX, totalY, totalZ;

    public long chunkX, chunkY, chunkZ;
    public final int LOD;

    public GenerationData(long chunkX, long chunkZ, int lod) {
        this.LOD = lod;

        chunkX &= MAX_CHUNKS_MASK >> lod;
        chunkZ &= MAX_CHUNKS_MASK >> lod;

        featureMap = featureMap(chunkX, chunkZ, lod);
        treeMap = treeMap(chunkX, chunkZ, lod);
        ChunkMapSamples samples = new ChunkMapSamples(chunkX, chunkZ, lod);

        resultingHeightMap = WorldGeneration.getResultingHeightMap(samples);
        steepnessMap = steepnessMap(resultingHeightMap, lod);
        biomeMap = WorldGeneration.getBiomes(resultingHeightMap, featureMap, samples);
        specialHeightMap = specialHeightMap(chunkX, chunkZ, lod, biomeMap);

        minHeight = getMinHeight(resultingHeightMap);
        maxHeight = getMaxHeight(resultingHeightMap);
        maxSpecialHeight = Math.max(maxHeight, getMaxSpecialHeight(resultingHeightMap, specialHeightMap));
    }

    public void setChunk(Chunk chunk) {
        chunkX = chunk.X;
        chunkY = chunk.Y;
        chunkZ = chunk.Z;

        Arrays.fill(uncompressedMaterials, AIR);
        Arrays.fill(cachedMaterials, AIR);
    }

    public void set(int inChunkX, int inChunkZ) {
        int index = inChunkX << CHUNK_SIZE_BITS | inChunkZ;
        int mapIndex = getMapIndex(inChunkX, inChunkZ);

        totalX = (chunkX << CHUNK_SIZE_BITS | inChunkX) << LOD;
        totalZ = (chunkZ << CHUNK_SIZE_BITS | inChunkZ) << LOD;

        feature = featureMap[index];
        steepness = steepnessMap[index];
        biome = biomeMap[index];
        specialHeight = specialHeightMap[index];
        height = resultingHeightMap[mapIndex];
        floorMaterialDepthMod = (int) (feature * 4.0F) - (steepness << 2);
        floorMaterialDepth = biome.getFloorMaterialDepth(this);
    }

    public void computeTotalY(int inChunkY) {
        totalY = (chunkY << CHUNK_SIZE_BITS | inChunkY) << LOD;
    }

    public boolean isBelowFloorMaterialLevel(long totalY, int floorMaterialDepth) {
        return totalY >> LOD < height - floorMaterialDepth >> LOD;
    }

    public boolean isInsideSurfaceMaterialLevel(long totalY, int surfaceMaterialDepth) {
        return totalY >> LOD >= height - surfaceMaterialDepth >> LOD;
    }

    public boolean isAboveSurface(long totalY) {
        return totalY >> LOD > height >> LOD;
    }

    public boolean hasTrees() {
        return treeMap != null;
    }

    public static int getMapIndex(int mapX, int mapZ) {
        return mapX * CHUNK_SIZE_PADDED + mapZ;
    }

    public void store(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        uncompressedMaterials[MaterialsData.getUncompressedIndex(inChunkX, inChunkY, inChunkZ)] = material;
    }

    public void storeConsecutive(int startIndex, int count, byte material) {
        for (int index = startIndex; index < startIndex + count; index++) uncompressedMaterials[index] = material;
    }

    public void fillAboveWithAir(int inChunkX, int inChunkY, int inChunkZ) {
        int xzIndex = MaterialsData.Z_ORDER_3D_TABLE_X[inChunkX] | MaterialsData.T_ORDER_3D_TABLE_Z[inChunkZ];
        for (; inChunkY < CHUNK_SIZE; inChunkY++) uncompressedMaterials[xzIndex | MaterialsData.Z_ORDER_3D_TABLE_Y[inChunkY]] = AIR;
    }

    public boolean storeTree(Tree tree) {
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

        MaterialsData.fillStructureMaterialsInto(uncompressedMaterials, tree.structure(), tree.transform(), LOD, targetStart, sourceStart, size);
        return true;
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


    public static byte getGeneratingStoneType(long x, long y, long z) {
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x1FCA4F81678D9EFEL, x * STONE_TYPE_FREQUENCY, y * STONE_TYPE_FREQUENCY, z * STONE_TYPE_FREQUENCY);
        noise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCDF7BB51DC497C88L, x * STONE_TYPE_FREQUENCY * 20, y * STONE_TYPE_FREQUENCY * 20, z * STONE_TYPE_FREQUENCY * 20) * 0.05;
        if (Math.abs(noise) < ANDESITE_THRESHOLD) return ANDESITE;
        else if (noise > SLATE_THRESHOLD) return SLATE;
        else if (noise < BLACKSTONE_THRESHOLD) return BLACKSTONE;
        else return STONE;
    }

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

    private static Tree treeMapValue(long totalX, long totalZ) {
        MapSample sample = new MapSample(totalX, totalZ, MapSample.GENERATE_BIOME_MAPS | MapSample.GENERATE_HEIGHT_MAPS);

        int resultingHeight = WorldGeneration.getResultingHeight(sample.height(), sample.erosion(), sample.continental(), sample.river(), sample.ridge());
        int heightPlusX = WorldGeneration.getResultingHeight(totalX + 1, totalZ);
        int heightPlusZ = WorldGeneration.getResultingHeight(totalX, totalZ + 1);
        int steepness = Math.max(Math.abs(resultingHeight - heightPlusX), Math.abs(resultingHeight - heightPlusZ));
        if (steepness != 0) return null;

        Biome biome = WorldGeneration.getBiome(sample.temperature(), sample.humidity(), 96, resultingHeight, sample.erosion(), sample.continental(), 0);

        if ((MathUtils.hash((int) totalX, (int) totalZ, (int) (SEED ^ 0x264F6E393FE89AAFL)) & biome.getRequiredTreeZeroBits()) != 0) return null;
        return biome.getGeneratingTree(totalX, resultingHeight, totalZ);
    }

    private static int getMinHeight(int[] resultingHeightMap) {
        int min = Integer.MAX_VALUE;
        for (int height : resultingHeightMap) min = Math.min(min, height);
        return min;
    }

    private static int getMaxHeight(int[] resultingHeightMap) {
        int max = Integer.MIN_VALUE;
        for (int height : resultingHeightMap) max = Math.max(max, height);
        return max;
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


    private int getCompressedIndex(long x, long y, long z) {
        // >> 2 for compression and performance improvement
        int compressedX = (int) (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (int) (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (int) (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        return compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
    }

    private final int minHeight, maxHeight, maxSpecialHeight;
    private final Tree[] treeMap;
    private final double[] featureMap;
    private final Biome[] biomeMap;

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
