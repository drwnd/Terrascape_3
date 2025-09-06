package server.biomes;

import server.generation.GenerationData;

import static utils.Constants.SAND;
import static utils.Constants.SANDSTONE;

public final class Desert extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalY = data.getTotalY(inChunkY);

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth + 80)) return false;   // Stone placed by caller
        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) data.store(inChunkX, inChunkY, inChunkZ, SANDSTONE);
        else data.store(inChunkX, inChunkY, inChunkZ, SAND);
        return true;
    }
}
