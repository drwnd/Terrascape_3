package server.biomes;

import server.generation.GenerationData;
import utils.Utils;

import static server.generation.WorldGeneration.WATER_LEVEL;

public final class DryMountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.getTotalX(inChunkX);
        int totalY = data.getTotalY(inChunkY);
        int totalZ = data.getTotalZ(inChunkZ);

        if (data.isAboveSurface(totalY)) return false;

        int dirtHeight = Utils.floor(data.feature * 512 + WATER_LEVEL);
        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth) || data.height > dirtHeight) return false;
        else data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingDirtType(totalX, totalY, totalZ));
        return true;
    }
}
