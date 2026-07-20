package game.server.biomes;

import game.assets.StructureCollectionIdentifier;
import game.server.generation.GenerationData;
import game.server.generation.Tree;

public final class Wasteland extends Biome {
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
        long totalX = data.totalX;
        long totalY = data.totalY;
        long totalZ = data.totalZ;

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingDirtType(totalX, totalY, totalZ));
        return true;
    }

    @Override
    public Tree getGeneratingTree(long totalX, long height, long totalZ) {
        return getRandomTree(totalX, height, totalZ, StructureCollectionIdentifier.BLACK_WOOD_TREES);
    }

    @Override
    public int getRequiredTreeZeroBits() {
        return 0b0111010010010100;
    }
}
