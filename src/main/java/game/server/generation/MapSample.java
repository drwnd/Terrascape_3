package game.server.generation;

import core.utils.OpenSimplex2S;

import static game.server.generation.WorldGeneration.SEED;

public record MapSample(double temperature, double humidity,
                        double height, double erosion, double continental, double river, double ridge) {

    public static final int GENERATE_HEIGHT_MAPS = 1;
    public static final int GENERATE_BIOME_MAPS = 2;

    public MapSample(long totalX, long totalZ, int flags) {
        this(
                (flags & GENERATE_BIOME_MAPS) == 0 ? 0.0 : temperatureMapValue(totalX, totalZ),
                (flags & GENERATE_BIOME_MAPS) == 0 ? 0.0 : humidityMapValue(totalX, totalZ),
                (flags & GENERATE_HEIGHT_MAPS) == 0 ? 0.0 : heightMapValue(totalX, totalZ),
                (flags & GENERATE_HEIGHT_MAPS) == 0 ? 0.0 : erosionMapValue(totalX, totalZ),
                (flags & GENERATE_HEIGHT_MAPS) == 0 ? 0.0 : continentalMapValue(totalX, totalZ),
                (flags & GENERATE_HEIGHT_MAPS) == 0 ? 0.0 : riverMapValue(totalX, totalZ),
                (flags & GENERATE_HEIGHT_MAPS) == 0 ? 0.0 : ridgeMapValue(totalX, totalZ)
        );
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
        return OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCF71B60E764BFC2CL, totalX * CONTINENTAL_FREQUENCY, totalZ * CONTINENTAL_FREQUENCY, 0);
    }

    public static double riverMapValue(long totalX, long totalZ) {
        return OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x84D43603ED399321L, totalX * RIVER_FREQUENCY, totalZ * RIVER_FREQUENCY, 0);
    }

    public static double ridgeMapValue(long totalX, long totalZ) {
        return 1 - Math.abs(OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xDD4D88700A5E4D7EL, totalX * RIDGE_FREQUENCY, totalZ * RIDGE_FREQUENCY, 0));
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


    private static final double TEMPERATURE_FREQUENCY = 1 / 16000.0;
    private static final double HUMIDITY_FREQUENCY = TEMPERATURE_FREQUENCY;
    private static final double HEIGHT_MAP_FREQUENCY = 1 / 6400.0;
    private static final double EROSION_FREQUENCY = 1 / 16000.0;
    private static final double CONTINENTAL_FREQUENCY = 1 / 64000.0;
    private static final double RIVER_FREQUENCY = 1 / 32000.0;
    private static final double RIDGE_FREQUENCY = 1 / 16300.0;
}
