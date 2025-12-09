package game.server.generation;

import game.server.Chunk;
import game.server.biomes.Biome;
import game.server.biomes.*;
import game.utils.Status;
import game.utils.Utils;

import static game.utils.Constants.*;

public final class WorldGeneration {

    public static final int WATER_LEVEL = 1 << 21;
    public static long SEED;

    public static void generate(Chunk chunk) {
        if (chunk.getGenerationStatus() != Status.NOT_STARTED) return;
        generate(chunk, new GenerationData(chunk.X, chunk.Z, chunk.LOD));
    }

    public static void generate(Chunk chunk, GenerationData data) {
        if (chunk.getGenerationStatus() != Status.NOT_STARTED) return;
        chunk.setGenerationStatus(Status.IN_PROGRESS);

        data.setChunk(chunk);

        if (data.chunkContainsGround()) generateStone(data);
        if (data.chunkContainsBiome())
            for (int inChunkX = 0; inChunkX < CHUNK_SIZE; inChunkX++)
                for (int inChunkZ = 0; inChunkZ < CHUNK_SIZE; inChunkZ++) {
                    data.set(inChunkX, inChunkZ);
                    generateBiome(inChunkX, inChunkZ, data);
                }

        generateTrees(data);

        chunk.setMaterials(data.getCompressedMaterials());
        chunk.setGenerationStatus(Status.DONE);
    }

    public static int getResultingHeight(double height, double erosion, double continental, double river, double ridge) {
        height = (height * 0.5 + 0.5) * HEIGHT_MAP_MULTIPLIER;

        double continentalModifier = getContinentalModifier(continental, ridge);
        double erosionModifier = getErosionModifier(height, erosion, continentalModifier);
        double riverModifier = getRiverModifier(height, continentalModifier, erosionModifier, river);

        return Utils.floor((height + continentalModifier + erosionModifier + riverModifier) * 2) + WATER_LEVEL - 15;
    }

    public static int getResultingHeight(int totalX, int totalZ) {
        double height = GenerationData.heightMapValue(totalX, totalZ);
        double erosion = GenerationData.erosionMapValue(totalX, totalZ);
        double continental = GenerationData.continentalMapValue(totalX, totalZ);
        double river = GenerationData.riverMapValue(totalX, totalZ);
        double ridge = GenerationData.ridgeMapValue(totalX, totalZ);

        return getResultingHeight(height, erosion, continental, river, ridge);
    }

    public static int[] getResultingHeightMap(double[] heightMap, double[] erosionMap, double[] continentalMap, double[] riverMap, double[] ridgeMap) {
        int[] resultingHeightMap = new int[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ++) {

                int mapIndex = GenerationData.getMapIndex(mapX, mapZ);
                double height = heightMap[mapIndex];
                double erosion = erosionMap[mapIndex];
                double continental = continentalMap[mapIndex];
                double river = riverMap[mapIndex];
                double ridge = ridgeMap[mapIndex];

                int resultingHeight = getResultingHeight(height, erosion, continental, river, ridge);
                resultingHeightMap[mapIndex] = resultingHeight;
            }
        return resultingHeightMap;
    }

