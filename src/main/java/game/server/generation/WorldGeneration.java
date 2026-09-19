package game.server.generation;

import core.utils.MathUtils;

import game.server.Chunk;
import game.server.materials_data.MaterialsData;
import game.server.biomes.Biome;
import game.utils.Status;
import game.utils.Utils;

import static game.utils.Constants.*;

public final class WorldGeneration {

    public static final int WATER_LEVEL = 0;
    public static final int MAX_SURFACE_MATERIALS_DEPTH = 132;

    public static final int OCEAN_FLOOR_OFFSET = -480;
    public static final int DEEP_OCEAN_FLOOR_OFFSET = -1120;
    public static final int FLATLAND_HEIGHT = 55;
    public static final int HIGHLAND_OFFSET = 500;
    public static final int RIVER_OFFSET = -200;

    public static final double MOUNTAIN_THRESHOLD = 0.3;
    public static final double OCEAN_THRESHOLD = -0.3;
    public static final double FLATLAND_THRESHOLD = 0.3;
    public static final double RIVER_THRESHOLD = 0.1;
    public static final double INNER_RIVER_THRESHOLD = 0.005;
    public static final double UNDERGROUND_RIVER_THRESHOLD = 0.012;

    public static long SEED;
    public static int BLOCK_SIZE_BITS, BLOCK_SIZE, BLOCK_SIZE_MASK;
    private static BiomeSamplers.BiomeSampler BIOME_SAMPLER;

    public static void setSettings(WorldGenerationSettings settings) {
        SEED = settings.seed();
        BLOCK_SIZE = MathUtils.nextLargestPowOf2(settings.blockSize());
        BLOCK_SIZE_BITS = Integer.numberOfTrailingZeros(BLOCK_SIZE);
        BLOCK_SIZE_MASK = -BLOCK_SIZE;
        BIOME_SAMPLER = settings.biomeSampler().biomeSampler;
    }

    public static void generate(Chunk chunk) {
        if (chunk.getGenerationStatus() != Status.NOT_STARTED) return;
        generate(chunk, new GenerationData(chunk.X, chunk.Z, chunk.LOD));
    }

