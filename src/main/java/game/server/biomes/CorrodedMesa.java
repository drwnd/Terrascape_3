package game.server.biomes;

import core.assets.identifiers.AssetIdentifier;

import game.assets.StructureCollection;
import game.server.generation.GenerationData;

import static game.utils.Constants.*;

public final class CorrodedMesa extends LayeredSurfaceBiome {

    public CorrodedMesa(
            AssetIdentifier<StructureCollection> structures, int structureChance,
            AssetIdentifier<StructureCollection> structureFeatures, int structureFeatureChance,
            int surfaceMaterialDepth, int biomeDepth, byte topMaterial, byte bottomMaterial,
            GenerationData.SpecialHeightFunction specialHeightFunction) {
        super("Corroded Mesa", structures, structureChance, structureFeatures, structureFeatureChance, surfaceMaterialDepth, biomeDepth, topMaterial, bottomMaterial);
        this.specialHeightFunction = specialHeightFunction;
    }

    @Override
    public void placeSpecialFeatures(int inChunkX, int inChunkZ, GenerationData data) {
        int pillarHeight = data.specialHeight;
        if (pillarHeight == 0) return;
        int start = data.clampStartHeightToInChunkY(data.height - data.biomeDepth);
        int end = data.clampEndHeightToInChunkY(data.height + pillarHeight);
        data.storeColumn(inChunkX, inChunkZ, start, end, CorrodedMesa::getGeneratingTerracottaType);
    }

    @Override
    public int getSpecialHeight(long totalX, long totalZ) {
        return specialHeightFunction.getSpecialHeight(totalX, totalZ);
    }

    private static byte getGeneratingTerracottaType(GenerationData data, long totalX, long totalY, long totalZ) {
        int terracottaIndex = (int) (totalY >> 4 & 15);
        return switch (terracottaIndex) {
            case 3, 6, 10, 11, 15 -> RED_TERRACOTTA;
            case 2, 8, 12 -> YELLOW_TERRACOTTA;
            default -> TERRACOTTA;
        };
    }

    private final GenerationData.SpecialHeightFunction specialHeightFunction;
}
