package game.server.generation;

import core.language.Translatable;
import core.settings.optionSettings.Option;
import game.server.biomes.Biome;
import game.server.biomes.BiomesCache;

public enum BiomeSamplers implements Option, Translatable {

    DEFAULT(BiomesCache::getAllBiomes),
    COLD_BIOMES(BiomesCache::getColdBiomes),
    WARM_BIOMES(BiomesCache::getWarmBiomes),
    MODERATE_BIOMES(BiomesCache::getModerateBiomes);

    public static BiomeSamplers getSaved(int savedOrdinal) {
        BiomeSamplers[] values = BiomeSamplers.values();
        if (savedOrdinal >= values.length) return DEFAULT;
        return values[savedOrdinal];
    }

    BiomeSamplers(BiomeSampler biomeSampler) {
        this.biomeSampler = biomeSampler;
    }

    @Override
    public String translationFileName() {
        return "BiomeSamplers";
    }

    public final BiomeSampler biomeSampler;

    public interface BiomeSampler {
        Biome getBiome(MapSample sample, int height, double feature);
    }
}
