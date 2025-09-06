package server.biomes;

import server.generation.GenerationData;
import utils.Utils;

import static server.generation.WorldGeneration.WATER_LEVEL;
import static utils.Constants.SNOW;

public final class SnowyMountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.getTotalX(inChunkX);
        int totalY = data.getTotalY(inChunkY);
        int totalZ = data.getTotalZ(inChunkZ);

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
