package game.server.generation;

import core.utils.OpenSimplex2S;
import game.server.Chunk;
import game.server.MaterialsData;
import game.server.biomes.Biome;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.Arrays;

import static game.server.generation.WorldGeneration.SEED;
import static game.utils.Constants.*;

public final class GenerationData {

    public double temperature;
    public double humidity;
    public double feature;
    public double erosion;
    public double continental;

    public int height, specialHeight;
    public byte steepness;
    public int chunkX, chunkY, chunkZ;
    public int totalX, totalY, totalZ;

    public final int LOD;

    public GenerationData(int chunkX, int chunkZ, int lod) {
        this.LOD = lod;

        chunkX &= MAX_CHUNKS_XZ >> lod;
        chunkZ &= MAX_CHUNKS_XZ >> lod;

        featureMap = featureMap(chunkX, chunkZ, lod);
        treeMap = treeMap(chunkX, chunkZ, lod);

        temperatureMap = temperatureMapPadded(chunkX, chunkZ, lod);
        humidityMap = humidityMapPadded(chunkX, chunkZ, lod);
        erosionMap = erosionMapPadded(chunkX, chunkZ, lod);
        continentalMap = continentalMapPadded(chunkX, chunkZ, lod);
        double[] heightMap = heightMapPadded(chunkX, chunkZ, lod);
        double[] riverMap = riverMapPadded(chunkX, chunkZ, lod);
        double[] ridgeMap = ridgeMapPadded(chunkX, chunkZ, lod);

        resultingHeightMap = WorldGeneration.getResultingHeightMap(heightMap, erosionMap, continentalMap, riverMap, ridgeMap);
        steepnessMap = steepnessMap(resultingHeightMap, lod);
    }

    public void setChunk(Chunk chunk) {
        chunkX = chunk.X;
        chunkY = chunk.Y;
        chunkZ = chunk.Z;

        Arrays.fill(uncompressedMaterials, AIR);
        Arrays.fill(cachedMaterials, AIR);
        Arrays.fill(cachedStoneMaterials, AIR);
    }

    public void set(int inChunkX, int inChunkZ) {
        int index = inChunkX << CHUNK_SIZE_BITS | inChunkZ;

        totalX = (chunkX << CHUNK_SIZE_BITS | inChunkX) << LOD;
        totalZ = (chunkZ << CHUNK_SIZE_BITS | inChunkZ) << LOD;

        feature = featureMap[index];
        steepness = steepnessMap[index];

        temperature = temperatureMap[getMapIndex(inChunkX + 1, inChunkZ + 1)];
        humidity = humidityMap[getMapIndex(inChunkX + 1, inChunkZ + 1)];
        erosion = erosionMap[getMapIndex(inChunkX + 1, inChunkZ + 1)];
        continental = continentalMap[getMapIndex(inChunkX + 1, inChunkZ + 1)];
        height = resultingHeightMap[getMapIndex(inChunkX + 1, inChunkZ + 1)];
    }

    public void setBiome(Biome biome) {
        specialHeight = biome.getSpecialHeight(totalX, totalZ, this);
    }

    public void computeTotalY(int inChunkY) {
        totalY = (chunkY << CHUNK_SIZE_BITS | inChunkY) << LOD;
    }


