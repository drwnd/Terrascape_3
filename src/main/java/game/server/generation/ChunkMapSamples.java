package game.server.generation;

import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.*;

public record ChunkMapSamples(float[] temperatureMap, float[] humidityMap,
                              float[] heightMap, float[] erosionMap, float[] continentalMap, float[] riverMap, float[] ridgeMap) {

    public ChunkMapSamples(long chunkX, long chunkZ, int lod) {
        this(
                mapPadded(chunkX, chunkZ, lod, MapSample::temperatureMapValue),
                mapPadded(chunkX, chunkZ, lod, MapSample::humidityMapValue),
                mapPadded(chunkX, chunkZ, lod, MapSample::heightMapValue),
                mapPadded(chunkX, chunkZ, lod, MapSample::erosionMapValue),
                mapPadded(chunkX, chunkZ, lod, MapSample::continentalMapValue),
                mapPadded(chunkX, chunkZ, lod, MapSample::riverMapValue),
                mapPadded(chunkX, chunkZ, lod, MapSample::ridgeMapValue)
        );
    }

    public MapSample getSample(int mapIndex) {
        return new MapSample(
                temperatureMap[mapIndex],
                humidityMap[mapIndex],
                heightMap[mapIndex],
                erosionMap[mapIndex],
                continentalMap[mapIndex],
                riverMap[mapIndex],
                ridgeMap[mapIndex]
        );
    }


    private static float[] mapPadded(long chunkX, long chunkZ, int lod, MapValueFunction function) {
        float[] map = new float[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;
        int stepSize = Math.max(INTERPOLATION_SIZE, BLOCK_SIZE >> lod);

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += stepSize)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += stepSize) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                map[GenerationData.getMapIndex(mapX, mapZ)] = (float) function.mapValue(totalX, totalZ);
            }

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += stepSize)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += stepSize) interpolate(map, mapX, mapZ, stepSize, lod);

        return map;
    }

    private static void interpolate(float[] map, int mapX, int mapZ, int stepSize, int lod) {
        float value1 = map[GenerationData.getMapIndex(mapX, mapZ)];
        float value2 = map[GenerationData.getMapIndex(mapX + stepSize, mapZ)];
        float value3 = map[GenerationData.getMapIndex(mapX, mapZ + stepSize)];
        float value4 = map[GenerationData.getMapIndex(mapX + stepSize, mapZ + stepSize)];

        float interpolationMultiplier = 1.0F / stepSize;

        for (int x = 0; x <= stepSize; x++) {
            int xValue = x & (BLOCK_SIZE_MASK >> lod);
            float interpolatedLowXValue = (value2 * xValue + value1 * (stepSize - xValue)) * interpolationMultiplier;
            float interpolatedHighXValue = (value4 * xValue + value3 * (stepSize - xValue)) * interpolationMultiplier;

            for (int z = 0; z <= stepSize; z++) {
                int zValue = z & (BLOCK_SIZE_MASK >> lod);
                float interpolatedValue = (interpolatedHighXValue * zValue + interpolatedLowXValue * (stepSize - zValue)) * interpolationMultiplier;
                map[GenerationData.getMapIndex(mapX + x, mapZ + z)] = interpolatedValue;
            }
        }
    }

    private static final int INTERPOLATION_SIZE = 8;

    private interface MapValueFunction {
        double mapValue(long totalX, long totalY);
    }
}
