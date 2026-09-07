package game.server.biomes;

import core.utils.MathUtils;
import core.utils.OpenSimplex2S;

import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.*;

public final class ColdOcean implements Biome {

    @Override
    public void placeMaterials(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, GenerationData data) {
        int sandHeight = (int) (data.feature * 64.0) + WATER_LEVEL - 80;
        if (data.height > sandHeight) data.storeColumn(inChunkX, inChunkZ, inChunkStartY, inChunkEndY, SAND);
        else data.storeColumn(inChunkX, inChunkZ, inChunkStartY, inChunkEndY, GenerationData::getColdOceanFloorMaterial);
    }

    @Override
    public void placeSpecialFeatures(int inChunkX, int inChunkZ, GenerationData data) {
        int iceHeight = Math.min(data.specialHeight, WATER_LEVEL - data.height);
        if (iceHeight == 0) return;
        int start = data.clampStartHeightToInChunkY(WATER_LEVEL - iceHeight);
        int end = data.clampStartHeightToInChunkY(WATER_LEVEL + (iceHeight >> 3));
        data.storeColumn(inChunkX, inChunkZ, start, end, iceHeight == 1 ? ICE : HEAVY_ICE);
    }

    @Override
    public int getSpecialHeight(long totalX, long totalZ) {
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

    private static final double ICE_BERG_FREQUENCY = 1 / 640.0;
    private static final double ICE_BERG_THRESHOLD = 0.45;
    private static final double ICE_BERG_HEIGHT = 128;
    private static final double ICE_PLANE_THRESHOLD = 0.3;
}
