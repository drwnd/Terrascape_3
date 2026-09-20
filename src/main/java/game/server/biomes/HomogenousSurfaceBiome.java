package game.server.biomes;

import core.assets.identifiers.AssetIdentifier;
import game.assets.StructureCollection;
import game.server.generation.GenerationData;
import game.server.generation.WorldGenStructure;

public class HomogenousSurfaceBiome implements Biome {

    public HomogenousSurfaceBiome(String name,
                                  AssetIdentifier<StructureCollection> structures, int structureChance,
                                  AssetIdentifier<StructureCollection> structureFeatures, int structureFeatureChance,
                                  int biomeDepth, byte surfaceMaterial) {
        this.name = name;
        this.structures = structures;
        this.structureChance = structureChance;
        this.structureFeatures = structureFeatures;
        this.structureFeatureChance = structureFeatureChance;
        this.biomeDepth = biomeDepth;
        this.surfaceMaterial = surfaceMaterial;
    }

    @Override
    public void placeMaterials(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, GenerationData data) {
        data.storeColumn(inChunkX, inChunkZ, inChunkStartY, inChunkEndY, surfaceMaterial);
    }

    @Override
    public int getBiomeDepth(GenerationData data) {
        return biomeDepth + data.biomeDepthMod;
    }

    @Override
    public int getStructureChancePromille() {
        return structureChance;
    }

    @Override
    public WorldGenStructure getStructure(long totalX, long height, long totalZ) {
        if (structures == null) return null;
        return Biome.getRandomStructure(totalX, height, totalZ, structures);
    }

    @Override
    public int getStructureFeatureChancePromille() {
        return structureFeatureChance;
    }

    @Override
    public WorldGenStructure getStructureFeature(long totalX, long height, long totalZ) {
        return Biome.getRandomStructure(totalX, height, totalZ, structureFeatures);
    }

    @Override
    public String getName() {
        return name;
    }

    protected final String name;
    protected final AssetIdentifier<StructureCollection> structures, structureFeatures;
    protected final int structureChance, structureFeatureChance, biomeDepth;
    protected final byte surfaceMaterial;
}
