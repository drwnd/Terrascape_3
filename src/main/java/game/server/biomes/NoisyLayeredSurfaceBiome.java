package game.server.biomes;

import core.assets.identifiers.AssetIdentifier;
import game.assets.StructureCollection;
import game.server.generation.GenerationData;
import game.server.generation.WorldGenStructure;

public class NoisyLayeredSurfaceBiome implements Biome {

    public NoisyLayeredSurfaceBiome(String name,
                                    AssetIdentifier<StructureCollection> structures, int structureChance,
                                    AssetIdentifier<StructureCollection> structureFeatures, int structureFeatureChance,
                                    int surfaceMaterialDepth, int biomeDepth, byte bottomMaterial,  GenerationData.MaterialFunction materialFunction) {
        this.name = name;
        this.structures = structures;
        this.structureChance = structureChance;
        this.structureFeatures = structureFeatures;
        this.structureFeatureChance = structureFeatureChance;
        this.biomeDepth = biomeDepth;
        this.surfaceMaterialDepth = surfaceMaterialDepth;
        this.bottomMaterial = bottomMaterial;
        this.materialFunction = materialFunction;
    }

    @Override
    public void placeMaterials(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, GenerationData data) {
        int surfaceMaterialStart = data.clampStartHeightToInChunkY(data.height - surfaceMaterialDepth - data.biomeDepthMod);
        data.storeColumn(inChunkX, inChunkZ, inChunkStartY, surfaceMaterialStart, bottomMaterial);
        data.storeColumn(inChunkX, inChunkZ, surfaceMaterialStart, inChunkEndY, materialFunction);
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
    protected final int structureChance, structureFeatureChance, surfaceMaterialDepth, biomeDepth;
    protected final byte bottomMaterial;
    protected final  GenerationData.MaterialFunction materialFunction;
}
