package game.server.biomes;

import core.utils.MathUtils;
import core.utils.OpenSimplex2S;
import game.assets.StructureCollectionIdentifier;
import game.server.generation.GenerationData;
import game.server.generation.MapSample;


import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.*;
import static game.assets.StructureCollectionIdentifier.*;

public final class BiomesCache {

    public static final Biome
            MOUNTAIN = new Mountain(),
            DRY_MOUNTAIN = new DryMountain(),
            SNOWY_MOUNTAIN = new SnowyMountain(),
            COLD_OCEAN = new ColdOcean(
                    null, 0,
                    null, 0,
                    48, GenerationData::getColdOceanFloorMaterial, BiomesCache::getSpecialIceHeight
            ),
            CORRODED_MESA = new CorrodedMesa(
                    null, 0,
                    StructureCollectionIdentifier.merge(new StructureCollectionIdentifier[]{CACTUS, SHRUB}, new float[]{0.5F, 0.5F}), 16,
                    48, 128, RED_SAND, RED_SANDSTONE, BiomesCache::getSpecialMesaPillarHeight
            ),
            BEACH = new HomogenousSurfaceBiome("Beach",
                    null, 0,
                    SHRUB, 64,
                    48, SAND
            ),
            WASTELAND = new NoisySurfaceBiome("Wasteland",
                    BLACK_WOOD_TREES, 8,
                    SHRUB, 16,
                    48, GenerationData::getGeneratingDirtType
            ),
            OCEAN = new NoisySurfaceBiome("Ocean",
                    null, 0,
                    null, 0,
                    48, GenerationData::getOceanFloorMaterial
            ),
            WARM_OCEAN = new NoisySurfaceBiome("Warm Ocean",
                    null, 0,
                    null, 0,
                    48, GenerationData::getWarmOceanFloorMaterial
            ),
            REDWOOD_FOREST = new NoisyLayeredSurfaceBiome("Redwood Forest",
                    REDWOOD_TREES, 128,
                    null, 0,
                    8, 48, DIRT, GenerationData::getGeneratingGrassType
            ),
            PLAINS = new LayeredSurfaceBiome("Plains",
                    OAK_TREES, 32,
                    null, 0,
                    8, 48, GRASS, DIRT
            ),
            SNOWY_PLAINS = new HomogenousSurfaceBiome("Snowy Plains",
                    SPRUCE_TREES, 32,
                    null, 0,
                    48, SNOW
            ),
            BLACK_WOOD_FOREST = new LayeredSurfaceBiome("Black Wood Forest",
                    BLACK_WOOD_TREES, 128,
                    null, 0,
                    8, 48, PODZOL, DIRT
            ),
            DARK_OAK_FOREST = new LayeredSurfaceBiome("Dark Oak Forest",
                    DARK_OAK_TREES, 128,
                    null, 0,
                    8, 48, PODZOL, DIRT
            ),
            OAK_FOREST = new LayeredSurfaceBiome("Oak Forest",
                    OAK_TREES, 128,
                    null, 0,
                    8, 48, GRASS, DIRT
            ),
            PINE_FOREST = new LayeredSurfaceBiome("Pine Forest",
                    PINE_TREES, 128,
                    null, 0,
                    8, 48, GRASS, DIRT
            ),
            SNOWY_SPRUCE_FOREST = new HomogenousSurfaceBiome("Snowy Spruce Forest",
                    SPRUCE_TREES, 128,
                    null, 0,
                    48, SNOW
            ),
            SPRUCE_FOREST = new LayeredSurfaceBiome("Spruce Forest",
                    SPRUCE_TREES, 128,
                    null, 0,
                    8, 48, GRASS, DIRT
            ),
            MESA = new LayeredSurfaceBiome("Mesa",
                    null, 0,
                    StructureCollectionIdentifier.merge(new StructureCollectionIdentifier[]{CACTUS, SHRUB}, new float[]{0.5F, 0.5F}), 64,
                    48, 128, RED_SAND, RED_SANDSTONE
            ),
            DESERT = new LayeredSurfaceBiome("Desert",
                    null, 0,
                    StructureCollectionIdentifier.merge(new StructureCollectionIdentifier[]{CACTUS, SHRUB}, new float[]{0.5F, 0.5F}), 64,
                    48, 128, SAND, SANDSTONE
            );