    public static Biome[] getBiomes(int[] heightMap, double[] featureMap, double[] humidityMap, double[] temperatureMap, double[] erosionMap, double[] continentalMap) {
        Biome[] biomes = new Biome[CHUNK_SIZE * CHUNK_SIZE];
        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int mapIndex = GenerationData.getMapIndex(mapX, mapZ);
                int index = mapX << CHUNK_SIZE_BITS | mapZ;
                biomes[index] = getBiome(
                        temperatureMap[mapIndex],
                        humidityMap[mapIndex],
                        WATER_LEVEL + (int) (featureMap[index] * 64.0) + 64,
                        heightMap[mapIndex],
                        erosionMap[mapIndex],
                        continentalMap[mapIndex]
                );
            }
        return biomes;
    }

    public static Biome getBiome(double temperature, double humidity, int beachHeight, int height, double erosion, double continental) {
        if (height < WATER_LEVEL) {
            if (temperature > 0.33) return WARM_OCEAN;
            else if (temperature < -0.33) return COLD_OCEAN;
            return OCEAN;
        }
        if (height < beachHeight) return BEACH;
        if (continental > MOUNTAIN_THRESHOLD && erosion < 0.425) {
            if (temperature > 0.33) return DRY_MOUNTAIN;
            else if (temperature < -0.33) return SNOWY_MOUNTAIN;
            return MOUNTAIN;
        }

        if (temperature > 0.33) {
            if (temperature > 0.45 && humidity < -0.3) return CORRODED_MESA;
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


    private static void generateBiome(int inChunkX, int inChunkZ, GenerationData data) {
        Biome biome = data.biome;
        int height = data.height;
        int start = Math.clamp((data.height - MAX_SURFACE_MATERIALS_DEPTH >> data.LOD) - ((long) data.chunkY << CHUNK_SIZE_BITS), 0, CHUNK_SIZE);

        for (int inChunkY = start; inChunkY < CHUNK_SIZE; inChunkY++) {
            data.computeTotalY(inChunkY);
            int totalY = data.totalY;

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

    private static void generateStone(GenerationData data) {
        int chunkStartX = data.chunkX << CHUNK_SIZE_BITS + data.LOD;
        int chunkStartY = data.chunkY << CHUNK_SIZE_BITS + data.LOD;
        int chunkStartZ = data.chunkZ << CHUNK_SIZE_BITS + data.LOD;

        for (int index = 0; index < CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE; index += 8 * 8 * 8) {
            int inChunkX = Utils.getInChunkX(index) << data.LOD;
            int inChunkY = Utils.getInChunkY(index) << data.LOD;
            int inChunkZ = Utils.getInChunkZ(index) << data.LOD;
            byte stoneType = GenerationData.getGeneratingStoneType(chunkStartX + inChunkX, chunkStartY + inChunkY, chunkStartZ + inChunkZ);
            data.storeConsecutive(index, 8 * 8 * 8, stoneType);
        }
    }

    private static void generateTrees(GenerationData data) {
        if (!data.hasTrees()) return;

        int sideLength = (1 << data.LOD) + 2;
        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++) {
                Tree tree = data.treeMapValue(x * sideLength + z);
                if (tree == null) continue;

                data.storeTree(tree);
            }
    }

    private static double getContinentalModifier(double continental, double ridge) {
        double continentalModifier = 0.0;
        // Mountains
        if (continental > MOUNTAIN_THRESHOLD)
            continentalModifier = (continental - MOUNTAIN_THRESHOLD) * (continental - MOUNTAIN_THRESHOLD) * ridge * 100000;
            // Normal ocean
        else if (continental < OCEAN_THRESHOLD && continental > OCEAN_THRESHOLD - 0.05)
            continentalModifier = Utils.smoothInOutQuad(-continental, -OCEAN_THRESHOLD, -OCEAN_THRESHOLD + 0.05) * OCEAN_FLOOR_OFFSET;
        else if (continental <= OCEAN_THRESHOLD - 0.05 && continental > OCEAN_THRESHOLD - 0.2)
            continentalModifier = (continental - (OCEAN_THRESHOLD - 0.05)) * 100 + OCEAN_FLOOR_OFFSET;
            // Deep Ocean
        else if (continental <= OCEAN_THRESHOLD - 0.2 && continental > OCEAN_THRESHOLD - 0.25)
            continentalModifier = Utils.smoothInOutQuad(-continental, -OCEAN_THRESHOLD + 0.2, -OCEAN_THRESHOLD + 0.25) * DEEP_OCEAN_FLOOR_OFFSET + OCEAN_FLOOR_OFFSET - 15;
        else if (continental <= OCEAN_THRESHOLD - 0.25)
            continentalModifier = (continental - (OCEAN_THRESHOLD - 0.25)) * 100 + OCEAN_FLOOR_OFFSET + DEEP_OCEAN_FLOOR_OFFSET - 15;
        return continentalModifier;
    }

    private static double getErosionModifier(double height, double erosion, double continentalModifier) {
        double erosionModifier = 0.0;
        // Elevated areas
        if (erosion < -0.25 && erosion > -0.4) erosionModifier = Utils.smoothInOutQuad(-erosion, 0.25, 0.4) * 55;
        else if (erosion <= -0.40) erosionModifier = (erosion + 0.40) * 20 + 55;
            // Flatland
        else if (erosion > FLATLAND_THRESHOLD && erosion < FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -(continentalModifier + height * 0.75 - FLATLAND_OFFSET) * Utils.smoothInOutQuad(erosion, FLATLAND_THRESHOLD, FLATLAND_THRESHOLD + 0.25);
        else if (erosion >= FLATLAND_THRESHOLD + 0.25)
            erosionModifier = -height * 0.75 - continentalModifier + FLATLAND_OFFSET;
        return erosionModifier;
    }

    private static double getRiverModifier(double height, double continentalModifier, double erosionModifier, double river) {
        double riverModifier = 0.0;
        if (Math.abs(river) < 0.005)
            riverModifier = -height * 0.85 - continentalModifier - erosionModifier + RIVER_OFFSET;
        else if (Math.abs(river) < RIVER_THRESHOLD)
            riverModifier = -(continentalModifier + erosionModifier + height * 0.85 - RIVER_OFFSET) * (1 - Utils.smoothInOutQuad(Math.abs(river), 0.005, RIVER_THRESHOLD));
        return riverModifier;
    }


    private static final int MAX_SURFACE_MATERIALS_DEPTH = 132;
    private static final int OCEAN_FLOOR_OFFSET = -480;
    private static final int DEEP_OCEAN_FLOOR_OFFSET = -1120;
    private static final int FLATLAND_OFFSET = 130;
    private static final int RIVER_OFFSET = -200;

    private static final double HEIGHT_MAP_MULTIPLIER = 250;

    private static final double MOUNTAIN_THRESHOLD = 0.3;    // Continental
    private static final double OCEAN_THRESHOLD = -0.3;      // Continental
    private static final double FLATLAND_THRESHOLD = 0.3;    // Erosion
    private static final double RIVER_THRESHOLD = 0.1;       // Erosion

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