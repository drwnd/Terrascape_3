package game.server;

import core.sound.Sound;
import core.utils.FileManager;
import core.utils.MathUtils;

import game.server.generation.WorldGeneration;
import game.server.saving.ChunkSaver;
import game.settings.IntSettings;
import game.utils.Position;
import game.utils.Status;
import game.utils.Utils;

import java.io.File;

import static game.utils.Constants.*;

public final class World {

    public final int RENDERED_WORLD_WIDTH;
    public final int RENDERED_WORLD_WIDTH_MASK;
    public final int RENDERED_WORLD_WIDTH_BITS;
    public final int CHUNKS_PER_LOD;
    public final int LOD_COUNT;

    /**
     * Initializes the world with a given seed.
     * @param seed the seed for world generation
     */
    public World(long seed) {
        int renderDistance = IntSettings.RENDER_DISTANCE.value();

        LOD_COUNT = IntSettings.LOD_COUNT.value();
        RENDERED_WORLD_WIDTH = MathUtils.nextLargestPowOf2(renderDistance * 2 + 3);
        RENDERED_WORLD_WIDTH_MASK = RENDERED_WORLD_WIDTH - 1;
        RENDERED_WORLD_WIDTH_BITS = Integer.numberOfTrailingZeros(RENDERED_WORLD_WIDTH);
        CHUNKS_PER_LOD = RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH;

        WorldGeneration.SEED = seed;
        chunks = new Chunk[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH];
    }

    /**
     * Creates a new world based on an existing one, optionally updating the render distance.
     * @param oldWorld the previous world instance
     * @param updateRenderDistance whether to update the world's dimensions based on the current render distance
     */
    public World(World oldWorld, boolean updateRenderDistance) {
        if (updateRenderDistance) {
            int renderDistance = IntSettings.RENDER_DISTANCE.value();

            LOD_COUNT = oldWorld.LOD_COUNT;
            RENDERED_WORLD_WIDTH = MathUtils.nextLargestPowOf2(renderDistance * 2 + 3);
            RENDERED_WORLD_WIDTH_MASK = RENDERED_WORLD_WIDTH - 1;
            RENDERED_WORLD_WIDTH_BITS = Integer.numberOfTrailingZeros(RENDERED_WORLD_WIDTH);
            CHUNKS_PER_LOD = RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH;

            chunks = new Chunk[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH];
            name = oldWorld.name;

            Position playerPosition = Game.getPlayer().getPosition();
            ChunkSaver saver = new ChunkSaver();

            for (int lod = 0; lod < LOD_COUNT; lod++) {
                long cameraX = playerPosition.longX >>> CHUNK_SIZE_BITS + lod;
                long cameraY = playerPosition.longY >>> CHUNK_SIZE_BITS + lod;
                long cameraZ = playerPosition.longZ >>> CHUNK_SIZE_BITS + lod;

                for (Chunk chunk : oldWorld.chunks[lod]) {
                    if (chunk == null) continue;
                    if (Utils.outsideChunkKeepDistance(cameraX, cameraY, cameraZ, chunk.X, chunk.Y, chunk.Z, chunk.LOD)) {
                        if (chunk.isModified())
                            saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
                        continue;
                    }
                    chunk.INDEX = Utils.getChunkIndex(chunk.X, chunk.Y, chunk.Z, chunk.LOD, renderDistance);
                    chunks[chunk.LOD][chunk.INDEX] = chunk;
                }
            }
        } else {    // Update LOD_COUNT
            LOD_COUNT = IntSettings.LOD_COUNT.value();
            RENDERED_WORLD_WIDTH = oldWorld.RENDERED_WORLD_WIDTH;
            RENDERED_WORLD_WIDTH_MASK = oldWorld.RENDERED_WORLD_WIDTH_MASK;
            RENDERED_WORLD_WIDTH_BITS = oldWorld.RENDERED_WORLD_WIDTH_BITS;
            CHUNKS_PER_LOD = oldWorld.CHUNKS_PER_LOD;

            chunks = new Chunk[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH * RENDERED_WORLD_WIDTH];
            name = oldWorld.name;
            System.arraycopy(oldWorld.chunks, 0, chunks, 0, Math.min(LOD_COUNT, oldWorld.LOD_COUNT));
        }
    }

    /**
     * Initializes world-related systems and generates higher LODs if necessary.
     */
    public static void init() {
        ChunkSaver.generateHigherLODs();
        Server.loadImmediateSurroundings();
        Sound.setDistanceScaler(0.0625F);
    }

    public Chunk getChunk(long chunkX, long chunkY, long chunkZ, int lod) {
        return chunks[lod][Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod)];
    }

    public Chunk getChunk(int chunkIndex, int lod) {
        return chunks[lod][chunkIndex];
    }

    public Chunk[] getLod(int lod) {
        return chunks[lod];
    }

    public void storeChunk(Chunk chunk) {
        chunks[chunk.LOD][chunk.getIndex()] = chunk;
    }

    /**
     * Retrieves the generation status of a chunk.
     * @param chunkX the x-coordinate of the chunk (Chunk Coordinates) at the specified LOD
     * @param chunkY the y-coordinate of the chunk (Chunk Coordinates) at the specified LOD
     * @param chunkZ the z-coordinate of the chunk (Chunk Coordinates) at the specified LOD
     * @param lod the level of detail
     * @return the current generation status of the chunk
     */
    public Status getGenerationStatus(long chunkX, long chunkY, long chunkZ, int lod) {
        Chunk chunk = getChunk(chunkX, chunkY, chunkZ, lod);
        if (chunk == null) return Status.NOT_STARTED;
        return chunk.getGenerationStatus();
    }

    /**
     * Gets the material at the specified LOD-specific world block coordinates.
     * @param x the x-coordinate (LOD-Specific World Block Coordinates)
     * @param y the y-coordinate (LOD-Specific World Block Coordinates)
     * @param z the z-coordinate (LOD-Specific World Block Coordinates)
     * @param lod the level of detail
     * @return the material at the specified position
     */
    public byte getMaterial(long x, long y, long z, int lod) {
        Chunk chunk = getChunk(x >> CHUNK_SIZE_BITS, y >> CHUNK_SIZE_BITS, z >> CHUNK_SIZE_BITS, lod);
        if (chunk == null) return OUT_OF_WORLD;
        return chunk.getSaveMaterial((int) x & CHUNK_SIZE_MASK, (int) y & CHUNK_SIZE_MASK, (int) z & CHUNK_SIZE_MASK);
    }

    public void setNull(int chunkIndex, int lod) {
        chunks[lod][chunkIndex] = null;
    }

    /**
     * Saves all modified chunks and cleans up temporary world files.
     */
    public void cleanUp() {
        ChunkSaver saver = new ChunkSaver();
        for (Chunk[] lod : chunks)
            for (Chunk chunk : lod) {
                if (chunk == null || !chunk.isModified()) continue;
                saver.save(chunk, ChunkSaver.getSaveFileLocation(chunk.ID, chunk.LOD));
            }

        deleteHigherLODs(LOD_COUNT - 1);
    }

    /**
     * Deletes saved data for levels of detail higher than the specified maximum.
     * @param maxKeptLod the highest LOD to keep
     */
    public static void deleteHigherLODs(int maxKeptLod) {
        File[] lodFiles = FileManager.getChildren(new File(ChunkSaver.getSaveFileLocation()));
        for (File file : lodFiles) {
            String fileLod = file.getName();
            if (!MathUtils.isInteger(fileLod, 10) || Integer.parseInt(fileLod) > maxKeptLod) FileManager.delete(file);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private String name;
    private final Chunk[][] chunks;
}
