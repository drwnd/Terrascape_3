package game.server.biomes;

import game.server.generation.GenerationData;

import static game.utils.Constants.DIRT;

public abstract class Biome {

    public abstract boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data);


    public int getSpecialHeight(int totalX, int totalZ, GenerationData data) {
        return 0;
    }

    public int getRequiredTreeZeroBits() {
        return 0;
    }

    public final String getName() {
        return getClass().getName();
    }

    protected static boolean placeHomogenousSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte material) {
        int totalY = data.getTotalY(inChunkY);

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        data.store(inChunkX, inChunkY, inChunkZ, material);
        return true;
    }

    protected static boolean placeLayeredSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte topMaterial) {
        int totalY = data.getTotalY(inChunkY);

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.getFloorMaterialDepthMod();

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (data.isInsideSurfaceMaterialLevel(totalY, 8)) data.store(inChunkX, inChunkY, inChunkZ, topMaterial);
        else data.store(inChunkX, inChunkY, inChunkZ, DIRT);
        return true;
    }
}