    public static void generate(Chunk chunk, GenerationData data) {
        if (chunk.getGenerationStatus() != Status.NOT_STARTED) return;
        chunk.setGenerationStatus(Status.IN_PROGRESS);

        data.setChunk(chunk);
        boolean chunkContainsVoxels = false;

        boolean containsGround = data.chunkContainsGround(), containsBiome = data.chunkContainsBiome(), containsRiver = data.containsUndergroundRiver();
        if (containsGround) {
            generateStone(data);
            chunkContainsVoxels = true;
        }

        if (!containsBiome && !containsGround && containsRiver) data.fillUncompressedMaterialsWithAir();
        if (containsBiome || containsRiver) {
            for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX++)
                for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ++) {
                    data.set(inChunkX, inChunkZ);
                    if (containsBiome) generateBiome(inChunkX, inChunkZ, data);
                    if (containsRiver) generateUndergroundRiver(inChunkX, inChunkZ, data);
                }
            chunkContainsVoxels = true;
        }

        chunkContainsVoxels |= generateStructures(data, !chunkContainsVoxels);

        chunk.setMaterials(chunkContainsVoxels ? data.getCompressedMaterials() : new MaterialsData(CHUNK_SIZE_BITS, AIR));
        chunk.setGenerationStatus(Status.DONE);
    }

    public static double getResultingHeight(MapSample sample) {
        double continentalModifier = getContinentalModifier(sample);
        double erosionModifier = getErosionModifier(continentalModifier, sample);
        double riverModifier = getRiverModifier(erosionModifier, continentalModifier, sample);

        return (sample.height() + continentalModifier + erosionModifier + riverModifier) * 2 + WATER_LEVEL - 15;
    }

    public static double getResultingHeight(long totalX, long totalZ) {
        return getResultingHeight(new MapSample(totalX, totalZ, false, true));
    }

    public static int[] getResultingHeightMap(ChunkMapSamples samples, float[] steepnessMap, int lod) {
        int[] resultingHeightMap = new int[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        double[] exactHeightMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {

                int mapIndex = GenerationData.getMapIndex(mapX, mapZ);
                double resultingHeight = getResultingHeight(samples.getSample(mapIndex));
                exactHeightMap[mapIndex] = resultingHeight;
                resultingHeightMap[mapIndex] = MathUtils.floor(resultingHeight);
            }

        float lodNormalizer = 1.0F / (1 << lod);
        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                double height = exactHeightMap[GenerationData.getMapIndex(mapX, mapZ)];

                double steepnessX = Math.abs(height - exactHeightMap[GenerationData.getMapIndex(mapX + 1, mapZ)]);
                double steepnessZ = Math.abs(height - exactHeightMap[GenerationData.getMapIndex(mapX, mapZ + 1)]);
                steepnessMap[mapX << CHUNK_SIZE_BITS | mapZ] = (float) Math.max(steepnessX, steepnessZ) * lodNormalizer;
            }
        return resultingHeightMap;
    }

    public static int[] getUndergroundRiverDepthMap(ChunkMapSamples samples) {
        float[] riverMap = samples.riverMap();
        int[] riverDepthMap = new int[riverMap.length];
        for (int index = 0; index < riverDepthMap.length; index++) riverDepthMap[index] = getRiverDepth(riverMap[index]);
        return riverDepthMap;
    }

    public static int getRiverDepth(double river) {
        return (int) (Math.sqrt(Math.max(0, UNDERGROUND_RIVER_THRESHOLD - river)) * 1500);
    }

    public static Biome[] getBiomes(int[] heightMap, double[] featureMap, ChunkMapSamples samples) {
        Biome[] biomes = new Biome[CHUNK_SIZE * CHUNK_SIZE];
        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int mapIndex = GenerationData.getMapIndex(mapX, mapZ);
                int index = mapX << CHUNK_SIZE_BITS | mapZ;
                biomes[index] = getBiome(samples.getSample(mapIndex), heightMap[mapIndex], featureMap[index]);
            }
        return biomes;
    }

    public static Biome getBiome(MapSample sample, int height, double feature) {
        return BIOME_SAMPLER.getBiome(sample, height, feature);
    }


    private static void generateStone(GenerationData data) {
        long chunkStartX = data.chunkX << CHUNK_SIZE_BITS + data.LOD;
        long chunkStartY = data.chunkY << CHUNK_SIZE_BITS + data.LOD;
        long chunkStartZ = data.chunkZ << CHUNK_SIZE_BITS + data.LOD;

        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE; index += 8 * 8 * 8) {
            int inChunkX = Utils.getInChunkX(index) << data.LOD;
            int inChunkY = Utils.getInChunkY(index) << data.LOD;
            int inChunkZ = Utils.getInChunkZ(index) << data.LOD;
            byte stoneType = GenerationData.getGeneratingStoneType(chunkStartX + inChunkX, chunkStartY + inChunkY, chunkStartZ + inChunkZ);
            data.storeConsecutive(index, 8 * 8 * 8, stoneType);
        }
    }

    private static void generateBiome(int inChunkX, int inChunkZ, GenerationData data) {
        Biome biome = data.biome;
        int height = data.height;
        int start = data.clampStartHeightToInChunkY(height - data.biomeDepth);
        int end = data.clampEndHeightToInChunkY(height);

        biome.placeMaterials(inChunkX, inChunkZ, start, end, data);
        data.fillAboveWith(inChunkX, end, inChunkZ, data.chunkY << CHUNK_SIZE_BITS + data.LOD < WATER_LEVEL ? WATER : AIR);
        biome.placeSpecialFeatures(inChunkX, inChunkZ, data);
    }

    private static void generateUndergroundRiver(int inChunkX, int inChunkZ, GenerationData data) {
        long chunkStartY = data.chunkY << CHUNK_SIZE_BITS + data.LOD;
        if (data.height < WATER_LEVEL && chunkStartY >= WATER_LEVEL) return;
        int start = data.clampStartHeightToInChunkY(WATER_LEVEL - data.undergroundRiverDepth);
        int end = data.clampStartHeightToInChunkY(WATER_LEVEL + data.undergroundRiverDepth);
        data.storeColumn(inChunkX, inChunkZ, start, end, chunkStartY < WATER_LEVEL ? WATER : AIR);
    }

    private static boolean generateStructures(GenerationData data, boolean clearBeforeGenerating) {
        boolean hasGeneratedStructure = false;

        int sideLength = (1 << data.LOD) + 2;
        if (data.hasStructures())
            for (int index = 0; index < sideLength * sideLength; index++) {
                WorldGenStructure worldGenStructure = data.structureMapValue(index);
                if (worldGenStructure == null) continue;

                hasGeneratedStructure |= data.storeStructure(worldGenStructure, clearBeforeGenerating && !hasGeneratedStructure);
            }

        sideLength = 2 << data.LOD;
        if (data.hasStructureFeatures())
            for (int index = 0; index < sideLength * sideLength; index++) {
                WorldGenStructure structureFeature = data.structureFeatureMapValue(index);
                if (structureFeature == null) continue;

                hasGeneratedStructure |= data.storeStructure(structureFeature, clearBeforeGenerating && !hasGeneratedStructure);
            }

        return hasGeneratedStructure;
    }

    private static double getContinentalModifier(MapSample sample) {
        double continentalModifier = 0.0, continental = sample.continental();
        // Mountains
        if (continental > MOUNTAIN_THRESHOLD)
            continentalModifier = (continental - MOUNTAIN_THRESHOLD) * (continental - MOUNTAIN_THRESHOLD) * 20000 + sample.ridge() * (continental - MOUNTAIN_THRESHOLD) * 10000;
            // Normal ocean
        else if (continental < OCEAN_THRESHOLD && continental > OCEAN_THRESHOLD - 0.05)
            continentalModifier = MathUtils.smoothInOutQuad(-continental, -OCEAN_THRESHOLD, -OCEAN_THRESHOLD + 0.05) * OCEAN_FLOOR_OFFSET;
        else if (continental <= OCEAN_THRESHOLD - 0.05 && continental > OCEAN_THRESHOLD - 0.2)
            continentalModifier = (continental - (OCEAN_THRESHOLD - 0.05)) * 100 + OCEAN_FLOOR_OFFSET;
            // Deep Ocean
        else if (continental <= OCEAN_THRESHOLD - 0.2 && continental > OCEAN_THRESHOLD - 0.25)
            continentalModifier = MathUtils.smoothInOutQuad(-continental, -OCEAN_THRESHOLD + 0.2, -OCEAN_THRESHOLD + 0.25) * DEEP_OCEAN_FLOOR_OFFSET + OCEAN_FLOOR_OFFSET - 15;
        else if (continental <= OCEAN_THRESHOLD - 0.25)
            continentalModifier = (continental - (OCEAN_THRESHOLD - 0.25)) * 100 + OCEAN_FLOOR_OFFSET + DEEP_OCEAN_FLOOR_OFFSET - 15;
        return continentalModifier;
    }

    private static double getErosionModifier(double continentalModifier, MapSample sample) {
        double erosionModifier = 0.0, erosion = sample.erosion(), height = sample.height();
        // Elevated areas
        if (erosion < -0 && erosion > -0.5) erosionModifier = MathUtils.smoothInOutQuad(-erosion, 0, 0.5) * HIGHLAND_OFFSET;
        else if (erosion <= -0.50) erosionModifier = -(erosion + 0.50) * 20 + HIGHLAND_OFFSET;
            // Flatland
        else if (erosion > FLATLAND_THRESHOLD && erosion < FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -(continentalModifier + height * 0.85 - FLATLAND_HEIGHT) * MathUtils.smoothInOutQuad(erosion, FLATLAND_THRESHOLD, FLATLAND_THRESHOLD + 0.25);
        else if (erosion >= FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -height * 0.85 - continentalModifier + FLATLAND_HEIGHT;
        return erosionModifier;
    }

    private static double getRiverModifier(double erosionModifier, double continentalModifier, MapSample sample) {
        double height = sample.height(), river = sample.river();
        double riverThinning = getRiverThinning(sample);
        double innerThreshold = (1 - riverThinning) * INNER_RIVER_THRESHOLD;
        double outerThreshold = (1 - riverThinning) * RIVER_THRESHOLD;
        if (river > outerThreshold) return 0;

        double oceanScale = Math.clamp(-sample.continental(), -OCEAN_THRESHOLD, -OCEAN_THRESHOLD + 0.2);
        oceanScale = MathUtils.smoothInOutQuad(oceanScale, -OCEAN_THRESHOLD, -OCEAN_THRESHOLD + 0.2);

        double riverModifier = -height * 0.85 - continentalModifier - erosionModifier + RIVER_OFFSET;
        if (river < outerThreshold && river >= innerThreshold)
            riverModifier *= (1 - MathUtils.smoothInOutQuad(river, innerThreshold, outerThreshold));
        return riverModifier * (1 - riverThinning) * (1 - oceanScale);
    }

    private static double getRiverThinning(MapSample sample) {
        double riverThinning = MathUtils.smoothInOutQuad(Math.max(sample.continental(), MOUNTAIN_THRESHOLD), MOUNTAIN_THRESHOLD, MOUNTAIN_THRESHOLD + 0.1);
        if (sample.continental() > MOUNTAIN_THRESHOLD + 0.1) riverThinning = 1;
        if (sample.erosion() > FLATLAND_THRESHOLD && sample.erosion() < FLATLAND_THRESHOLD + 0.25)
            riverThinning = riverThinning * (1 - MathUtils.smoothInOutQuad(sample.erosion(), FLATLAND_THRESHOLD, FLATLAND_THRESHOLD + 0.25));
        else if (sample.erosion() >= FLATLAND_THRESHOLD + 0.25) riverThinning = 0;
        return riverThinning;
    }

    private WorldGeneration() {
    }
}