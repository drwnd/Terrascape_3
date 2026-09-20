package game.server.biomes;

import core.assets.AssetManager;

import core.assets.identifiers.AssetIdentifier;
import core.utils.MathUtils;

import game.assets.StructureCollection;
import game.server.generation.GenerationData;
import game.server.generation.Structure;
import game.server.generation.WorldGenStructure;
import game.server.generation.WorldGeneration;

import static game.utils.Constants.*;

public interface Biome {

    void placeMaterials(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, GenerationData data);

    default void placeSpecialFeatures(int inChunkX, int inChunkZ, GenerationData data) {
    }

    default int getBiomeDepth(GenerationData data) {
        return 48 + data.biomeDepthMod;
    }

    default int getSpecialHeight(long totalX, long totalZ) {
        return 0;
    }

    default int getStructureChancePromille() {
        return 0;
    }

    default int getStructureFeatureChancePromille() {
        return 0;
    }

    default WorldGenStructure getStructure(long totalX, long height, long totalZ) {
        return null;
    }

    default WorldGenStructure getStructureFeature(long totalX, long height, long totalZ) {
        return null;
    }

    default String getName() {
        return getClass().getSimpleName();
    }


    static WorldGenStructure getRandomStructure(long x, long y, long z, AssetIdentifier<StructureCollection> structures) {
        if (structures == null) return null;
        byte transform = (byte) (MathUtils.hash((int) x >>> CHUNK_SIZE_BITS, (int) z >>> CHUNK_SIZE_BITS, (int) WorldGeneration.SEED ^ 0xEB0A8449) & Structure.ALL_TRANSFORMS);
        Structure structure = AssetManager.get(structures).getRandom((int) x, (int) y, (int) z);
        if (structure == null) return null;
        return new WorldGenStructure(x, y - structure.centerY(), z, structure, transform);
    }
}