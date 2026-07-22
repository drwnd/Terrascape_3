package game.server.biomes;

import game.server.generation.GenerationData;
import core.utils.OpenSimplex2S;

import static game.utils.Constants.*;
import static game.server.generation.WorldGeneration.SEED;

public final class CorrodedMesa extends Biome {
    /**
     * Places materials for the corroded mesa biome, including tall pillars of terracotta.
     * @param inChunkX the x-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param inChunkY the y-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param inChunkZ the z-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param data the generation data
     * @return true if a material was placed, false otherwise
     */
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        long totalY = data.totalY;

        int pillarHeight = data.specialHeight;
        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (pillarHeight != 0 && totalY >= data.height - floorMaterialDepth) {
            if (totalY > data.height + pillarHeight) return false;
            data.store(inChunkX, inChunkY, inChunkZ, getGeneratingTerracottaType((int) (totalY >> 4 & 15)));
            return true;
        }

        if (data.isAboveSurface(totalY)) return false;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth + 80)) return false;   // Stone placed by caller
        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) data.store(inChunkX, inChunkY, inChunkZ, RED_SANDSTONE);
        else data.store(inChunkX, inChunkY, inChunkZ, RED_SAND);
        return true;
    }

    /**
     * Calculates the height of mesa pillars at the specified horizontal coordinates.
     * @param totalX the x-coordinate (Absolute World Coordinates)
     * @param totalZ the z-coordinate (Absolute World Coordinates)
     * @return the pillar height in blocks
     */
    @Override
    public int getSpecialHeight(long totalX, long totalZ) {
        double noise = OpenSimplex2S.noise2(SEED ^ 0xDF860F2E2A604A17L, totalX * MESA_PILLAR_FREQUENCY, totalZ * MESA_PILLAR_FREQUENCY);
        noise += OpenSimplex2S.noise2(SEED ^ 0x3B632CA2452D2CCDL, totalX * MESA_PILLAR_FREQUENCY * 10, totalZ * MESA_PILLAR_FREQUENCY * 10) * 0.075;
        if (Math.abs(noise) > MESA_PILLAR_THRESHOLD) return MESA_PILLAR_HEIGHT;
        return 0;
    }

    @Override
    public int getFloorMaterialDepth(GenerationData data) {
        return 128 + data.floorMaterialDepthMod;
    }

    /**
     * Determines the terracotta type based on the vertical position index.
     * @param terracottaIndex the vertical index for terracotta banding
     * @return the material ID for the terracotta type
     */
    private static byte getGeneratingTerracottaType(int terracottaIndex) {
        return switch (terracottaIndex) {
            case 3, 6, 10, 11, 15 -> RED_TERRACOTTA;
            case 2, 8, 12 -> YELLOW_TERRACOTTA;
            default -> TERRACOTTA;
        };
    }

    private static final double MESA_PILLAR_THRESHOLD = 0.55;
    private static final double MESA_PILLAR_FREQUENCY = 1 / 516.0;
    private static final int MESA_PILLAR_HEIGHT = 400;
}
