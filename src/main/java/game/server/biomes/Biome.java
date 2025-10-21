package game.server.biomes;

import core.assets.AssetManager;

import game.assets.StructureCollectionIdentifier;
import game.server.generation.GenerationData;
import game.server.generation.Tree;

import static game.utils.Constants.*;

public abstract class Biome {

    public abstract boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data);


    public int getSpecialHeight(int totalX, int totalZ, GenerationData data) {
        return 0;
    }

    public int getRequiredTreeZeroBits() {
        return 0;
    }

    public Tree getGeneratingTree(int totalX, int height, int totalZ) {
        return null;
    }

    public final String getName() {
        return getClass().getName();
    }

    protected static Tree getRandomTree(int x, int y, int z, StructureCollectionIdentifier trees) {
        return new Tree(x, y, z, AssetManager.get(trees).getRandom(x, y, z), (byte) 0); // TODO random~ transform
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