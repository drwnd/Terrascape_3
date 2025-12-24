package game.player.interaction;

import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;
import game.utils.Utils;

import org.joml.Math;
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
        int chunkStartX = minPosition.x >> lod + CHUNK_SIZE_BITS;
        int chunkStartY = minPosition.y >> lod + CHUNK_SIZE_BITS;
        int chunkStartZ = minPosition.z >> lod + CHUNK_SIZE_BITS;
        int chunkEndX = maxPosition.x >> lod + CHUNK_SIZE_BITS;
        int chunkEndY = maxPosition.y >> lod + CHUNK_SIZE_BITS;
        int chunkEndZ = maxPosition.z >> lod + CHUNK_SIZE_BITS;
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

        int inChunkStartX = Math.max(chunkStartX, Utils.getWrappedPosition(minPosition.x, chunkStartX, WORLD_SIZE_XZ)) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkStartY = Math.max(chunkStartY, Utils.getWrappedPosition(minPosition.y, chunkStartY, WORLD_SIZE_Y)) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkStartZ = Math.max(chunkStartZ, Utils.getWrappedPosition(minPosition.z, chunkStartZ, WORLD_SIZE_XZ)) >> chunk.LOD & CHUNK_SIZE_MASK;

        int inChunkEndX = Math.min(chunkStartX + CHUNK_SIZE - 1, Utils.getWrappedPosition(maxPosition.x, chunkStartX, WORLD_SIZE_XZ)) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkEndY = Math.min(chunkStartY + CHUNK_SIZE - 1, Utils.getWrappedPosition(maxPosition.y, chunkStartY, WORLD_SIZE_Y)) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkEndZ = Math.min(chunkStartZ + CHUNK_SIZE - 1, Utils.getWrappedPosition(maxPosition.z, chunkStartZ, WORLD_SIZE_XZ)) >> chunk.LOD & CHUNK_SIZE_MASK;

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
