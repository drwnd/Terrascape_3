package game.server.biomes;

import core.assets.AssetManager;

import core.utils.MathUtils;

import game.assets.StructureCollectionIdentifier;
import game.server.generation.GenerationData;
import game.server.generation.Structure;
import game.server.generation.Tree;
import game.server.generation.WorldGeneration;

import static game.utils.Constants.*;

public abstract class Biome {

    public abstract boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data);

    public int getFloorMaterialDepth(GenerationData data) {
        return 48 + data.floorMaterialDepthMod;
    }

    public int getSpecialHeight(long totalX, long totalZ) {
        return 0;
    }

    public int getRequiredTreeZeroBits() {
        return 0;
    }

    public Tree getGeneratingTree(long totalX, long height, long totalZ) {
        return null;
    }

    public final String getName() {
        return getClass().getSimpleName();
    }

/**
 * Returns the random tree.
 *
 * @param x X coordinate in local block coordinates
 * @param y Y coordinate in local block coordinates
 * @param z Z coordinate in local block coordinates
 * @param trees parameter
 * @return result
 */
    protected static Tree getRandomTree(long x, long y, long z, StructureCollectionIdentifier trees) {
        byte transform = (byte) (MathUtils.hash((int) x >>> CHUNK_SIZE_BITS, (int) z >>> CHUNK_SIZE_BITS, (int) WorldGeneration.SEED ^ 0xEB0A8449) & Structure.ALL_TRANSFORMS);
        return new Tree(x, y, z, AssetManager.get(trees).getRandom((int) x, (int) y, (int) z), transform);
    }

/**
 * Performs place homogenous surface material.
 *
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param data parameter
 * @param material parameter
 * @return true if the condition holds
 */
    protected static boolean placeHomogenousSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte material) {
        long totalY = data.totalY;

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        data.store(inChunkX, inChunkY, inChunkZ, material);
        return true;
    }

/**
 * Performs place layered surface material.
 *
 * @param inChunkX X coordinate in local block coordinates
 * @param inChunkY Y coordinate in local block coordinates
 * @param inChunkZ Z coordinate in local block coordinates
 * @param data parameter
 * @param topMaterial parameter
 * @return true if the condition holds
 */
    protected static boolean placeLayeredSurfaceMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data, byte topMaterial) {
        long totalY = data.totalY;

        if (data.isAboveSurface(totalY)) return false;

        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth)) return false;   // Stone placed by caller
        if (data.isInsideSurfaceMaterialLevel(totalY, 8)) data.store(inChunkX, inChunkY, inChunkZ, topMaterial);
        else data.store(inChunkX, inChunkY, inChunkZ, DIRT);
        return true;
    }
}
