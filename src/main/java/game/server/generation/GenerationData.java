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

        double[] temperatureMap = temperatureMapPadded(chunkX, chunkZ, lod);
        double[] humidityMap = humidityMapPadded(chunkX, chunkZ, lod);
        double[] erosionMap = erosionMapPadded(chunkX, chunkZ, lod);
        double[] continentalMap = continentalMapPadded(chunkX, chunkZ, lod);
        double[] heightMap = heightMapPadded(chunkX, chunkZ, lod);
        double[] riverMap = riverMapPadded(chunkX, chunkZ, lod);
        double[] ridgeMap = ridgeMapPadded(chunkX, chunkZ, lod);

        resultingHeightMap = WorldGeneration.getResultingHeightMap(heightMap, erosionMap, continentalMap, riverMap, ridgeMap);
        steepnessMap = steepnessMap(resultingHeightMap, lod);
        biomeMap = WorldGeneration.getBiomes(resultingHeightMap, featureMap, humidityMap, temperatureMap, erosionMap, continentalMap);
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


    public static double heightMapValue(long totalX, long totalZ) {
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

    public static double continentalMapValue(long totalX, long totalZ) {
        double continental;
        continental = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCF71B60E764BFC2CL, totalX * CONTINENTAL_FREQUENCY, totalZ * CONTINENTAL_FREQUENCY, 0) * 0.9588;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8EF1C1F90DA10C0AL, totalX * CONTINENTAL_FREQUENCY * 6, totalZ * CONTINENTAL_FREQUENCY * 6, 0) * 0.0411;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x608308CA890553E3L, totalX * CONTINENTAL_FREQUENCY * 12, totalZ * CONTINENTAL_FREQUENCY * 12, 0) * 0.0211;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xE29B01A5152C8664L, totalX * CONTINENTAL_FREQUENCY * 24, totalZ * CONTINENTAL_FREQUENCY * 24, 0) * 0.0111;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x27C1986D27551225L, totalX * CONTINENTAL_FREQUENCY * 48, totalZ * CONTINENTAL_FREQUENCY * 48, 0) * 0.00511;
        continental += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x33382D4F463883B8L, totalX * CONTINENTAL_FREQUENCY * 160, totalZ * CONTINENTAL_FREQUENCY * 160, 0) * 0.00111;
        return continental;
    }

    public static double riverMapValue(long totalX, long totalZ) {
        double river;
        river = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x84D43603ED399321L, totalX * RIVER_FREQUENCY, totalZ * RIVER_FREQUENCY, 0) * 0.9588;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7C46A6B469AC4A05L, totalX * RIVER_FREQUENCY * 50, totalZ * RIVER_FREQUENCY * 50, 0) * 0.0411;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x14CBFBB4AF4AB8D4L, totalX * RIVER_FREQUENCY * 200, totalZ * RIVER_FREQUENCY * 200, 0) * 0.0111;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBC183CA6F3488FCAL, totalX * RIVER_FREQUENCY * 400, totalZ * RIVER_FREQUENCY * 400, 0) * 0.0051;
        river += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x09340E1C502CED3CL, totalX * RIVER_FREQUENCY * 800, totalZ * RIVER_FREQUENCY * 800, 0) * 0.0025;
        return river;
    }

    public static double ridgeMapValue(long totalX, long totalZ) {
        double ridge;
        ridge = (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xDD4D88700A5E4D7EL, totalX * RIDGE_FREQUENCY, totalZ * RIDGE_FREQUENCY, 0))) * 0.5;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x8A3E12DE957E78C5L, totalX * RIDGE_FREQUENCY * 2, totalZ * RIDGE_FREQUENCY * 2, 0))) * 0.25;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x0A8E80B850A75321L, totalX * RIDGE_FREQUENCY * 4, totalZ * RIDGE_FREQUENCY * 4, 0))) * 0.125;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x6E0744EACB517937L, totalX * RIDGE_FREQUENCY * 8, totalZ * RIDGE_FREQUENCY * 8, 0))) * 0.0625;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBCCFDBF01B87426FL, totalX * RIDGE_FREQUENCY * 64, totalZ * RIDGE_FREQUENCY * 64, 0))) * 0.0390625;
        ridge += (1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x7F36866E4079518BL, totalX * RIDGE_FREQUENCY * 128, totalZ * RIDGE_FREQUENCY * 128, 0))) * 0.01953125;
        return ridge;
    }

    public static double erosionMapValue(long totalX, long totalZ) {
        double erosion;
        erosion = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xBEF86CF6C75F708DL, totalX * EROSION_FREQUENCY, totalZ * EROSION_FREQUENCY, 0) * 0.9588;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x60E4A215EA2087BCL, totalX * EROSION_FREQUENCY * 40, totalZ * EROSION_FREQUENCY * 40, 0) * 0.0411;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x75A0E541F1E10B53L, totalX * EROSION_FREQUENCY * 160, totalZ * EROSION_FREQUENCY * 160, 0) * 0.0111;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xD5398D722513F0A3L, totalX * EROSION_FREQUENCY * 320, totalZ * EROSION_FREQUENCY * 320, 0) * 0.0051;
        erosion += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x3084497B496D8532L, totalX * EROSION_FREQUENCY * 640, totalZ * EROSION_FREQUENCY * 640, 0) * 0.0025;
        return erosion;
    }

    public static double temperatureMapValue(long totalX, long totalZ) {
        double temperature;
        temperature = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xADA1CE5C24C4A44FL, totalX * TEMPERATURE_FREQUENCY, totalZ * TEMPERATURE_FREQUENCY, 0) * 0.8888;
        temperature += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xEEA0CB5D51C0A447L, totalX * TEMPERATURE_FREQUENCY * 50, totalZ * TEMPERATURE_FREQUENCY * 50, 0) * 0.1111;
        temperature += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF7C6F9389CEEF1A7L, totalX * 0.03125, totalZ * 0.03125, 0) * 0.02;
        return temperature;
    }

    public static double humidityMapValue(long totalX, long totalZ) {
        double humidity;
        humidity = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x41C8F1921D50DF82L, totalX * HUMIDITY_FREQUENCY, totalZ * HUMIDITY_FREQUENCY, 0) * 0.8888;
        humidity += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xB935E00850C8416EL, totalX * HUMIDITY_FREQUENCY * 50, totalZ * HUMIDITY_FREQUENCY * 50, 0) * 0.1111;
        humidity += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x9BCC9E0E7A1F3A5CL, totalX * 0.03125, totalZ * 0.03125, 0) * 0.02;
        return humidity;
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


    private static double[] temperatureMapPadded(long chunkX, long chunkZ, int lod) {
        double[] temperatureMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                temperatureMap[getMapIndex(mapX, mapZ)] = temperatureMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(temperatureMap, mapX, mapZ);

        return temperatureMap;
    }

    private static double[] humidityMapPadded(long chunkX, long chunkZ, int lod) {
        double[] humidityMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                humidityMap[getMapIndex(mapX, mapZ)] = humidityMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(humidityMap, mapX, mapZ);

        return humidityMap;
    }

    private static double[] heightMapPadded(long chunkX, long chunkZ, int lod) {
        double[] heightMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                heightMap[getMapIndex(mapX, mapZ)] = heightMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(heightMap, mapX, mapZ);

        return heightMap;
    }

    private static double[] erosionMapPadded(long chunkX, long chunkZ, int lod) {
        double[] erosionMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                erosionMap[getMapIndex(mapX, mapZ)] = erosionMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(erosionMap, mapX, mapZ);
        return erosionMap;
    }

    private static double[] continentalMapPadded(long chunkX, long chunkZ, int lod) {
        double[] continentalMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                continentalMap[getMapIndex(mapX, mapZ)] = continentalMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(continentalMap, mapX, mapZ);
        return continentalMap;
    }

    private static double[] riverMapPadded(long chunkX, long chunkZ, int lod) {
        double[] riverMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                riverMap[getMapIndex(mapX, mapZ)] = riverMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(riverMap, mapX, mapZ);
        return riverMap;
    }

    private static double[] ridgeMapPadded(long chunkX, long chunkZ, int lod) {
        double[] ridgeMap = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        // Calculate actual values
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                ridgeMap[getMapIndex(mapX, mapZ)] = ridgeMapValue(totalX, totalZ);
            }

        // Interpolate values for every point
        // CHUNK_SIZE_PADDED - 1 to not write out of bounds
        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(ridgeMap, mapX, mapZ);
        return ridgeMap;
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

        Biome biome = WorldGeneration.getBiome(temperature, humidity, 96, resultingHeight, erosion, continental, 0);

        if ((MathUtils.hash((int) totalX, (int) totalZ, (int) (SEED ^ 0x264F6E393FE89AAFL)) & biome.getRequiredTreeZeroBits()) != 0) return null;
        return biome.getGeneratingTree(totalX, resultingHeight, totalZ);
    }

    private static void interpolate(double[] map, int mapX, int mapZ) {
        double value1 = map[getMapIndex(mapX, mapZ)];
        double value2 = map[getMapIndex(mapX + INTERPOLATION_SIZE, mapZ)];
        double value3 = map[getMapIndex(mapX, mapZ + INTERPOLATION_SIZE)];
        double value4 = map[getMapIndex(mapX + INTERPOLATION_SIZE, mapZ + INTERPOLATION_SIZE)];

        for (int x = 0; x <= INTERPOLATION_SIZE; x++) {
            double interpolatedLowXValue = (value2 * x + value1 * (INTERPOLATION_SIZE - x)) * INTERPOLATION_MULTIPLIER;
            double interpolatedHighXValue = (value4 * x + value3 * (INTERPOLATION_SIZE - x)) * INTERPOLATION_MULTIPLIER;

            for (int z = 0; z <= INTERPOLATION_SIZE; z++) {
                double interpolatedValue = (interpolatedHighXValue * z + interpolatedLowXValue * (INTERPOLATION_SIZE - z)) * INTERPOLATION_MULTIPLIER;
                map[getMapIndex(mapX + x, mapZ + z)] = interpolatedValue;
            }
        }
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

    private static final double TEMPERATURE_FREQUENCY = 1 / 16000.0;
    private static final double HUMIDITY_FREQUENCY = TEMPERATURE_FREQUENCY;
    private static final double HEIGHT_MAP_FREQUENCY = 1 / 6400.0;
    private static final double EROSION_FREQUENCY = 1 / 16000.0;
    private static final double CONTINENTAL_FREQUENCY = 1 / 64000.0;
    private static final double RIVER_FREQUENCY = 1 / 32000.0;
    private static final double RIDGE_FREQUENCY = 1 / 16300.0;

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

    private static final int INTERPOLATION_SIZE = 8;
    private static final double INTERPOLATION_MULTIPLIER = 1.0 / INTERPOLATION_SIZE;
}
