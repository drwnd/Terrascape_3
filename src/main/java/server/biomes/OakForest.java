package server.biomes;

import server.generation.GenerationData;

import static utils.Constants.GRASS;

public final class OakForest extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        return Biome.placeLayeredSurfaceMaterial(inChunkX, inChunkY, inChunkZ, data, GRASS);
    }

    @Override
    public int getRequiredTreeZeroBits() {
        return 0b010010010000;
    }
}
