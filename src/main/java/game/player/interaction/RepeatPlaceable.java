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

public final class RepeatPlaceable implements Placeable {

    public RepeatPlaceable(ShapePlaceable placeable, Vector3l position1, Vector3l position2) {
        this.placeable = placeable;
        this.minPosition = Utils.min(position1, position2);
        this.maxPosition = Utils.max(position1, position2);
    }

    public static void offsetPositions(Vector3l minPosition, Vector3l maxPosition) {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        int breakPlaceAlign = Game.getPlayer().getInteractionHandler().getBreakPlaceAlign();
        int minMask = -(1 << breakPlaceAlign);
        int maxMask = -(1 << breakPlaceSize);

        minPosition.x &= minMask;
        minPosition.y &= minMask;
        minPosition.z &= minMask;

        maxPosition.x = (maxPosition.x - minPosition.x & maxMask) + minPosition.x + (1L << breakPlaceSize) - 1;
        maxPosition.y = (maxPosition.y - minPosition.y & maxMask) + minPosition.y + (1L << breakPlaceSize) - 1;
        maxPosition.z = (maxPosition.z - minPosition.z & maxMask) + minPosition.z + (1L << breakPlaceSize) - 1;
    }

    @Override
    public void place(Vector3l position, int lod) {
        int breakPlaceSize = Game.getPlayer().getInteractionHandler().getBreakPlaceSize();
        int breakPlaceAlign = Game.getPlayer().getInteractionHandler().getBreakPlaceAlign();
        int countX = (int) (maxPosition.x - minPosition.x + (1 << breakPlaceSize) - 1) >> breakPlaceSize;
        int countY = (int) (maxPosition.y - minPosition.y + (1 << breakPlaceSize) - 1) >> breakPlaceSize;
        int countZ = (int) (maxPosition.z - minPosition.z + (1 << breakPlaceSize) - 1) >> breakPlaceSize;

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
                    placeInChunk(saver.loadAndGenerate(chunkX, chunkY, chunkZ, lod),
                            countX, countY, countZ, breakPlaceSize, breakPlaceAlign);
    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return affectedChunks;
    }

    @Override
    public Structure getStructure() {
        return placeable.getStructure();
    }

    @Override
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        if (Properties.hasProperties(placeable.material, NO_COLLISION)) return false;

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
        player.getParticleCollector().addBreakPlaceParticleEffect(
                minPosition.x, minPosition.y, minPosition.z,
                length.x, length.y, length.z,
                placeable.material, placeable.getBitMap(), 1 << player.getInteractionHandler().getBreakPlaceSize());
    }

    @Override
    public void save(Placeable placeable, Saver<?> saver) {
        throw new UnsupportedOperationException("This placeable should not be saved");
    }


    private void placeInChunk(Chunk chunk, int countX, int countY, int countZ, int breakPlaceSize, int align) {
        long chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int inChunkX = (int) (minPosition.x - chunkStartX) >> chunk.LOD;
        int inChunkY = (int) (minPosition.y - chunkStartY) >> chunk.LOD;
        int inChunkZ = (int) (minPosition.z - chunkStartZ) >> chunk.LOD;

        int length = 1 << Math.max(0, breakPlaceSize - chunk.LOD);
        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, placeable.material, countX, countY, countZ, length, placeable.getBitMap(), chunk.LOD, align);

        affectedChunks.add(chunk);
        World world = Game.getWorld();
        affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, chunk.LOD));
        affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, chunk.LOD));
    }

    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private final Vector3l minPosition, maxPosition;
    private final ShapePlaceable placeable;
}