    public static double heightMapValue(int totalX, int totalZ) {
        double height;
        height = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x08D2BCC9BD98BBF5L, totalX * HEIGHT_MAP_FREQUENCY, totalZ * HEIGHT_MAP_FREQUENCY, 0);
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCEC793764665EF7DL, totalX * HEIGHT_MAP_FREQUENCY * 2, totalZ * HEIGHT_MAP_FREQUENCY * 2, 0) * 0.5;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBD4957D70308DEBFL, totalX * HEIGHT_MAP_FREQUENCY * 4, totalZ * HEIGHT_MAP_FREQUENCY * 4, 0) * 0.25;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xD68F54787A92D53CL, totalX * HEIGHT_MAP_FREQUENCY * 8, totalZ * HEIGHT_MAP_FREQUENCY * 8, 0) * 0.125;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x574730707031DA54L, totalX * HEIGHT_MAP_FREQUENCY * 16, totalZ * HEIGHT_MAP_FREQUENCY * 16, 0) * 0.0625;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF82698C39EE31D97L, totalX * HEIGHT_MAP_FREQUENCY * 32, totalZ * HEIGHT_MAP_FREQUENCY * 32, 0) * 0.03125;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x6F51382316D4C57FL, totalX * HEIGHT_MAP_FREQUENCY * 64, totalZ * HEIGHT_MAP_FREQUENCY * 64, 0) * 0.015625;
        height += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x09D355804F5FB2F7L, totalX * HEIGHT_MAP_FREQUENCY * 128, totalZ * HEIGHT_MAP_FREQUENCY * 128, 0) * 0.0078125;
        return height;
    }

    public static double continentalMapValue(int totalX, int totalZ) {
        double continental;
        continental = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCF71B60E764BFC2CL, totalX * CONTINENTAL_FREQUENCY, totalZ * CONTINENTAL_FREQUENCY, 0) * 0.9588;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8EF1C1F90DA10C0AL, totalX * CONTINENTAL_FREQUENCY * 6, totalZ * CONTINENTAL_FREQUENCY * 6, 0) * 0.0411;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x608308CA890553E3L, totalX * CONTINENTAL_FREQUENCY * 12, totalZ * CONTINENTAL_FREQUENCY * 12, 0) * 0.0211;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xE29B01A5152C8664L, totalX * CONTINENTAL_FREQUENCY * 24, totalZ * CONTINENTAL_FREQUENCY * 24, 0) * 0.0111;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x27C1986D27551225L, totalX * CONTINENTAL_FREQUENCY * 48, totalZ * CONTINENTAL_FREQUENCY * 48, 0) * 0.00511;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x33382D4F463883B8L, totalX * CONTINENTAL_FREQUENCY * 160, totalZ * CONTINENTAL_FREQUENCY * 160, 0) * 0.00111;
        return continental;
    }

    public static double riverMapValue(int totalX, int totalZ) {
        double river;
        river = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x84D43603ED399321L, totalX * RIVER_FREQUENCY, totalZ * RIVER_FREQUENCY, 0) * 0.9588;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7C46A6B469AC4A05L, totalX * RIVER_FREQUENCY * 50, totalZ * RIVER_FREQUENCY * 50, 0) * 0.0411;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x14CBFBB4AF4AB8D4L, totalX * RIVER_FREQUENCY * 200, totalZ * RIVER_FREQUENCY * 200, 0) * 0.0111;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBC183CA6F3488FCAL, totalX * RIVER_FREQUENCY * 400, totalZ * RIVER_FREQUENCY * 400, 0) * 0.0051;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x09340E1C502CED3CL, totalX * RIVER_FREQUENCY * 800, totalZ * RIVER_FREQUENCY * 800, 0) * 0.0025;
        return river;
    }

    public static double ridgeMapValue(int totalX, int totalZ) {
        double ridge;
        ridge = (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xDD4D88700A5E4D7EL, totalX * RIDGE_FREQUENCY, totalZ * RIDGE_FREQUENCY, 0))) * 0.5;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8A3E12DE957E78C5L, totalX * RIDGE_FREQUENCY * 2, totalZ * RIDGE_FREQUENCY * 2, 0))) * 0.25;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x0A8E80B850A75321L, totalX * RIDGE_FREQUENCY * 4, totalZ * RIDGE_FREQUENCY * 4, 0))) * 0.125;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x6E0744EACB517937L, totalX * RIDGE_FREQUENCY * 8, totalZ * RIDGE_FREQUENCY * 8, 0))) * 0.0625;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBCCFDBF01B87426FL, totalX * RIDGE_FREQUENCY * 64, totalZ * RIDGE_FREQUENCY * 64, 0))) * 0.0390625;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7F36866E4079518BL, totalX * RIDGE_FREQUENCY * 128, totalZ * RIDGE_FREQUENCY * 128, 0))) * 0.01953125;
        return ridge;
    }

    public static double erosionMapValue(int totalX, int totalZ) {
        double erosion;
        erosion = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBEF86CF6C75F708DL, totalX * EROSION_FREQUENCY, totalZ * EROSION_FREQUENCY, 0) * 0.9588;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x60E4A215EA2087BCL, totalX * EROSION_FREQUENCY * 40, totalZ * EROSION_FREQUENCY * 40, 0) * 0.0411;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x75A0E541F1E10B53L, totalX * EROSION_FREQUENCY * 160, totalZ * EROSION_FREQUENCY * 160, 0) * 0.0111;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xD5398D722513F0A3L, totalX * EROSION_FREQUENCY * 320, totalZ * EROSION_FREQUENCY * 320, 0) * 0.0051;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x3084497B496D8532L, totalX * EROSION_FREQUENCY * 640, totalZ * EROSION_FREQUENCY * 640, 0) * 0.0025;
        return erosion;
    }

    public static double temperatureMapValue(int totalX, int totalZ) {
        double temperature;
        temperature = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xADA1CE5C24C4A44FL, totalX * TEMPERATURE_FREQUENCY, totalZ * TEMPERATURE_FREQUENCY, 0) * 0.8888;
        temperature += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEEA0CB5D51C0A447L, totalX * TEMPERATURE_FREQUENCY * 50, totalZ * TEMPERATURE_FREQUENCY * 50, 0) * 0.1111;
        return temperature;
    }

    public static double humidityMapValue(int totalX, int totalZ) {
        double humidity;
        humidity = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x41C8F1921D50DF82L, totalX * HUMIDITY_FREQUENCY, totalZ * HUMIDITY_FREQUENCY, 0) * 0.8888;
        humidity += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xB935E00850C8416EL, totalX * HUMIDITY_FREQUENCY * 50, totalZ * HUMIDITY_FREQUENCY * 50, 0) * 0.1111;
        return humidity;
    }


    public boolean isBelowFloorMaterialLevel(int totalY, int floorMaterialDepth) {
        return totalY >> LOD < height - floorMaterialDepth >> LOD;
    }

    public boolean isInsideSurfaceMaterialLevel(int totalY, int surfaceMaterialDepth) {
        return totalY >> LOD >= height - surfaceMaterialDepth >> LOD;
    }

    public boolean isAboveSurface(int totalY) {
        return totalY >> LOD > height >> LOD;
    }

    public boolean hasTrees() {
        return treeMap != null;
    }

    public int getFloorMaterialDepthMod() {
        return (int) (feature * 4.0F) - (steepness << 2);
    }

    public static int getMapIndex(int mapX, int mapZ) {
        return mapX * CHUNK_SIZE_PADDED + mapZ;
    }

    public void store(int inChunkX, int inChunkY, int inChunkZ, byte material) {
        uncompressedMaterials[inChunkX << CHUNK_SIZE_BITS * 2 | inChunkZ << CHUNK_SIZE_BITS | inChunkY] = material;
    }

    public void storeTree(Tree tree) {
        int chunkStartX = chunkX << CHUNK_SIZE_BITS + LOD;
        int chunkStartY = chunkY << CHUNK_SIZE_BITS + LOD;
        int chunkStartZ = chunkZ << CHUNK_SIZE_BITS + LOD;

        int chunkMaxY = chunkY + 1 << CHUNK_SIZE_BITS + LOD;
        if (chunkStartY > tree.getMaxY() || chunkMaxY < tree.getMinY()) return;

        int inChunkX = Math.max(chunkStartX, tree.getMinX()) >> LOD & CHUNK_SIZE_MASK;
        int inChunkY = Math.max(chunkStartY, tree.getMinY()) >> LOD & CHUNK_SIZE_MASK;
        int inChunkZ = Math.max(chunkStartZ, tree.getMinZ()) >> LOD & CHUNK_SIZE_MASK;

        int startX = chunkStartX + (inChunkX << LOD) - tree.getMinX();
        int startY = chunkStartY + (inChunkY << LOD) - tree.getMinY();
        int startZ = chunkStartZ + (inChunkZ << LOD) - tree.getMinZ();

        int lengthX = Utils.min(tree.sizeX() - startX, CHUNK_SIZE - inChunkX << LOD, tree.sizeX());
        int lengthY = Utils.min(tree.sizeY() - startY, CHUNK_SIZE - inChunkY << LOD, tree.sizeY());
        int lengthZ = Utils.min(tree.sizeZ() - startZ, CHUNK_SIZE - inChunkZ << LOD, tree.sizeZ());
        if (lengthX <= 0 || lengthY <= 0 || lengthZ <= 0) return;

        Vector3i targetStart = new Vector3i(inChunkX, inChunkY, inChunkZ);
        Vector3i sourceStart = new Vector3i(startX, startY, startZ);
        Vector3i size = new Vector3i(lengthX, lengthY, lengthZ);

        MaterialsData.fillStructureMaterialsInto(uncompressedMaterials, tree.structure(), CHUNK_SIZE_BITS, tree.transform(), LOD, targetStart, sourceStart, size);
    }

    public MaterialsData getCompressedMaterials() {
        return MaterialsData.getCompressedMaterials(CHUNK_SIZE_BITS, uncompressedMaterials);
    }

    public Tree treeMapValue(int index) {
        return treeMap[index];
    }


    public byte getGeneratingStoneType(int x, int y, int z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedStoneMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x1FCA4F81678D9EFEL, x * STONE_TYPE_FREQUENCY, y * STONE_TYPE_FREQUENCY, z * STONE_TYPE_FREQUENCY);
        if (Math.abs(noise) < ANDESITE_THRESHOLD) material = ANDESITE;
        else if (noise > SLATE_THRESHOLD) material = SLATE;
        else if (noise < BLACKSTONE_THRESHOLD) material = BLACKSTONE;
        else material = STONE;

        cachedStoneMaterials[index] = material;
        return material;
    }

    public byte getOceanFloorMaterial(int x, int y, int z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x30CD70827706B4C0L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < SAND_THRESHOLD) material = SAND;
        else material = MUD;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getWarmOceanFloorMaterial(int x, int y, int z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEB26D0A3459AAA03L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < MUD_THRESHOLD) material = MUD;
        else material = SAND;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getColdOceanFloorMaterial(int x, int y, int z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7A182AB93793E000L, x * MUD_TYPE_FREQUENCY, y * MUD_TYPE_FREQUENCY, z * MUD_TYPE_FREQUENCY);
        if (Math.abs(noise) < GRAVEL_THRESHOLD) material = GRAVEL;
        else if (noise > CLAY_THRESHOLD) material = CLAY;
        else if (noise < MUD_THRESHOLD) material = MUD;
        else material = GRAVEL;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getGeneratingDirtType(int x, int y, int z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF88966EA665D953EL, x * DIRT_TYPE_FREQUENCY, y * DIRT_TYPE_FREQUENCY, z * DIRT_TYPE_FREQUENCY);
        if (Math.abs(noise) < COURSE_DIRT_THRESHOLD) material = COURSE_DIRT;
        else material = DIRT;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getGeneratingIceType(int x, int y, int z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xD6744EFC8D01AEFCL, x * ICE_TYPE_FREQUENCY, y * ICE_TYPE_FREQUENCY, z * ICE_TYPE_FREQUENCY);
        if (noise > HEAVY_ICE_THRESHOLD) material = HEAVY_ICE;
        else material = ICE;

        cachedMaterials[index] = material;
        return material;
    }

    public byte getGeneratingGrassType(int x, int y, int z) {
        int index = getCompressedIndex(x, y, z);
        byte material = cachedMaterials[index];
        if (material != AIR) return material;

        // Generate if not yet generated
        double noise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEFB13EFD3B5AC7A7L, x * GRASS_TYPE_FREQUENCY, y * GRASS_TYPE_FREQUENCY, z * GRASS_TYPE_FREQUENCY);
        noise += feature * 0.4 - 0.2;
        if (Math.abs(noise) < MOSS_THRESHOLD) material = MOSS;
        else material = GRASS;

        cachedMaterials[index] = material;
        return material;
    }


    private static double[] temperatureMapPadded(int chunkX, int chunkZ, int lod) {
        double[] temperatureMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += 5) {
                int totalX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int totalZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                temperatureMap[getMapIndex(mapX, mapZ)] = temperatureMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += 5) interpolate(temperatureMap, mapX, mapZ);

        return temperatureMap;
    }

    private static double[] humidityMapPadded(int chunkX, int chunkZ, int lod) {
        double[] humidityMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += 5) {
                int totalX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int totalZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                humidityMap[getMapIndex(mapX, mapZ)] = humidityMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += 5) interpolate(humidityMap, mapX, mapZ);

        return humidityMap;
    }

    private static double[] heightMapPadded(int chunkX, int chunkZ, int lod) {
        double[] heightMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += 5) {
                int totalX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int totalZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                heightMap[getMapIndex(mapX, mapZ)] = heightMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += 5) interpolate(heightMap, mapX, mapZ);

        return heightMap;
    }

    private static double[] erosionMapPadded(int chunkX, int chunkZ, int lod) {
        double[] erosionMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += 5) {
                int totalX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int totalZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                erosionMap[getMapIndex(mapX, mapZ)] = erosionMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += 5) interpolate(erosionMap, mapX, mapZ);
        return erosionMap;
    }

    private static double[] continentalMapPadded(int chunkX, int chunkZ, int lod) {
        double[] continentalMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += 5) {
                int totalX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int totalZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                continentalMap[getMapIndex(mapX, mapZ)] = continentalMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += 5) interpolate(continentalMap, mapX, mapZ);
        return continentalMap;
    }

    private static double[] riverMapPadded(int chunkX, int chunkZ, int lod) {
        double[] riverMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += 5) {
                int totalX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int totalZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                riverMap[getMapIndex(mapX, mapZ)] = riverMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += 5) interpolate(riverMap, mapX, mapZ);
        return riverMap;
    }

    private static double[] ridgeMapPadded(int chunkX, int chunkZ, int lod) {
        double[] ridgeMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += 5) {
                int totalX = (chunkX << chunkSizeBits) + mapX * gapSize - gapSize;
                int totalZ = (chunkZ << chunkSizeBits) + mapZ * gapSize - gapSize;

                ridgeMap[getMapIndex(mapX, mapZ)] = ridgeMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += 5)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += 5) interpolate(ridgeMap, mapX, mapZ);
        return ridgeMap;
    }

    private static double[] featureMap(int chunkX, int chunkZ, int lod) {
        double[] featureMap = new double[CHUNK_SIZE * CHUNK_SIZE];
        double inverseMaxValue = 1.0 / Integer.MAX_VALUE;

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int totalX = (chunkX << CHUNK_SIZE_BITS | mapX) << lod;
                int totalZ = (chunkZ << CHUNK_SIZE_BITS | mapZ) << lod;
                featureMap[mapX << CHUNK_SIZE_BITS | mapZ] = (double) Utils.hash(totalX, totalZ, (int) SEED ^ 0x5C34A7B3) * inverseMaxValue;
            }

        return featureMap;
    }

    private static byte[] steepnessMap(int[] heightMapPadded, int lod) {
        byte[] steepnessMap = new byte[CHUNK_SIZE * CHUNK_SIZE];

        for (int mapX = 0; mapX < CHUNK_SIZE; mapX++)
            for (int mapZ = 0; mapZ < CHUNK_SIZE; mapZ++) {
                int height = heightMapPadded[getMapIndex(mapX + 1, mapZ + 1)];
                int steepnessX = Math.max(Math.abs(height - heightMapPadded[getMapIndex(mapX, mapZ + 1)]), Math.abs(height - heightMapPadded[getMapIndex(mapX + 2, mapZ + 1)]));
                int steepnessZ = Math.max(Math.abs(height - heightMapPadded[getMapIndex(mapX + 1, mapZ)]), Math.abs(height - heightMapPadded[getMapIndex(mapX + 1, mapZ + 2)]));
                steepnessMap[mapX << CHUNK_SIZE_BITS | mapZ] = (byte) Math.max(steepnessX >> lod, steepnessZ >> lod);
            }

        return steepnessMap;
    }

    private static Tree[] treeMap(int chunkX, int chunkZ, int lod) {
        if (lod > MAX_TREE_LOD) return null;

        int sideLength = (1 << lod) + 2;
        Tree[] treeMap = new Tree[sideLength * sideLength];

        int treeStartX = (chunkX << CHUNK_SIZE_BITS + lod) - CHUNK_SIZE / 2;
        int treeStartZ = (chunkZ << CHUNK_SIZE_BITS + lod) - CHUNK_SIZE / 2;

        for (int x = 0; x < sideLength; x++)
            for (int z = 0; z < sideLength; z++) {
                int totalX = treeStartX + (x << CHUNK_SIZE_BITS);
                int totalZ = treeStartZ + (z << CHUNK_SIZE_BITS);

                treeMap[x * sideLength + z] = treeMapValue(totalX, totalZ);
            }
        return treeMap;
    }

    private static Tree treeMapValue(int totalX, int totalZ) {
        double temperature = temperatureMapValue(totalX, totalZ);
        double humidity = humidityMapValue(totalX, totalZ);
        double height = heightMapValue(totalX, totalZ);
        double erosion = erosionMapValue(totalX, totalZ);
        double continental = continentalMapValue(totalX, totalZ);
        double river = riverMapValue(totalX, totalZ);
        double ridge = ridgeMapValue(totalX, totalZ);

        int resultingHeight = WorldGeneration.getResultingHeight(height, erosion, continental, river, ridge);
        int heightPlusX = WorldGeneration.getResultingHeight(totalX + 1, totalZ);
        int heightPlusZ = WorldGeneration.getResultingHeight(totalX, totalZ + 1);
        int steepness = Math.max(Math.abs(resultingHeight - heightPlusX), Math.abs(resultingHeight - heightPlusZ));
        if (steepness != 0) return null;

        Biome biome = WorldGeneration.getBiome(temperature, humidity, 96, resultingHeight, erosion, continental);

        if ((Utils.hash(totalX, totalZ, (int) (SEED ^ 0x264F6E393FE89AAFL)) & biome.getRequiredTreeZeroBits()) != 0) return null;
        return biome.getGeneratingTree(totalX, resultingHeight, totalZ);
    }

    private static void interpolate(double[] map, int mapX, int mapZ) {
        double value1 = map[getMapIndex(mapX, mapZ)];
        double value2 = map[getMapIndex(mapX + 5, mapZ)];
        double value3 = map[getMapIndex(mapX, mapZ + 5)];
        double value4 = map[getMapIndex(mapX + 5, mapZ + 5)];

        for (int x = 0; x <= 5; x++) {
            double interpolatedLowXValue = (value2 * x + value1 * (5 - x)) * 0.2;
            double interpolatedHighXValue = (value4 * x + value3 * (5 - x)) * 0.2;

            for (int z = 0; z <= 5; z++) {
                double interpolatedValue = (interpolatedHighXValue * z + interpolatedLowXValue * (5 - z)) * 0.2;
                map[getMapIndex(mapX + x, mapZ + z)] = interpolatedValue;
            }
        }
    }

    private int getCompressedIndex(int x, int y, int z) {
        // >> 2 for compression and performance improvement
        int compressedX = (x >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedY = (y >> LOD & CHUNK_SIZE_MASK) >> 2;
        int compressedZ = (z >> LOD & CHUNK_SIZE_MASK) >> 2;

        return compressedX << CHUNK_SIZE_BITS * 2 - 4 | compressedZ << CHUNK_SIZE_BITS - 2 | compressedY;
    }


    private final Tree[] treeMap;
    private final double[] temperatureMap;
    private final double[] humidityMap;
    private final double[] featureMap;
    private final double[] erosionMap;
    private final double[] continentalMap;

    private final int[] resultingHeightMap;
    private final byte[] steepnessMap;
    private final byte[] cachedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE >> 3];
    private final byte[] cachedStoneMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE >> 3];

    private final byte[] uncompressedMaterials = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

    private static final double TEMPERATURE_FREQUENCY = 6.25E-5;
    private static final double HUMIDITY_FREQUENCY = TEMPERATURE_FREQUENCY;
    private static final double HEIGHT_MAP_FREQUENCY = 1.5625E-4;
    private static final double EROSION_FREQUENCY = 6.25E-5;
    private static final double CONTINENTAL_FREQUENCY = 1.5625E-5;
    private static final double RIVER_FREQUENCY = 3.125E-5;
    private static final double RIDGE_FREQUENCY = 6.125E-5;

    private static final double STONE_TYPE_FREQUENCY = 0.00125;
    private static final double ANDESITE_THRESHOLD = 0.1;
    private static final double SLATE_THRESHOLD = 0.6;
    private static final double BLACKSTONE_THRESHOLD = -0.6;

    private static final double MUD_TYPE_FREQUENCY = 0.0025;
    private static final double GRAVEL_THRESHOLD = 0.1;
    private static final double CLAY_THRESHOLD = 0.5;
    private static final double SAND_THRESHOLD = -0.5;
    private static final double MUD_THRESHOLD = -0.5;

    private static final double DIRT_TYPE_FREQUENCY = 0.003125;
    private static final double COURSE_DIRT_THRESHOLD = 0.15;

    private static final double GRASS_TYPE_FREQUENCY = 0.0015625;
    private static final double MOSS_THRESHOLD = 0.3;

    private static final double ICE_TYPE_FREQUENCY = 0.005;
    private static final double HEAVY_ICE_THRESHOLD = 0.6;
}
