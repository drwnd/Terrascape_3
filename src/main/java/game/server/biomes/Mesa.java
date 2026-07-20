package game.server.biomes;

import game.server.generation.GenerationData;

import static game.utils.Constants.RED_SAND;
import static game.utils.Constants.RED_SANDSTONE;

public final class Mesa extends Biome {
/**
 * Performs place material.
 *
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param data parameter
 * @return true if the condition holds
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

    @Override
    public int getFloorMaterialDepth(GenerationData data) {
        return 128 + data.floorMaterialDepthMod;
    }
}
