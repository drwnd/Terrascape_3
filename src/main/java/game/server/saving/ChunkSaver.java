package game.server.saving;

import game.server.Chunk;
import game.server.Game;
import game.server.MaterialsData;
import game.utils.Utils;

public final class ChunkSaver extends Saver<Chunk> {

    public static String getSaveFileLocation(long id, int lod) {
        // return "saves/" + Game.getWorld().getName() + "/chunks/" + lod + "/" + Utils.getChunkId(chunkX, chunkY, chunkZ);
        return "saves/%s/chunks/%s/%s".formatted(Game.getWorld().getName(), lod, id);
    }

    public Chunk load(int chunkX, int chunkY, int chunkZ, int lod) {
        long expectedID = Utils.getChunkId(chunkX, chunkY, chunkZ);
        Chunk chunk = Game.getWorld().getChunk(chunkX, chunkY, chunkZ, lod);

        if (chunk == null) return load(chunkX, chunkY, chunkZ, lod, expectedID);
        if (chunk.ID != expectedID) {
            if (chunk.isModified()) save(chunk, getSaveFileLocation(chunk.ID, chunk.LOD));
            return load(chunkX, chunkY, chunkZ, lod, expectedID);
        }
        return chunk;
    }

    private Chunk load(int chunkX, int chunkY, int chunkZ, int lod, long id) {
        Chunk chunk = load(getSaveFileLocation(id, lod));
        if (chunk == null) chunk = new Chunk(chunkX, chunkY, chunkZ, lod);
        else chunk.setGenerated();

        Game.getWorld().storeChunk(chunk);
        return chunk;
    }

    @Override
    void save(Chunk chunk) {
        saveInt(chunk.X);
        saveInt(chunk.Y);
        saveInt(chunk.Z);
        saveInt(chunk.LOD);
        saveByteArray(chunk.getMaterials().getBytes());
    }

    @Override
    Chunk load() {
        int x = loadInt();
        int y = loadInt();
        int z = loadInt();
        int lod = loadInt();
        byte[] materials = loadByteArray();

        Chunk chunk = new Chunk(x, y, z, lod);
        chunk.setMaterials(new MaterialsData(materials));
        return chunk;
    }

    @Override
    Chunk getDefault() {
        return null;
    }
}
