package game.server.biomes;

import game.assets.StructureCollectionIdentifier;
import game.server.generation.GenerationData;
import game.server.generation.Tree;

import static game.utils.Constants.*;

public final class BlackWoodForest extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        return Biome.placeLayeredSurfaceMaterial(inChunkX, inChunkY, inChunkZ, data, PODZOL);
    }

    @Override
    public Tree getGeneratingTree(int totalX, int height, int totalZ) {
        return getRandomTree(totalX, height, totalZ, StructureCollectionIdentifier.BLACK_WOOD_TREES);
    }

    @Override
    public int getRequiredTreeZeroBits() {
        return 0b010010010000;
    }
}
