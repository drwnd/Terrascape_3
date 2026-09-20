package game.server.biomes;

import core.assets.identifiers.AssetIdentifier;

import game.assets.StructureCollection;
import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.*;
import static game.utils.Constants.*;

public final class ColdOcean extends NoisySurfaceBiome {

    public ColdOcean(AssetIdentifier<StructureCollection> structures, int structureChance,
                     AssetIdentifier<StructureCollection> structureFeatures, int structureFeatureChance,
                     int biomeDepth, GenerationData.MaterialFunction materialFunction, GenerationData.SpecialHeightFunction specialHeightFunction) {
        super("Cold Ocean", structures, structureChance, structureFeatures, structureFeatureChance, biomeDepth, materialFunction);
        this.specialHeightFunction = specialHeightFunction;
    }

    @Override
    public void placeMaterials(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, GenerationData data) {
        int sandHeight = (int) (data.feature * 64.0) + WATER_LEVEL - 80;
        if (data.height > sandHeight) data.storeColumn(inChunkX, inChunkZ, inChunkStartY, inChunkEndY, SAND);
        else data.storeColumn(inChunkX, inChunkZ, inChunkStartY, inChunkEndY, materialFunction);
    }

    @Override
    public void placeSpecialFeatures(int inChunkX, int inChunkZ, GenerationData data) {
        int iceHeight = Math.min(data.specialHeight, WATER_LEVEL - data.height);
        if (iceHeight == 0) return;
        int start = data.clampStartHeightToInChunkY(WATER_LEVEL - iceHeight);
        int end = data.clampStartHeightToInChunkY(WATER_LEVEL + (iceHeight >> 3));
        data.storeColumn(inChunkX, inChunkZ, start, end, iceHeight == 1 ? ICE : HEAVY_ICE);
    }

    @Override
    public int getSpecialHeight(long totalX, long totalZ) {
        return specialHeightFunction.getSpecialHeight(totalX, totalZ);
    }

    private final GenerationData.SpecialHeightFunction specialHeightFunction;
}
