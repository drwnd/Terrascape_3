package server.biomes;

import server.generation.GenerationData;

import static utils.Constants.SNOW;

public final class SnowyPlains extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        return Biome.placeHomogenousSurfaceMaterial(inChunkX, inChunkY, inChunkZ, data, SNOW);
    }

    @Override
    public int getRequiredTreeZeroBits() {
        return 0b01001010010010000;
    }
}
