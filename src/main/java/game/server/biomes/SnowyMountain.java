package game.server.biomes;

import core.utils.MathUtils;

import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.WATER_LEVEL;
import static game.utils.Constants.SNOW;

public final class SnowyMountain extends Biome {
    /**
     * Determines and stores the material at the specified in-chunk coordinates for the snowy mountain biome.
     * Places snow or ice depending on the height.
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

        int iceHeight = MathUtils.floor(data.feature * 512 + ICE_LEVEL);
        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (totalY > iceHeight) data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingIceType(totalX, totalY, totalZ));
        else data.store(inChunkX, inChunkY, inChunkZ, SNOW);
        return true;
    }

    private static final int ICE_LEVEL = WATER_LEVEL + 2256;
}
