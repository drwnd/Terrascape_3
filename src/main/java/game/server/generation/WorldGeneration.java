package game.server.generation;

import core.utils.MathUtils;
import game.server.Chunk;
import game.server.MaterialsData;
import game.server.biomes.Biome;
import game.server.biomes.*;
import game.utils.Status;
import game.utils.Utils;

import static game.utils.Constants.*;

public final class WorldGeneration {

    public static final int WATER_LEVEL = 0;
    public static final int MAX_SURFACE_MATERIALS_DEPTH = 132;
    public static long SEED;

    public static void generate(Chunk chunk) {
        if (chunk.getGenerationStatus() != Status.NOT_STARTED) return;
        generate(chunk, new GenerationData(chunk.X, chunk.Z, chunk.LOD));
    }

    public static void generate(Chunk chunk, GenerationData data) {
        if (chunk.getGenerationStatus() != Status.NOT_STARTED) return;
        chunk.setGenerationStatus(Status.IN_PROGRESS);

        data.setChunk(chunk);
        boolean chunkContainsVoxels = false;

        if (data.chunkContainsGround()) {
            generateStone(data);
            chunkContainsVoxels = true;
        }

        boolean containsBiome = data.chunkContainsBiome(), containsRiver = data.containsUndergroundRiver();
        if (containsBiome || containsRiver) {
            for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX++)
                for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ++) {
                    data.set(inChunkX, inChunkZ);
                    if (containsBiome) generateBiome(inChunkX, inChunkZ, data);
                    if (containsRiver) generateUndergroundRiver(inChunkX, inChunkZ, data);
                }
            chunkContainsVoxels = true;
        }

        chunkContainsVoxels |= generateTrees(data);

        chunk.setMaterials(chunkContainsVoxels ? data.getCompressedMaterials() : new MaterialsData(CHUNK_SIZE_BITS, AIR));
        chunk.setGenerationStatus(Status.DONE);
    }

    public static int getResultingHeight(MapSample sample) {
        double continentalModifier = getContinentalModifier(sample);
        double erosionModifier = getErosionModifier(continentalModifier, sample);
        double riverModifier = getRiverModifier(erosionModifier, continentalModifier, sample);

        return MathUtils.floor((sample.height() + continentalModifier + erosionModifier + riverModifier) * 2) + WATER_LEVEL - 15;
    }

    public static int getResultingHeight(long totalX, long totalZ) {
        return getResultingHeight(new MapSample(totalX, totalZ, false, true));
    }

    public static int[] getResultingHeightMap(ChunkMapSamples samples) {
        int[] resultingHeightMap = new int[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {

                int mapIndex = GenerationData.getMapIndex(mapX, mapZ);
                int resultingHeight = getResultingHeight(samples.getSample(mapIndex));
                resultingHeightMap[mapIndex] = resultingHeight;
            }
        return resultingHeightMap;
    }

    public static int[] getUndergroundRiverDepthMap(ChunkMapSamples samples) {
        float[] riverMap = samples.riverMap();
        int[] riverDepthMap = new int[riverMap.length];
        for (int index = 0; index < riverDepthMap.length; index++) riverDepthMap[index] = getRiverDepth(riverMap[index]);
        return riverDepthMap;
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
        double dither = feature * 0.05 - 0.025;

        double temperature = sample.temperature() + dither;
        double humidity = sample.humidity() + dither;
        double continental = sample.continental() - Math.abs(dither);
        double erosion = sample.erosion() + dither;
        int beachHeight = WATER_LEVEL + 64 + (int) (feature * 64);

        if (height < WATER_LEVEL) {
            if (temperature > 0.33) return WARM_OCEAN;
            else if (temperature - dither < -0.33) return COLD_OCEAN;
            return OCEAN;
        }
        if (height < beachHeight) return BEACH;
        if (continental > MOUNTAIN_THRESHOLD && erosion < 0.51) {
            if (temperature > 0.33) return DRY_MOUNTAIN;
            else if (temperature < -0.33) return SNOWY_MOUNTAIN;
            return MOUNTAIN;
        }

        if (temperature > 0.33) {
            if (height > 128 && sample.continental() < MOUNTAIN_THRESHOLD
                    && sample.temperature() > 0.45 && sample.humidity() < -0.3) return CORRODED_MESA;
            if (temperature > 0.55 && humidity < 0.15) return MESA;
            if (humidity < 0.15) return DESERT;
            if (humidity > 0.5 && temperature > 0.5) return BLACK_WOOD_FOREST;
            if (humidity > 0.4 && temperature > 0.4) return DARK_OAK_FOREST;
            return WASTELAND;
        }
        if (humidity > 0.33) {
            if (temperature > -0.1) return REDWOOD_FOREST;
            if (temperature > -0.4) return SPRUCE_FOREST;
            return SNOWY_SPRUCE_FOREST;
        }
        if (humidity < 0.0 && temperature > -0.25) return PLAINS;
        if (humidity > -0.33 && temperature > -0.33) return OAK_FOREST;
        if (humidity < -0.33 && temperature > -0.5) return PINE_FOREST;
        return SNOWY_PLAINS;
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
        int start = Math.clamp(height - data.floorMaterialDepth - (data.chunkY << CHUNK_SIZE_BITS + data.LOD) >> data.LOD, 0, CHUNK_SIZE);

        for (int inChunkY = start; inChunkY < CHUNK_SIZE; inChunkY++) {
            data.computeTotalY(inChunkY);
            long totalY = data.totalY;

            // Attempting to place biome specific materials and features
            if (biome.placeMaterial(inChunkX, inChunkY, inChunkZ, data)) continue;
            // Placing stone beneath surface materials
            if (totalY <= height) continue;
            // Reached surface, everything above is just air
            if (totalY >= WATER_LEVEL) {
                data.fillAboveWithAir(inChunkX, inChunkY, inChunkZ);
                break;
            }
            // Filling Oceans with water
            else data.store(inChunkX, inChunkY, inChunkZ, WATER);
        }
    }

    private static void generateUndergroundRiver(int inChunkX, int inChunkZ, GenerationData data) {
        int start = Math.clamp(-data.undergroundRiverDepth + WATER_LEVEL - (data.chunkY << CHUNK_SIZE_BITS + data.LOD) >> data.LOD, 0, CHUNK_SIZE);
        int end = Math.clamp(data.undergroundRiverDepth + WATER_LEVEL - (data.chunkY << CHUNK_SIZE_BITS + data.LOD) >> data.LOD, 0, CHUNK_SIZE);

        for (int inChunkY = start; inChunkY < end; inChunkY++) {
            data.computeTotalY(inChunkY);
            data.store(inChunkX, inChunkY, inChunkZ, data.totalY < 0 ? WATER : AIR);
        }
    }

    private static boolean generateTrees(GenerationData data) {
        if (!data.hasTrees()) return false;
        boolean hasGeneratedTree = false;

        int sideLength = (1 << data.LOD) + 2;
        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++) {
                Tree tree = data.treeMapValue(x * sideLength + z);
                if (tree == null) continue;

                hasGeneratedTree |= data.storeTree(tree);
            }
        return hasGeneratedTree;
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
        if (erosion < -0.25 && erosion > -0.4) erosionModifier = MathUtils.smoothInOutQuad(-erosion, 0.25, 0.4) * 55;
        else if (erosion <= -0.40) erosionModifier = (erosion + 0.40) * 20 + 55;
            // Flatland
        else if (erosion > FLATLAND_THRESHOLD && erosion < FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -(continentalModifier + height * 0.75 - FLATLAND_OFFSET) * MathUtils.smoothInOutQuad(erosion, FLATLAND_THRESHOLD, FLATLAND_THRESHOLD + 0.25);
        else if (erosion >= FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -height * 0.75 - continentalModifier + FLATLAND_OFFSET;
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

    private static int getRiverDepth(double river) {
        return (int) (Math.sqrt(Math.max(0, UNDERGROUND_RIVER_THRESHOLD - river)) * 1500);
    }


    private static final int OCEAN_FLOOR_OFFSET = -480;
    private static final int DEEP_OCEAN_FLOOR_OFFSET = -1120;
    private static final int FLATLAND_OFFSET = 330;
    private static final int RIVER_OFFSET = -200;

    private static final double MOUNTAIN_THRESHOLD = 0.3;
    private static final double OCEAN_THRESHOLD = -0.3;
    private static final double FLATLAND_THRESHOLD = 0.3;
    private static final double RIVER_THRESHOLD = 0.1;
    private static final double INNER_RIVER_THRESHOLD = 0.005;
    static final double UNDERGROUND_RIVER_THRESHOLD = 0.012;

    private static final Biome DESERT = new Desert();
    private static final Biome WASTELAND = new Wasteland();
    private static final Biome DARK_OAK_FOREST = new DarkOakForest();
    private static final Biome SNOWY_SPRUCE_FOREST = new SnowySpruceForest();
    private static final Biome SNOWY_PLAINS = new SnowyPlains();
    private static final Biome SPRUCE_FOREST = new SpruceForest();
    private static final Biome PLAINS = new Plains();
    private static final Biome OAK_FOREST = new OakForest();
    private static final Biome WARM_OCEAN = new WarmOcean();
    private static final Biome COLD_OCEAN = new ColdOcean();
    private static final Biome OCEAN = new Ocean();
    private static final Biome DRY_MOUNTAIN = new DryMountain();
    private static final Biome SNOWY_MOUNTAIN = new SnowyMountain();
    private static final Biome MOUNTAIN = new Mountain();
    private static final Biome MESA = new Mesa();
    private static final Biome CORRODED_MESA = new CorrodedMesa();
    private static final Biome BEACH = new Beach();
    private static final Biome PINE_FOREST = new PineForest();
    private static final Biome REDWOOD_FOREST = new RedwoodForest();
    private static final Biome BLACK_WOOD_FOREST = new BlackWoodForest();

    private WorldGeneration() {
    }
}