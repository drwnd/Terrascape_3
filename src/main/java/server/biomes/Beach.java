package server.biomes;

import server.generation.GenerationData;

import static utils.Constants.SAND;

public final class Beach extends Biome {

    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        return Biome.placeHomogenousSurfaceMaterial(inChunkX, inChunkY, inChunkZ, data, SAND);
    }
}
