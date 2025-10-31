package game.server.biomes;

import game.server.generation.GenerationData;
import game.utils.Utils;

import static game.server.generation.WorldGeneration.WATER_LEVEL;

public final class DryMountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.totalX;
        int totalY = data.totalY;
        int totalZ = data.totalZ;

        if (data.isAboveSurface(totalY)) return false;

        int dirtHeight = Utils.floor(data.feature * 512 + WATER_LEVEL);
        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth) || data.height > dirtHeight) return false;
        else data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingDirtType(totalX, totalY, totalZ));
        return true;
    }
}
