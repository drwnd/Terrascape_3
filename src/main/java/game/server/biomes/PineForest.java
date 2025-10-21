package game.server.biomes;

import game.assets.StructureCollectionIdentifier;
import game.server.generation.GenerationData;
import game.server.generation.Tree;

import static game.utils.Constants.GRASS;

public final class PineForest extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        return Biome.placeLayeredSurfaceMaterial(inChunkX, inChunkY, inChunkZ, data, GRASS);
    }

    @Override
    public Tree getGeneratingTree(int totalX, int height, int totalZ) {
        return getRandomTree(totalX, height, totalZ, StructureCollectionIdentifier.PINE_TREES);
    }

    @Override
    public int getRequiredTreeZeroBits() {
        return 0b010010010000;
    }
}
