package game.server.biomes;

import game.server.generation.GenerationData;

import static game.utils.Constants.SAND;
import static game.utils.Constants.SANDSTONE;

public final class Desert extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalY = data.totalY;

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth + 80)) return false;   // Stone placed by caller
        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) data.store(inChunkX, inChunkY, inChunkZ, SANDSTONE);
        else data.store(inChunkX, inChunkY, inChunkZ, SAND);
        return true;
    }

    @Override
    public int getFloorMaterialDepth(GenerationData data) {
        return 128 + data.floorMaterialDepthMod;
    }
}
