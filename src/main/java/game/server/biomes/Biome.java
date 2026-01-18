package game.server.biomes;

import core.assets.AssetManager;

import game.assets.StructureCollectionIdentifier;
import game.server.generation.GenerationData;
import game.server.generation.Structure;
import game.server.generation.Tree;
import game.server.generation.WorldGeneration;
import game.utils.Utils;

import static game.utils.Constants.*;

public abstract class Biome {

    public abstract boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data);

    public int getFloorMaterialDepth(GenerationData data) {
        return 48 + data.floorMaterialDepthMod;
    }

    public int getSpecialHeight(int totalX, int totalZ) {
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
        byte transform = (byte) (Utils.hash(x >>> CHUNK_SIZE_BITS, z >>> CHUNK_SIZE_BITS, (int) WorldGeneration.SEED ^ 0xEB0A8449) & Structure.ALL_TRANSFORMS);
        return new Tree(x, y, z, AssetManager.get(trees).getRandom(x, y, z), transform);
    }

    protected static boolean placeHomogenousSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte material) {
        int totalY = data.totalY;

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        data.store(inChunkX, inChunkY, inChunkZ, material);
        return true;
    }

    protected static boolean placeLayeredSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte topMaterial) {
        int totalY = data.totalY;

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (data.isInsideSurfaceMaterialLevel(totalY, 8)) data.store(inChunkX, inChunkY, inChunkZ, topMaterial);
        else data.store(inChunkX, inChunkY, inChunkZ, DIRT);
        return true;
    }
}