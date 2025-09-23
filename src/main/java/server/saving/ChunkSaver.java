package server.saving;

import server.Chunk;
import server.Game;
import server.MaterialsData;
import utils.Utils;

public final class ChunkSaver extends Saver<Chunk> {

    public static String getSaveFileLocation(int chunkX, int chunkY, int chunkZ, int lod) {
        // return "saves/" + Game.getWorld().getName() + "/chunks/" + lod + "/" + Utils.getChunkId(chunkX, chunkY, chunkZ);
        return "saves/%s/chunks/%s/%s".formatted(Game.getWorld().getName(), lod, Utils.getChunkId(chunkX, chunkY, chunkZ));
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
}
