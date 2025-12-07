package game.server.biomes;

import game.server.generation.GenerationData;
import core.utils.OpenSimplex2S;
import game.utils.Utils;

import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.SAND;

public final class ColdOcean extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.totalX;
        int totalY = data.totalY;
        int totalZ = data.totalZ;

        int iceHeight = Math.min(data.specialHeight, WATER_LEVEL - data.height);
        if (totalY > WATER_LEVEL - iceHeight && totalY <= WATER_LEVEL + (iceHeight >> 1)) {
            data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingIceType(totalX, totalY, totalZ));
            return true;
        }
        if (data.isAboveSurface(totalY)) return false;

        int sandHeight = (int) (data.feature * 64.0) + WATER_LEVEL - 80;
        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (totalY > sandHeight) data.store(inChunkX, inChunkY, inChunkZ, SAND);
        else data.store(inChunkX, inChunkY, inChunkZ, data.getColdOceanFloorMaterial(totalX, totalY, totalZ));
        return true;
    }

    @Override
    public int getSpecialHeight(int totalX, int totalZ) {
        double iceBergNoise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0xF90C1662F77EE4DFL, totalX * ICE_BERG_FREQUENCY, totalZ * ICE_BERG_FREQUENCY, 0.0);
        double icePlainNoise = OpenSimplex2S.noise3_ImproveXY(SEED ^ 0x649C844EA835C9A7L, totalX * ICE_BERG_FREQUENCY, totalZ * ICE_BERG_FREQUENCY, 0.0);
        if (iceBergNoise > ICE_BERG_THRESHOLD + 0.1) return (int) (ICE_BERG_HEIGHT + (icePlainNoise * 4.0));
        if (iceBergNoise > ICE_BERG_THRESHOLD)
            return (int) (Utils.smoothInOutQuad(iceBergNoise, ICE_BERG_THRESHOLD, ICE_BERG_THRESHOLD + 0.1) * ICE_BERG_HEIGHT + (icePlainNoise * 4.0));
        if (icePlainNoise > ICE_PLANE_THRESHOLD) return 1;
        return 0;
    }

    private static final double ICE_BERG_FREQUENCY = 0.0015625;
    private static final double ICE_BERG_THRESHOLD = 0.45;
    private static final double ICE_BERG_HEIGHT = 128;
    private static final double ICE_PLANE_THRESHOLD = 0.3;
}
