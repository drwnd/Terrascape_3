package game.server.biomes;

import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.WATER_LEVEL;
import static game.utils.Constants.SAND;

public final class Ocean extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.totalX;
        int totalY = data.totalY;
        int totalZ = data.totalZ;

        if (data.isAboveSurface(totalY)) return false;

        int sandHeight = (int) (data.feature * 64.0) + WATER_LEVEL - 80;
        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (totalY > sandHeight) data.store(inChunkX, inChunkY, inChunkZ, SAND);
        else data.store(inChunkX, inChunkY, inChunkZ, data.getOceanFloorMaterial(totalX, totalY, totalZ));
        return true;
    }
}
