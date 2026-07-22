package game.server.biomes;

import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.WATER_LEVEL;
import static game.utils.Constants.SAND;

public final class WarmOcean extends Biome {
    /**
     * Determines and stores the material at the specified in-chunk coordinates for the warm ocean biome.
     * @param inChunkX the x-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param inChunkY the y-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param inChunkZ the z-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param data the generation data for the current position
     * @return true if a material was placed, false otherwise
     */
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        long totalX = data.totalX;
        long totalY = data.totalY;
        long totalZ = data.totalZ;

        if (data.isAboveSurface(totalY)) return false;

        int sandHeight = (int) (data.feature * 64.0) + WATER_LEVEL - 80;
        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (totalY > sandHeight) data.store(inChunkX, inChunkY, inChunkZ, SAND);
        else data.store(inChunkX, inChunkY, inChunkZ, data.getWarmOceanFloorMaterial(totalX, totalY, totalZ));
        return true;
    }
}
