package game.server.biomes;

import core.utils.MathUtils;
import core.utils.OpenSimplex2S;

import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.SAND;

public final class ColdOcean extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        long totalX = data.totalX;
        long totalY = data.totalY;
        long totalZ = data.totalZ;

        int iceHeight = Math.min(data.specialHeight, WATER_LEVEL - data.height);
        if (totalY > WATER_LEVEL - iceHeight && totalY <= WATER_LEVEL + (iceHeight >> 1)) {
            data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingIceType(totalX, totalY, totalZ));
            return true;
        }
        if (data.isAboveSurface(totalY)) return false;

        int sandHeight = (int) (data.feature * 64.0) + WATER_LEVEL - 80;
        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (totalY > sandHeight) data.store(inChunkX, inChunkY, inChunkZ, SAND);
        else data.store(inChunkX, inChunkY, inChunkZ, data.getColdOceanFloorMaterial(totalX, totalY, totalZ));
        return true;
    }

    @Override
    public int getSpecialHeight(long totalX, long totalZ) {
        double iceBergNoise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF90C1662F77EE4DFL, totalX * ICE_BERG_FREQUENCY, totalZ * ICE_BERG_FREQUENCY, 0);
        iceBergNoise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xFAA4418F549636ABL, totalX * ICE_BERG_FREQUENCY * 10, totalZ * ICE_BERG_FREQUENCY * 10, 0) * 0.03;
        double icePlainNoise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x649C844EA835C9A7L, totalX * ICE_BERG_FREQUENCY, totalZ * ICE_BERG_FREQUENCY, 0);
        icePlainNoise += OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xCD9B4E7568B5747CL, totalX * ICE_BERG_FREQUENCY * 40, totalZ * ICE_BERG_FREQUENCY * 40, 0) * 0.05;
        double iceBergTopHeightOffset = Math.abs(icePlainNoise) * 16;
        if (iceBergNoise > ICE_BERG_THRESHOLD + 0.2) return (int) (ICE_BERG_HEIGHT + iceBergTopHeightOffset);
        if (iceBergNoise > ICE_BERG_THRESHOLD)
            return (int) (MathUtils.smoothInOutQuad(iceBergNoise, ICE_BERG_THRESHOLD, ICE_BERG_THRESHOLD + 0.2) * ICE_BERG_HEIGHT + iceBergTopHeightOffset);
        return icePlainNoise > ICE_PLANE_THRESHOLD ? 1 : 0;
    }

    private static final double ICE_BERG_FREQUENCY = 1 / 640.0;
    private static final double ICE_BERG_THRESHOLD = 0.45;
    private static final double ICE_BERG_HEIGHT = 128;
    private static final double ICE_PLANE_THRESHOLD = 0.3;
}
