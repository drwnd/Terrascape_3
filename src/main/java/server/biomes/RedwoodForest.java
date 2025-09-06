package server.biomes;

import server.generation.GenerationData;

import static utils.Constants.DIRT;

public final class RedwoodForest extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        int totalX = data.getTotalX(inChunkX);
        int totalY = data.getTotalY(inChunkY);
        int totalZ = data.getTotalZ(inChunkZ);

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (data.isInsideSurfaceMaterialLevel(totalY, 8)) data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingGrassType(totalX, totalZ, totalZ));
        else data.store(inChunkX, inChunkY, inChunkZ, DIRT);
        return true;
    }

    @Override
    public int getRequiredTreeZeroBits() {
        return 0b010010010000;
    }
}
