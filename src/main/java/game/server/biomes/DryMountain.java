package game.server.biomes;

import core.utils.MathUtils;

import game.server.generation.GenerationData;

import static game.server.generation.WorldGeneration.WATER_LEVEL;

public final class DryMountain extends Biome {
    @Override
    public boolean placeMaterial(int inChunkX, int inChunkY, int inChunkZ, GenerationData data) {
        long totalX = data.totalX;
        long totalY = data.totalY;
        long totalZ = data.totalZ;

        if (data.isAboveSurface(totalY)) return false;

        int dirtHeight = MathUtils.floor(data.feature * 512 + WATER_LEVEL);
        int floorMaterialDepth = 48 + data.floorMaterialDepthMod;

        if (data.isBelowFloorMaterialLevel(totalY, floorMaterialDepth) || data.height > dirtHeight) return false;
        else data.store(inChunkX, inChunkY, inChunkZ, data.getGeneratingDirtType(totalX, totalY, totalZ));
        return true;
    }
}
