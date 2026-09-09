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
    public int height, specialHeight, biomeDepth, biomeDepthMod, undergroundRiverDepth;
    public float steepness;
    public long totalX, totalZ, chunkY;

    public final long chunkX, chunkZ;
    public final int LOD;

    public GenerationData(long chunkX, long chunkZ, int lod) {
        this.LOD = lod;

        chunkX &= MAX_CHUNKS_MASK >> lod;
        chunkZ &= MAX_CHUNKS_MASK >> lod;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;

        featureMap = featureMap(chunkX, chunkZ, lod);
        ChunkMapSamples samples = new ChunkMapSamples(chunkX, chunkZ, lod);

        containsUndergroundRiver = getMinRiver(samples) < UNDERGROUND_RIVER_THRESHOLD;

        resultingHeightMap = WorldGeneration.getResultingHeightMap(samples, steepnessMap, lod);
        biomeMap = WorldGeneration.getBiomes(resultingHeightMap, featureMap, samples);
        undergroundRiverDepthMap = containsUndergroundRiver ? WorldGeneration.getUndergroundRiverDepthMap(samples) : null;
        specialHeightMap = specialHeightMap(chunkX, chunkZ, lod, biomeMap);

        worldGenStructureMap = structureMap(chunkX, chunkZ, lod);
        structureFeatureMap = structureFeatureMap();

        containsUndergroundRiver = isUndergroundRiverDominant(undergroundRiverDepthMap, resultingHeightMap);

        maxRiverDepth = containsUndergroundRiver ? getMax(undergroundRiverDepthMap) : Integer.MIN_VALUE;
        minHeight = getMinHeight(resultingHeightMap);
        maxHeight = getMax(resultingHeightMap);
        maxSpecialHeight = Math.max(maxHeight, getMaxSpecialHeight(resultingHeightMap, specialHeightMap));
    }

    public void setChunk(Chunk chunk) {
        chunkY = chunk.Y;

        Arrays.fill(cachedMaterials, AIR);
    }

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
        biomeDepthMod = (int) (-Math.max(0, steepness - 2 + feature * 2) * 26);
        biomeDepth = biome.getBiomeDepth(this);
    }

    public long computeTotalY(int inChunkY) {
        return (chunkY << CHUNK_SIZE_BITS | inChunkY) << LOD;
    }

    public boolean isInsideSurfaceMaterialLevel(long totalY, int surfaceMaterialDepth) {
        return totalY >> LOD >= height - surfaceMaterialDepth - biomeDepthMod >> LOD;
    }

    public boolean hasStructures() {
        return worldGenStructureMap != null;
    }

    public boolean hasStructureFeatures() {
        return structureFeatureMap != null;
    }

    public static int getMapIndex(int mapX, int mapZ) {
        return mapX * CHUNK_SIZE_PADDED + mapZ;
    }

    public int clampStartHeightToInChunkY(int height) {
        return Math.clamp(height - (chunkY << CHUNK_SIZE_BITS + LOD) >> LOD, 0, CHUNK_SIZE);
    }

    public int clampEndHeightToInChunkY(int height) {
        return Math.clamp((height - (chunkY << CHUNK_SIZE_BITS + LOD) >> LOD) + 1, 0, CHUNK_SIZE);
    }

    public void store(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        uncompressedMaterials[MaterialsData.getUncompressedIndex(inChunkX, inChunkY, inChunkZ)] = material;
    }

    public void storeColumn(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, byte material) {
        int xzIndex = MaterialsData.Z_ORDER_3D_TABLE_X[inChunkX] | MaterialsData.Z_ORDER_3D_TABLE_Z[inChunkZ];
        for (int inChunkY = inChunkStartY; inChunkY < inChunkEndY; inChunkY++)
            uncompressedMaterials[xzIndex | MaterialsData.Z_ORDER_3D_TABLE_Y[inChunkY]] = material;
    }

    public void storeColumn(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, MaterialFunction materialFunction) {
        int xzIndex = MaterialsData.Z_ORDER_3D_TABLE_X[inChunkX] | MaterialsData.Z_ORDER_3D_TABLE_Z[inChunkZ];
        for (int inChunkY = inChunkStartY; inChunkY < inChunkEndY; inChunkY++) {
            byte material = materialFunction.getGeneratingMaterial(this, totalX, computeTotalY(inChunkY), totalZ);
            uncompressedMaterials[xzIndex | MaterialsData.Z_ORDER_3D_TABLE_Y[inChunkY]] = material;
        }
    }

    public void storeConsecutive(int startIndex, int count, byte material) {
        Arrays.fill(uncompressedMaterials, startIndex, startIndex + count, material);
    }

    public void fillAboveWith(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        int xzIndex = MaterialsData.Z_ORDER_3D_TABLE_X[inChunkX] | MaterialsData.Z_ORDER_3D_TABLE_Z[inChunkZ];
        for (; inChunkY < CHUNK_SIZE; inChunkY++) uncompressedMaterials[xzIndex | MaterialsData.Z_ORDER_3D_TABLE_Y[inChunkY]] = material;
    }

    public boolean storeStructure(WorldGenStructure worldGenStructure, boolean clearBeforeGenerating) {
        long chunkStartX = chunkX << CHUNK_SIZE_BITS + LOD;
        long chunkStartY = chunkY << CHUNK_SIZE_BITS + LOD;
        long chunkStartZ = chunkZ << CHUNK_SIZE_BITS + LOD;

        long chunkMaxY = chunkY + 1 << CHUNK_SIZE_BITS + LOD;
        if (chunkStartY > worldGenStructure.getMaxY() || chunkMaxY < worldGenStructure.getMinY()) return false;

        int inChunkX = (int) Math.max(chunkStartX, worldGenStructure.getMinX()) >> LOD & CHUNK_SIZE_MASK;
        int inChunkY = (int) Math.max(chunkStartY, worldGenStructure.getMinY()) >> LOD & CHUNK_SIZE_MASK;
        int inChunkZ = (int) Math.max(chunkStartZ, worldGenStructure.getMinZ()) >> LOD & CHUNK_SIZE_MASK;

        int startX = (int) (chunkStartX + (inChunkX << LOD) - worldGenStructure.getMinX());
        int startY = (int) (chunkStartY + (inChunkY << LOD) - worldGenStructure.getMinY());
        int startZ = (int) (chunkStartZ + (inChunkZ << LOD) - worldGenStructure.getMinZ());

        int lengthX = MathUtils.min(worldGenStructure.sizeX() - startX, CHUNK_SIZE - inChunkX << LOD, worldGenStructure.sizeX());
        int lengthY = MathUtils.min(worldGenStructure.sizeY() - startY, CHUNK_SIZE - inChunkY << LOD, worldGenStructure.sizeY());
        int lengthZ = MathUtils.min(worldGenStructure.sizeZ() - startZ, CHUNK_SIZE - inChunkZ << LOD, worldGenStructure.sizeZ());
        if (lengthX <= 0 || lengthY <= 0 || lengthZ <= 0) return false;

        Vector3i targetStart = new Vector3i(inChunkX, inChunkY, inChunkZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        if (clearBeforeGenerating) fillUncompressedMaterialsWithAir();
        MaterialsData.fillStructureMaterialsInto(uncompressedMaterials, worldGenStructure.structure(), worldGenStructure.transform(), LOD, targetStart, sourceStart, size, false);
        return true;
    }

    public void fillUncompressedMaterialsWithAir() {
        Arrays.fill(uncompressedMaterials, AIR);
    }

    public MaterialsData getCompressedMaterials() {
        return MaterialsData.getCompressedMaterials(CHUNK_SIZE_BITS, uncompressedMaterials);
    }

    public WorldGenStructure structureMapValue(int index) {
        return worldGenStructureMap[index];
    }

    public WorldGenStructure structureFeatureMapValue(int index) {
        return structureFeatureMap[index];
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


    public static byte getGeneratingStoneType(long x, long y, long z) {
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x1FCA4F81678D9EFEL, x * STONE_TYPE_FREQUENCY, y * STONE_TYPE_FREQUENCY, z * STONE_TYPE_FREQUENCY);
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

    private static WorldGenStructure[] structureMap(long chunkX, long chunkZ, int lod) {
        if (lod > MAX_STRUCTURE_LOD) return null;

        int sideLength = (1 << lod) + 2;
        WorldGenStructure[] worldGenStructureMap = new WorldGenStructure[sideLength * sideLength];

        long structureStartX = (chunkX << CHUNK_SIZE_BITS + lod) - CHUNK_SIZE / 2;
        long structureStartZ = (chunkZ << CHUNK_SIZE_BITS + lod) - CHUNK_SIZE / 2;

        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++) {
                long totalX = structureStartX + ((long) x << CHUNK_SIZE_BITS);
                long totalZ = structureStartZ + ((long) z << CHUNK_SIZE_BITS);

                worldGenStructureMap[x * sideLength + z] = structureMapValue(totalX, totalZ);
            }
        return worldGenStructureMap;
    }

    private static WorldGenStructure structureMapValue(long totalX, long totalZ) {
        MapSample sample = new MapSample(totalX, totalZ, true, true);

        double resultingHeight = WorldGeneration.getResultingHeight(sample);
        double heightPlusX = WorldGeneration.getResultingHeight(totalX + 1, totalZ);
        double heightPlusZ = WorldGeneration.getResultingHeight(totalX, totalZ + 1);
        double steepness = Math.max(Math.abs(resultingHeight - heightPlusX), Math.abs(resultingHeight - heightPlusZ));
        int riverDepth = WorldGeneration.getRiverDepth(sample.river());
        if (steepness > 0.4 || riverDepth >= Math.abs(resultingHeight) - 16) return null;

        Biome biome = WorldGeneration.getBiome(sample, MathUtils.floor(resultingHeight), 0);

        if ((MathUtils.hash((int) totalX, (int) totalZ, (int) (SEED ^ 0x264F6E393FE89AAFL)) & 1023) >= biome.getStructureChancePromille()) return null;
        return biome.getStructure(totalX, MathUtils.floor(resultingHeight) - 8, totalZ);
    }

    private WorldGenStructure[] structureFeatureMap() {
        if (LOD > MAX_STRUCTURE_FEATURE_LOD) return null;

        int sideLength = (2 << LOD);
        WorldGenStructure[] structureFeatureMap = new WorldGenStructure[sideLength * sideLength];

        int inChunkDistance = CHUNK_SIZE / 2 >> LOD;
        int inChunkStart = CHUNK_SIZE / 4 >> LOD;
        if (inChunkDistance == 0) return null;

        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++) {
                int inChunkX = inChunkStart + x * inChunkDistance;
                int inChunkZ = inChunkStart + z * inChunkDistance;
                structureFeatureMap[x * sideLength + z] = structureFeatureMapValue(inChunkX, inChunkZ);
            }

        return structureFeatureMap;
    }

    private WorldGenStructure structureFeatureMapValue(int inChunkX, int inChunkZ) {
        int index = inChunkX << CHUNK_SIZE_BITS | inChunkZ;
        Biome biome = biomeMap[index];

        long totalX = chunkX << CHUNK_SIZE_BITS + LOD | ((long) inChunkX << LOD);
        long totalZ = chunkZ << CHUNK_SIZE_BITS + LOD | ((long) inChunkZ << LOD);

        int riverDepth = WorldGeneration.getRiverDepth(MapSample.riverMapValue(totalX, totalZ));
        int resultingHeight = resultingHeightMap[getMapIndex(inChunkX, inChunkZ)];

        if (steepnessMap[index] > 1 || riverDepth >= Math.abs(resultingHeight) - 16) return null;
        if ((MathUtils.hash((int) totalX, (int) totalZ, (int) (SEED ^ 0x264F6E393FE89AAFL)) & 1023) >= biome.getStructureFeatureChancePromille()) return null;
        return biome.getStructureFeature(totalX, resultingHeight, totalZ);
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


    private int getCompressedIndex(long x, long y, long z) {
        // >> 2 for compression and performance improvement
        int compressedX = (int) (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (int) (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (int) (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        return compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
    }

    private final int minHeight, maxHeight, maxSpecialHeight, maxRiverDepth;
    private boolean containsUndergroundRiver;
    private final WorldGenStructure[] worldGenStructureMap, structureFeatureMap;
    private final double[] featureMap;
    private final Biome[] biomeMap;

    private final int[] undergroundRiverDepthMap;
    private final int[] resultingHeightMap;
    private final int[] specialHeightMap;
    private final float[] steepnessMap = new float[CHUNK_SIZE * CHUNK_SIZE];
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

    public interface MaterialFunction {
        byte getGeneratingMaterial(GenerationData data, long x, long y, long z);
    }
}
