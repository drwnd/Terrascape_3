package game.player.interaction;

import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public class CuboidPlaceable implements Placeable {

    public CuboidPlaceable(byte material, Vector3i position1, Vector3i position2) {
        this.material = material;
        this.minPosition = Utils.min(position1, position2);
        this.maxPosition = Utils.max(position1, position2);
    }

    public static void offsetPositions(Vector3i minPosition, Vector3i maxPosition) {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getPlaceBreakSize();
        int mask = -(1 << breakPlaceSize);

        minPosition.x &= mask;
        minPosition.y &= mask;
        minPosition.z &= mask;

        maxPosition.x = (maxPosition.x & mask) + (1 << breakPlaceSize) - 1;
        maxPosition.y = (maxPosition.y & mask) + (1 << breakPlaceSize) - 1;
        maxPosition.z = (maxPosition.z & mask) + (1 << breakPlaceSize) - 1;
    }

    @Override
    public void place(Vector3i position, int lod) {
        int chunkStartX = minPosition.x >>> CHUNK_SIZE_BITS + lod;
        int chunkStartY = minPosition.y >>> CHUNK_SIZE_BITS + lod;
        int chunkStartZ = minPosition.z >>> CHUNK_SIZE_BITS + lod;
        int chunkEndX = Utils.getWrappedChunkCoordinate(maxPosition.x >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        int chunkEndY = Utils.getWrappedChunkCoordinate(maxPosition.y >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        int chunkEndZ = Utils.getWrappedChunkCoordinate(maxPosition.z >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (int chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (int chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (int chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++)
                    placeInChunk(saver.loadAndGenerate(chunkX, chunkY, chunkZ, lod));
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public Structure getStructure() {
        return new Structure(material);
    }

    @Override
    public boolean intersectsAABB(Vector3i position, Vector3i min, Vector3i max) {
        if (Properties.hasProperties(material, NO_COLLISION)) return false;

        return min.x < maxPosition.x && minPosition.x <= max.x
                && min.y < maxPosition.y && minPosition.y <= max.y
                && min.z < maxPosition.z && minPosition.z <= max.z;
    }

    @Override
    public void offsetPosition(Vector3i position) {
        offsetPositions(minPosition, maxPosition);
    }

    public Vector3i getMinPosition() {
        return minPosition;
    }

    public Vector3i getMaxPosition() {
        return maxPosition;
    }

    public byte getMaterial() {
        return material;
    }

    private void placeInChunk(Chunk chunk) {
        int chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        int chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        int chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int inChunkStartX = Math.max(chunkStartX, minPosition.x) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkStartY = Math.max(chunkStartY, minPosition.y) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkStartZ = Math.max(chunkStartZ, minPosition.z) >> chunk.LOD & CHUNK_SIZE_MASK;

        int inChunkEndX = Math.min(chunkStartX + (CHUNK_SIZE << chunk.LOD) - 1, maxPosition.x) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkEndY = Math.min(chunkStartY + (CHUNK_SIZE << chunk.LOD) - 1, maxPosition.y) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkEndZ = Math.min(chunkStartZ + (CHUNK_SIZE << chunk.LOD) - 1, maxPosition.z) >> chunk.LOD & CHUNK_SIZE_MASK;

        chunk.storeMaterial(
                inChunkStartX, inChunkStartY, inChunkStartZ,
                material,
                inChunkEndX - inChunkStartX + 1,
                inChunkEndY - inChunkStartY + 1,
                inChunkEndZ - inChunkStartZ + 1
        );

        affectedChunks.add(chunk);
        World world = Game.getWorld();
        if (inChunkStartX == 0) affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkStartY == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, chunk.LOD));
        if (inChunkStartZ == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, chunk.LOD));
        if (inChunkEndX == CHUNK_SIZE - 1) affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkEndY == CHUNK_SIZE - 1) affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, chunk.LOD));
        if (inChunkEndZ == CHUNK_SIZE - 1) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, chunk.LOD));
    }

    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private final Vector3i minPosition, maxPosition;
    private final byte material;
}
