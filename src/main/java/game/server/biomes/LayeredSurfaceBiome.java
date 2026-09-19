package game.server.biomes;

import core.assets.identifiers.AssetIdentifier;
import game.assets.StructureCollection;
import game.server.generation.GenerationData;
import game.server.generation.WorldGenStructure;

public class LayeredSurfaceBiome implements Biome {

    public LayeredSurfaceBiome(String name,
                               AssetIdentifier<StructureCollection> structures, int structureChance,
                               AssetIdentifier<StructureCollection> structureFeatures, int structureFeatureChance,
                               int surfaceMaterialDepth, int biomeDepth, byte topMaterial, byte bottomMaterial) {
        this.name = name;
        this.structures = structures;
        this.structureChance = structureChance;
        this.surfaceMaterialDepth = surfaceMaterialDepth;
        this.structureFeatures = structureFeatures;
        this.structureFeatureChance = structureFeatureChance;
        this.biomeDepth = biomeDepth;
        this.topMaterial = topMaterial;
        this.bottomMaterial = bottomMaterial;
    }

    @Override
    public void placeMaterials(int inChunkX, int inChunkZ, int inChunkStartY, int inChunkEndY, GenerationData data) {
        int surfaceMaterialStart = data.clampStartHeightToInChunkY(data.height - surfaceMaterialDepth - data.biomeDepthMod);
        data.storeColumn(inChunkX, inChunkZ, inChunkStartY, surfaceMaterialStart, bottomMaterial);
        data.storeColumn(inChunkX, inChunkZ, surfaceMaterialStart, inChunkEndY, topMaterial);
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
    protected final int structureChance, structureFeatureChance, surfaceMaterialDepth, biomeDepth;
    protected final byte topMaterial, bottomMaterial;
}
