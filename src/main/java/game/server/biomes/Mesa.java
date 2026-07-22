package game.server.biomes;

import game.server.generation.GenerationData;

import static game.utils.Constants.RED_SAND;
import static game.utils.Constants.RED_SANDSTONE;

public final class Mesa extends Biome {
    /**
     * Places materials specific to the mesa biome, using red sand and red sandstone.
     * @param inChunkX the x-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param inChunkY the y-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param inChunkZ the z-coordinate within the chunk (In-Chunk Block Coordinates)
     * @param data the generation data
     * @return true if a material was placed, false otherwise
     */
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        long totalY = data.totalY;

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth + 80)) return false;   // Stone placed by caller
        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) data.store(inChunkX, inChunkY, inChunkZ, RED_SANDSTONE);
        else data.store(inChunkX, inChunkY, inChunkZ, RED_SAND);
        return true;
    }

    /**
     * Returns a deeper floor material depth for the mesa biome.
     * @param data the generation data
     * @return the depth of the floor material in blocks
     */
    @Override
    public int getFloorMaterialDepth(GenerationData data) {
        return 128 + data.floorMaterialDepthMod;
    }
}
