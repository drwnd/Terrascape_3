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

    public static void offsetPositionFromGround(Vector3l position, int targetedSide, int lengthX, int lengthY, int lengthZ) {
        int offset = 1 << IntSettings.BREAK_PLACE_ALIGN.value();
        if (!ToggleSettings.OFFSET_FROM_GROUND.value() || targetedSide != WEST && targetedSide != EAST) position.x += offset - lengthX >> 1;
        if (!ToggleSettings.OFFSET_FROM_GROUND.value() || targetedSide != TOP && targetedSide != BOTTOM) position.y += offset - lengthY >> 1;
        if (!ToggleSettings.OFFSET_FROM_GROUND.value() || targetedSide != NORTH && targetedSide != SOUTH) position.z += offset - lengthZ >> 1;

        offset = 1 << IntSettings.BREAK_PLACE_ALIGN.value();
        if (ToggleSettings.OFFSET_FROM_GROUND.value() && targetedSide == EAST) position.x += offset - lengthX;
        if (ToggleSettings.OFFSET_FROM_GROUND.value() && targetedSide == BOTTOM) position.y += offset - lengthY;
        if (ToggleSettings.OFFSET_FROM_GROUND.value() && targetedSide == SOUTH) position.z += offset - lengthZ;
    }

    public static void offsetPositions(Vector3l startPosition, Vector3l endPosition, int targetedSide, Placeable placeable) {
        int lengthX = placeable == null ? 1 << IntSettings.BREAK_PLACE_SIZE.value() : placeable.getLengthX();
        int lengthY = placeable == null ? 1 << IntSettings.BREAK_PLACE_SIZE.value() : placeable.getLengthY();
        int lengthZ = placeable == null ? 1 << IntSettings.BREAK_PLACE_SIZE.value() : placeable.getLengthZ();
        int startMask = -(1 << IntSettings.BREAK_PLACE_ALIGN.value());

        offsetPositionFromGround(startPosition, targetedSide, lengthX, lengthY, lengthZ);
        startPosition.x &= startMask;
        startPosition.y &= startMask;
        startPosition.z &= startMask;

        endPosition.x -= MathUtils.mod(endPosition.x - startPosition.x, lengthX);
        endPosition.y -= MathUtils.mod(endPosition.y - startPosition.y, lengthY);
        endPosition.z -= MathUtils.mod(endPosition.z - startPosition.z, lengthZ);

        if (startPosition.x <= endPosition.x) endPosition.x += lengthX - 1;
        else startPosition.x += lengthX - 1;
        if (startPosition.y <= endPosition.y) endPosition.y += lengthY - 1;
        else startPosition.y += lengthY - 1;
        if (startPosition.z <= endPosition.z) endPosition.z += lengthZ - 1;
        else startPosition.z += lengthZ - 1;
    }

    @Override
    public void place(Vector3l position, int lod) {
        int countX = (int) (maxPosition.x - minPosition.x + placeable.getLengthX()) / placeable.getLengthX();
        int countY = (int) (maxPosition.y - minPosition.y + placeable.getLengthY()) / placeable.getLengthY();
        int countZ = (int) (maxPosition.z - minPosition.z + placeable.getLengthZ()) / placeable.getLengthZ();

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
                    placeInChunk(saver.loadAndGenerate(chunkX, chunkY, chunkZ, lod), countX, countY, countZ);
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
        if (Properties.hasProperties(placeable.getMaterial(), NO_COLLISION)) return false;

        return min.x < maxPosition.x && minPosition.x <= max.x
                && min.y < maxPosition.y && minPosition.y <= max.y
                && min.z < maxPosition.z && minPosition.z <= max.z;
    }

    @Override
    public void offsetPosition(Vector3l position, int targetedSide) {
        offsetPositions(startPosition, endPosition, targetedSide, placeable);
        minPosition.set(Utils.min(startPosition, endPosition));
        maxPosition.set(Utils.max(startPosition, endPosition));
    }

    @Override
    public void spawnParticles(Vector3l position) {
        Player player = Game.getPlayer();
        int countX = (int) (maxPosition.x - minPosition.x + placeable.getLengthX()) / placeable.getLengthX();
        int countY = (int) (maxPosition.y - minPosition.y + placeable.getLengthY()) / placeable.getLengthY();
        int countZ = (int) (maxPosition.z - minPosition.z + placeable.getLengthZ()) / placeable.getLengthZ();

        player.getParticleCollector().addBreakPlaceParticleEffect(minPosition.x, minPosition.y, minPosition.z, countX, countY, countZ, placeable);
    }

    @Override
    public void save(Saver<?> saver) {
        throw new UnsupportedOperationException("This placeable should not be saved");
    }


    private void placeInChunk(Chunk chunk, int countX, int countY, int countZ) {
        long chunkStartX = chunk.X << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartY = chunk.Y << CHUNK_SIZE_BITS + chunk.LOD;
        long chunkStartZ = chunk.Z << CHUNK_SIZE_BITS + chunk.LOD;

        int inChunkX = (int) (minPosition.x - chunkStartX) >> chunk.LOD;
        int inChunkY = (int) (minPosition.y - chunkStartY) >> chunk.LOD;
        int inChunkZ = (int) (minPosition.z - chunkStartZ) >> chunk.LOD;

        chunk.storeMaterial(inChunkX, inChunkY, inChunkZ, countX, countY, countZ, chunk.LOD, placeable);

        affectedChunks.add(chunk);
        World world = Game.getWorld();
        if (inChunkX == 0) affectedChunks.add(world.getChunk(chunk.X - 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkY == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y - 1, chunk.Z, chunk.LOD));
        if (inChunkZ == 0) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z - 1, chunk.LOD));
        if (inChunkX + countX * placeable.getLengthX() == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X + 1, chunk.Y, chunk.Z, chunk.LOD));
        if (inChunkY + countY * placeable.getLengthY() == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y + 1, chunk.Z, chunk.LOD));
        if (inChunkZ + countZ * placeable.getLengthZ() == CHUNK_SIZE) affectedChunks.add(world.getChunk(chunk.X, chunk.Y, chunk.Z + 1, chunk.LOD));
    }

    private final ArrayList<Chunk> affectedChunks = new ArrayList<>();
    private final Vector3l minPosition, maxPosition;
    private final Vector3l startPosition, endPosition;
    private final ShapePlaceable placeable;
}
