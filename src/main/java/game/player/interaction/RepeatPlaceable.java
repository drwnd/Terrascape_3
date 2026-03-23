package game.player.interaction;

import core.utils.MathUtils;
import core.utils.Saver;
import core.utils.Vector3l;

import game.player.Player;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.Structure;
import game.server.material.Properties;
import game.server.saving.ChunkSaver;
import game.settings.IntSettings;
import game.settings.ToggleSettings;
import game.utils.Utils;
import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class RepeatPlaceable implements Placeable {

    public RepeatPlaceable(ShapePlaceable placeable, Vector3l startPosition, Vector3l endPosition) {
        this.placeable = placeable;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.minPosition = Utils.min(startPosition, endPosition);
        this.maxPosition = Utils.max(startPosition, endPosition);
    }

    public static void offsetPositionFromGround(Vector3l position, int targetedSide, int preferredSize) {
        int offset = ((1 << IntSettings.BREAK_PLACE_ALIGN.value()) - preferredSize) / 2;
        if (!ToggleSettings.OFFSET_FROM_GROUND.value() || targetedSide != WEST && targetedSide != EAST) position.x += offset;
        if (!ToggleSettings.OFFSET_FROM_GROUND.value() || targetedSide != TOP && targetedSide != BOTTOM) position.y += offset;
        if (!ToggleSettings.OFFSET_FROM_GROUND.value() || targetedSide != NORTH && targetedSide != SOUTH) position.z += offset;

        offset = (1 << IntSettings.BREAK_PLACE_ALIGN.value()) - preferredSize;
        if (ToggleSettings.OFFSET_FROM_GROUND.value() && targetedSide == EAST) position.x += offset;
        if (ToggleSettings.OFFSET_FROM_GROUND.value() && targetedSide == BOTTOM) position.y += offset;
        if (ToggleSettings.OFFSET_FROM_GROUND.value() && targetedSide == SOUTH) position.z += offset;
    }

    public static void offsetPositions(Vector3l startPosition, Vector3l endPosition, int targetedSide, Placeable placeable) {
        int preferredSize = placeable == null ? 1 << IntSettings.BREAK_PLACE_SIZE.value() : MathUtils.nextLargestPowOf2(placeable.getPreferredSize());
        int breakPlaceAlign = 1 << IntSettings.BREAK_PLACE_ALIGN.value();
        int startMask = -breakPlaceAlign;
        int endMask = -preferredSize;

        offsetPositionFromGround(startPosition, targetedSide, preferredSize);
        startPosition.x &= startMask;
        startPosition.y &= startMask;
        startPosition.z &= startMask;

        endPosition.x = (endPosition.x - startPosition.x & endMask) + startPosition.x;
        endPosition.y = (endPosition.y - startPosition.y & endMask) + startPosition.y;
        endPosition.z = (endPosition.z - startPosition.z & endMask) + startPosition.z;

        if (startPosition.x <= endPosition.x) endPosition.x += preferredSize - 1;
        else startPosition.x += preferredSize - 1;
        if (startPosition.y <= endPosition.y) endPosition.y += preferredSize - 1;
        else startPosition.y += preferredSize - 1;
        if (startPosition.z <= endPosition.z) endPosition.z += preferredSize - 1;
        else startPosition.z += preferredSize - 1;
    }

    @Override
    public void place(Vector3l position, int lod) {
        int preferredSize = MathUtils.nextLargestPowOf2(placeable.getPreferredSize());
        int preferredSizeBits = Integer.numberOfTrailingZeros(preferredSize);
        int breakPlaceAlign = IntSettings.BREAK_PLACE_ALIGN.value();

        int countX = (int) (maxPosition.x - minPosition.x + preferredSize) >> preferredSizeBits;
        int countY = (int) (maxPosition.y - minPosition.y + preferredSize) >> preferredSizeBits;
        int countZ = (int) (maxPosition.z - minPosition.z + preferredSize) >> preferredSizeBits;

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
                            countX, countY, countZ, preferredSizeBits, breakPlaceAlign);
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
    public void offsetPosition(Vector3l position, int targetedSide) {
        offsetPositions(startPosition, endPosition, getPreferredSize(), placeable);
        minPosition.set(Utils.min(startPosition, endPosition));
        maxPosition.set(Utils.max(startPosition, endPosition));
    }

    @Override
    public void spawnParticles(Vector3l position) {
        Player player = Game.getPlayer();
        Vector3i length = new Vector3l(maxPosition).sub(minPosition).add(1, 1, 1).toInt();
        player.getParticleCollector().addBreakPlaceParticleEffect(
                minPosition.x, minPosition.y, minPosition.z,
                length.x, length.y, length.z,
                placeable.material, placeable.getBitMap(), placeable.getPreferredSize());
    }

    @Override
    public void save(Placeable placeable, Saver<?> saver) {
        throw new UnsupportedOperationException("This placeable should not be saved");
    }


    private void placeInChunk(Chunk chunk, int countX, int countY, int countZ, int preferredSizeBits, int align) {
        long chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int inChunkX = (int) (minPosition.x - chunkStartX) >> chunk.LOD;
        int inChunkY = (int) (minPosition.y - chunkStartY) >> chunk.LOD;
        int inChunkZ = (int) (minPosition.z - chunkStartZ) >> chunk.LOD;

        int length = 1 << Math.max(0, preferredSizeBits - chunk.LOD);
        int countDecrease = Math.max(0, chunk.LOD - preferredSizeBits);
        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, placeable.material,
                Math.max(1, countX >> countDecrease), Math.max(1, countY >> countDecrease), Math.max(1, countZ >> countDecrease),
                length, placeable.getBitMap(), chunk.LOD, align);

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
    private final Vector3l startPosition, endPosition;
    private final ShapePlaceable placeable;
}