    public static Biome getAllBiomes(MapSample sample, int height, double feature) {
        double dither = feature * 0.05 - 0.025;

        double temperature = sample.temperature() + dither;
        double humidity = sample.humidity() + dither;
        double continental = sample.continental() - Math.abs(dither);
        double erosion = sample.erosion() + dither;
        int beachHeight = WATER_LEVEL + 64 + (int) (feature * 64 - sample.erosion() * 64);
        int sandHeight = (int) (feature * 64.0) + WATER_LEVEL - 80;

        if (height < WATER_LEVEL) {
            if (sample.temperature() < -0.33) return BiomesCache.COLD_OCEAN;
            if (height > sandHeight) return BiomesCache.BEACH;
            if (temperature > 0.33) return BiomesCache.WARM_OCEAN;
            return BiomesCache.OCEAN;
        }
        if (height < beachHeight) return BiomesCache.BEACH;
        if (continental > MOUNTAIN_THRESHOLD && erosion < 0.51) {
            if (temperature > 0.33) return BiomesCache.DRY_MOUNTAIN;
            else if (temperature < -0.33) return BiomesCache.SNOWY_MOUNTAIN;
            return BiomesCache.MOUNTAIN;
        }

        if (temperature > 0.33) return getWarmBiome(sample, height, temperature, humidity);
        if (humidity > 0.33) {
            if (temperature > -0.1) return BiomesCache.REDWOOD_FOREST;
            if (temperature > -0.4) return BiomesCache.SPRUCE_FOREST;
            return BiomesCache.SNOWY_SPRUCE_FOREST;
        }
        if (humidity < 0.0 && temperature > -0.25) return BiomesCache.PLAINS;
        if (humidity > -0.33 && temperature > -0.33) return BiomesCache.OAK_FOREST;
        if (humidity < -0.33 && temperature > -0.5) return BiomesCache.PINE_FOREST;
        return BiomesCache.SNOWY_PLAINS;
    }

    public static Biome getColdBiomes(MapSample sample, int height, double feature) {
        double dither = feature * 0.05 - 0.025;

        double humidity = sample.humidity() + dither;
        double continental = sample.continental() - Math.abs(dither);
        double erosion = sample.erosion() + dither;
        int beachHeight = WATER_LEVEL + 64 + (int) (feature * 64 - sample.erosion() * 64);

        if (height < WATER_LEVEL) return COLD_OCEAN;
        if (height < beachHeight) return BiomesCache.BEACH;
        if (continental > MOUNTAIN_THRESHOLD && erosion < 0.51) return SNOWY_MOUNTAIN;
        if (humidity > 0.33) return SNOWY_SPRUCE_FOREST;
        return SNOWY_PLAINS;
    }

    public static Biome getWarmBiomes(MapSample sample, int height, double feature) {
        double dither = feature * 0.05 - 0.025;

        double temperature = sample.temperature() + dither;
        double humidity = sample.humidity() + dither;
        double continental = sample.continental() - Math.abs(dither);
        int beachHeight = WATER_LEVEL + 64 + (int) (feature * 64 - sample.erosion() * 64);
        int sandHeight = (int) (feature * 64.0) + WATER_LEVEL - 80;

        if (height < WATER_LEVEL && height <= sandHeight) return WARM_OCEAN;
        if (height < MathUtils.max(WATER_LEVEL, sandHeight, beachHeight)) return BiomesCache.BEACH;
        if (continental > MOUNTAIN_THRESHOLD) return DRY_MOUNTAIN;

        return getWarmBiome(sample, height, temperature, humidity);
    }

