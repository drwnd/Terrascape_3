package game.player.interaction;

import core.utils.Saver;
import core.utils.Vector3l;

import game.player.Player;
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

public final class CuboidPlaceable implements Placeable {

    public CuboidPlaceable(byte material, Vector3l position1, Vector3l position2) {
        this.material = material;
        this.minPosition = Utils.min(position1, position2);
        this.maxPosition = Utils.max(position1, position2);
    }

    public static void offsetPositions(Vector3l minPosition, Vector3l maxPosition) {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        int breakPlaceAlign = Game.getPlayer().getInteractionHandler().getBreakPlaceAlign();
        int mask = -(1 << breakPlaceAlign);

        minPosition.x &= mask;
        minPosition.y &= mask;
        minPosition.z &= mask;

        maxPosition.x = (maxPosition.x & mask) + (1L << breakPlaceSize) - 1;
        maxPosition.y = (maxPosition.y & mask) + (1L << breakPlaceSize) - 1;
        maxPosition.z = (maxPosition.z & mask) + (1L << breakPlaceSize) - 1;
    }

    @Override
    public void place(Vector3l position, int lod) {
        long chunkStartX = minPosition.x >>> CHUNK_SIZE_BITS + lod;
        long chunkStartY = minPosition.y >>> CHUNK_SIZE_BITS + lod;
        long chunkStartZ = minPosition.z >>> CHUNK_SIZE_BITS + lod;
        long chunkEndX = Utils.getWrappedChunkCoordinate(maxPosition.x >>> CHUNK_SIZE_BITS + lod, chunkStartX, lod);
        long chunkEndY = Utils.getWrappedChunkCoordinate(maxPosition.y >>> CHUNK_SIZE_BITS + lod, chunkStartY, lod);
        long chunkEndZ = Utils.getWrappedChunkCoordinate(maxPosition.z >>> CHUNK_SIZE_BITS + lod, chunkStartZ, lod);
        ChunkSaver saver = new ChunkSaver();

        for (long chunkX = chunkStartX; chunkX <= chunkEndX; chunkX++)
            for (long chunkY = chunkStartY; chunkY <= chunkEndY; chunkY++)
                for (long chunkZ = chunkStartZ; chunkZ <= chunkEndZ; chunkZ++)
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
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        if (Properties.hasProperties(material, NO_COLLISION)) return false;

        return min.x < maxPosition.x && minPosition.x <= max.x
                && min.y < maxPosition.y && minPosition.y <= max.y
                && min.z < maxPosition.z && minPosition.z <= max.z;
    }

    @Override
    public void offsetPosition(Vector3l position) {
        offsetPositions(minPosition, maxPosition);
    }

    @Override
    public void spawnParticles(Vector3l position) {
        Player player = Game.getPlayer();
        Vector3i length = new Vector3l(maxPosition).sub(minPosition).add(1, 1, 1).toInt();
        player.getParticleCollector().addBreakParticleEffect(minPosition.x, minPosition.y, minPosition.z, length.x, length.y, length.z, material);
        player.getParticleCollector().addPlaceParticleEffect(minPosition.x, minPosition.y, minPosition.z, length.x, length.y, length.z, material);
    }

    @Override
    public void save(Placeable placeable, Saver<?> saver) {
        throw new UnsupportedOperationException("This placeable should not be saved");
    }

    public byte getMaterial() {
        return material;
    }

    private void placeInChunk(Chunk chunk) {
        long chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int inChunkStartX = (int) Utils.wrappedMax(chunkStartX, minPosition.x) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkStartY = (int) Utils.wrappedMax(chunkStartY, minPosition.y) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkStartZ = (int) Utils.wrappedMax(chunkStartZ, minPosition.z) >> chunk.LOD & CHUNK_SIZE_MASK;

        int inChunkEndX = (int) Utils.wrappedMin(chunkStartX + ((long) CHUNK_SIZE << chunk.LOD) - 1, maxPosition.x) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkEndY = (int) Utils.wrappedMin(chunkStartY + ((long) CHUNK_SIZE << chunk.LOD) - 1, maxPosition.y) >> chunk.LOD & CHUNK_SIZE_MASK;
        int inChunkEndZ = (int) Utils.wrappedMin(chunkStartZ + ((long) CHUNK_SIZE << chunk.LOD) - 1, maxPosition.z) >> chunk.LOD & CHUNK_SIZE_MASK;

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
    private final Vector3l minPosition, maxPosition;
    private final byte material;
}
