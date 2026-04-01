package game.server.generation;

import static game.utils.Constants.*;

public record ChunkMapSamples(double[] temperatureMap, double[] humidityMap,
                              double[] heightMap, double[] erosionMap, double[] continentalMap, double[] riverMap, double[] ridgeMap) {

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

    private static double[] mapPadded(long chunkX, long chunkZ, int lod, MapValueFunction function) {
        double[] map = new double[CHUNK_SIZE_PADDED * CHUNK_SIZE_PADDED];
        int chunkSizeBits = CHUNK_SIZE_BITS + lod;
        int gapSize = 1 << lod;

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED; mapZ += INTERPOLATION_SIZE) {
                long totalX = (chunkX << chunkSizeBits) + (long) mapX * gapSize - gapSize;
                long totalZ = (chunkZ << chunkSizeBits) + (long) mapZ * gapSize - gapSize;

                map[GenerationData.getMapIndex(mapX, mapZ)] = function.mapValue(totalX, totalZ);
            }

        for (int mapX = 0; mapX < CHUNK_SIZE_PADDED - 1; mapX += INTERPOLATION_SIZE)
            for (int mapZ = 0; mapZ < CHUNK_SIZE_PADDED - 1; mapZ += INTERPOLATION_SIZE) interpolate(map, mapX, mapZ);

        return map;
    }

    private static void interpolate(double[] map, int mapX, int mapZ) {
        double value1 = map[GenerationData.getMapIndex(mapX, mapZ)];
        double value2 = map[GenerationData.getMapIndex(mapX + INTERPOLATION_SIZE, mapZ)];
        double value3 = map[GenerationData.getMapIndex(mapX, mapZ + INTERPOLATION_SIZE)];
        double value4 = map[GenerationData.getMapIndex(mapX + INTERPOLATION_SIZE, mapZ + INTERPOLATION_SIZE)];

        for (int x = 0; x <= INTERPOLATION_SIZE; x++) {
            double interpolatedLowXValue = (value2 * x + value1 * (INTERPOLATION_SIZE - x)) * INTERPOLATION_MULTIPLIER;
            double interpolatedHighXValue = (value4 * x + value3 * (INTERPOLATION_SIZE - x)) * INTERPOLATION_MULTIPLIER;

            for (int z = 0; z <= INTERPOLATION_SIZE; z++) {
                double interpolatedValue = (interpolatedHighXValue * z + interpolatedLowXValue * (INTERPOLATION_SIZE - z)) * INTERPOLATION_MULTIPLIER;
                map[GenerationData.getMapIndex(mapX + x, mapZ + z)] = interpolatedValue;
            }
        }
    }

    private static final int INTERPOLATION_SIZE = 8;
    private static final double INTERPOLATION_MULTIPLIER = 1.0 / INTERPOLATION_SIZE;

    private interface MapValueFunction {
        double mapValue(long totalX, long totalY);
    }
}