    public static Biome getModerateBiomes(MapSample sample, int height, double feature) {
        double dither = feature * 0.05 - 0.025;

        double temperature = sample.temperature() + dither;
        double humidity = sample.humidity() + dither;
        double continental = sample.continental() - Math.abs(dither);
        int beachHeight = WATER_LEVEL + 64 + (int) (feature * 64 - sample.erosion() * 64);
        int sandHeight = (int) (feature * 64.0) + WATER_LEVEL - 80;

        if (height < WATER_LEVEL && height <= sandHeight) return OCEAN;
        if (height < MathUtils.max(WATER_LEVEL, sandHeight, beachHeight)) return BiomesCache.BEACH;
        if (continental > MOUNTAIN_THRESHOLD) return MOUNTAIN;

        if (humidity > 0.33) {
            if (temperature > -0.1) return BiomesCache.REDWOOD_FOREST;
            return BiomesCache.SPRUCE_FOREST;
        }
        if (humidity < 0.0 && temperature > -0.25) return BiomesCache.PLAINS;
        if (humidity > -0.33 && temperature > -0.33) return BiomesCache.OAK_FOREST;
        return BiomesCache.PINE_FOREST;
    }

    private static Biome getWarmBiome(MapSample sample, int height, double temperature, double humidity) {
        if (height > 128 && sample.continental() < MOUNTAIN_THRESHOLD
                && sample.temperature() > 0.45 && sample.humidity() < -0.3) return BiomesCache.CORRODED_MESA;
        if (temperature > 0.55 && humidity < 0.15) return BiomesCache.MESA;
        if (humidity < 0.15) return BiomesCache.DESERT;
        if (humidity > 0.5 && temperature > 0.5) return BiomesCache.BLACK_WOOD_FOREST;
        if (humidity > 0.4 && temperature > 0.4) return BiomesCache.DARK_OAK_FOREST;
        return BiomesCache.WASTELAND;
    }


    private static int getSpecialIceHeight(long totalX, long totalZ) {
        double iceBergNoise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF90C1662F77EE4DFL, totalX * ICE_BERG_FREQUENCY, totalZ * ICE_BERG_FREQUENCY, 0);
        iceBergNoise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xFAA4418F549636ABL, totalX * ICE_BERG_FREQUENCY * 10, totalZ * ICE_BERG_FREQUENCY * 10, 0) * 0.03;

        double icePlainNoise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x649C844EA835C9A7L, totalX * ICE_BERG_FREQUENCY, totalZ * ICE_BERG_FREQUENCY, 0);
        icePlainNoise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCD9B4E7568B5747CL, totalX * ICE_BERG_FREQUENCY * 40, totalZ * ICE_BERG_FREQUENCY * 40, 0) * 0.05;
        double iceBergTopHeightOffset = Math.abs(icePlainNoise) * 16;

        if (iceBergNoise > ICE_BERG_THRESHOLD + 0.2) return (int) (ICE_BERG_HEIGHT + iceBergTopHeightOffset);
        if (iceBergNoise > ICE_BERG_THRESHOLD) {
            double smoothedNoise = MathUtils.smoothInOutQuad(iceBergNoise, ICE_BERG_THRESHOLD, ICE_BERG_THRESHOLD + 0.2);
            return Math.max(1, (int) (Math.pow(smoothedNoise, 0.1) * (ICE_BERG_HEIGHT + iceBergTopHeightOffset)));
        }
        return icePlainNoise > ICE_PLANE_THRESHOLD ? 1 : 0;
    }

    private static int getSpecialMesaPillarHeight(long totalX, long totalZ) {
        double noise = OpenSimplex2S.noise2(SEED ^ 0xDF860F2E2A604A17L, totalX * MESA_PILLAR_FREQUENCY, totalZ * MESA_PILLAR_FREQUENCY);
        noise += OpenSimplex2S.noise2(SEED ^ 0x3B632CA2452D2CCDL, totalX * MESA_PILLAR_FREQUENCY * 10, totalZ * MESA_PILLAR_FREQUENCY * 10) * 0.075;
        if (Math.abs(noise) > MESA_PILLAR_THRESHOLD) return MESA_PILLAR_HEIGHT;
        return 0;
    }

    private static final double ICE_BERG_FREQUENCY = 1 / 640.0;
    private static final double ICE_BERG_THRESHOLD = 0.45;
    private static final double ICE_BERG_HEIGHT = 128;
    private static final double ICE_PLANE_THRESHOLD = 0.3;

    private static final double MESA_PILLAR_THRESHOLD = 0.55;
    private static final double MESA_PILLAR_FREQUENCY = 1 / 516.0;
    private static final int MESA_PILLAR_HEIGHT = 400;
}
