package game.server.biomes;

import game.server.generation.GenerationData;
import game.utils.Utils;

import static game.server.generation.WorldGeneration.WATER_LEVEL;
import static game.utils.Constants.SNOW;

public final class SnowyMountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.totalX;
        int totalY = data.totalY;
        int totalZ = data.totalZ;

        if (data.isAboveSurface(totalY)) return false;

        int iceHeight = Utils.floor(data.feature * 512 + ICE_LEVEL);
        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (totalY > iceHeight) data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingIceType(totalX, totalY, totalZ));
        else data.store(inChunkX, inChunkY, inChunkZ, SNOW);
        return true;
    }

    private static final int ICE_LEVEL = WATER_LEVEL + 2256;
}
