package game.server.biomes;

import game.server.generation.GenerationData;

import static game.utils.Constants.SAND;

public final class Beach extends Biome {

    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        return Biome.placeHomogenousSurfaceMaterial(inChunkX, inChunkY, inChunkZ, data, SAND);
    }
}
