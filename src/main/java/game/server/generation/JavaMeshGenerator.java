package game.server.generation;

import game.player.rendering.Mesh;
import game.player.rendering.MeshCollector;
import game.player.rendering.MeshGenerator;
import game.server.Chunk;
import game.server.ChunkID;
import game.server.Game;
import game.server.World;
import game.utils.Status;
import game.utils.Utils;

import static game.utils.Constants.RENDER_DISTANCE;

record JavaMeshGenerator(long chunkX, long playerChunkY, long chunkZ, int lod) implements Runnable {

    @Override
    public void run() {

        MeshGenerator meshGenerator = new MeshGenerator();
        World world = Game.getWorld();
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();

        for (long chunkY = playerChunkY - RENDER_DISTANCE; chunkY != playerChunkY + RENDER_DISTANCE + 1; chunkY++) {
            try {
                int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
                ChunkID expectedId = new ChunkID(chunkX, chunkY, chunkZ, lod);
                Chunk chunk = world.getChunk(chunkIndex, lod);

                if (chunk == null) {
                    System.err.printf("to mesh chunk is null %d %d %d %d%n", chunkX, chunkY, chunkZ, lod);
                    continue;
                }
                if (!chunk.ID.equals(expectedId)) {
                    System.err.printf("Chunk has wrong ID %d %d %d %d is %s should be %s%n", chunkX, chunkY, chunkZ, lod, chunk.ID, expectedId);
                    continue;
                }
                if (chunk.getGenerationStatus() != Status.DONE) {
                    System.err.printf("to mesh chunk hasn't been generated %s%n", chunk.getGenerationStatus().name());
                    System.err.printf("%d %d %d %d%n", chunkX, chunkY, chunkZ, lod);
                    continue;
                }

                if (meshCollector.isMeshed(chunkIndex, lod)) continue;
                meshCollector.setMeshed(true, chunkIndex, lod);

                Mesh mesh = meshGenerator.generateMesh(chunk);
                if (mesh == null) meshCollector.setMeshed(false, chunkIndex, lod);
                else meshCollector.queueMesh(mesh);

            } catch (Exception exception) {
                System.err.println("Meshing:");
                System.err.println(exception.getClass());
                exception.printStackTrace();
                System.err.printf("%d %d %d%n", chunkX, chunkY, chunkZ);
            }
        }
    }
}
