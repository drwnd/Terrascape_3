package server.biomes;

import server.generation.GenerationData;
import server.generation.OpenSimplex2S;

import static utils.Constants.*;
import static server.generation.WorldGeneration.SEED;

public final class CorrodedMesa extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalY = data.getTotalY(inChunkY);

        int pillarHeight = data.specialHeight;
        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (pillarHeight != 0 && totalY >= data.height - floorMaterialDepth) {
            if (totalY > data.height + pillarHeight) return false;
            data.store(inChunkX, inChunkY, inChunkZ, getGeneratingTerracottaType(totalY >> 4 & 15));
            return true;
        }

        if (data.isAboveSurface(totalY)) return false;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth + 80)) return false;   // Stone placed by caller
        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) data.store(inChunkX, inChunkY, inChunkZ, RED_SANDSTONE);
        else data.store(inChunkX, inChunkY, inChunkZ, RED_SAND);
        return true;
    }

    @Override
    public int getSpecialHeight(int totalX, int totalZ, GenerationData data) {
        double noise = OpenSimplex2S.noise2(SEED ^ 0xDF860F2E2A604A17L, totalX * MESA_PILLAR_FREQUENCY, totalZ * MESA_PILLAR_FREQUENCY);
        if (Math.abs(noise) > MESA_PILLAR_THRESHOLD) return MESA_PILLAR_HEIGHT;
        return 0;
    }

    private static byte getGeneratingTerracottaType(int terracottaIndex) {
        return switch (terracottaIndex) {
            case 3, 6, 10, 11, 15 -> RED_TERRACOTTA;
            case 2, 8, 12 -> YELLOW_TERRACOTTA;
            default -> TERRACOTTA;
        };
    }

    private static final double MESA_PILLAR_THRESHOLD = 0.55;
    private static final double MESA_PILLAR_FREQUENCY = 0.001875;
    private static final int MESA_PILLAR_HEIGHT = 400;
}
