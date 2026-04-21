import game.player.rendering.Mesh;
import game.player.rendering.MeshGenerator;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.GenerationData;
import game.server.generation.WorldGeneration;

public final class PerformanceTester {

    private static final int CHUNK_COUNT = 128;

    public static void main(String[] args) {
        Game.setTemporaryWorld(new World(0x9EF6E7FAF3299DDDL));
        long totalStart = System.nanoTime();

        for (int lod = 0; CHUNK_COUNT >> lod != 0; lod++) {
            int chunkCount = CHUNK_COUNT >> lod;

            long generationStart = System.nanoTime();
            for (int chunkX = 0; chunkX < chunkCount; chunkX++)
                for (int chunkZ = 0; chunkZ < chunkCount; chunkZ++) generateColumn(chunkX, chunkZ, chunkCount / 2, lod);
            long generationTime = System.nanoTime() - generationStart;

            System.out.printf("Generated lod %d in %dms %n", lod, generationTime / 1_000_000);

            long meshingStart = System.nanoTime();
            for (int chunkX = 1; chunkX < chunkCount - 1; chunkX++)
                for (int chunkZ = 1; chunkZ < chunkCount - 1; chunkZ++) meshColumn(chunkX, chunkZ, chunkCount / 2, lod);
            long meshingTime = System.nanoTime() - meshingStart;

            System.out.printf("Meshed lod %d in %dms %n", lod, meshingTime / 1_000_000);
        }

        long totalTime = System.nanoTime() - totalStart;
        System.out.printf("Total time %ds%n", totalTime / 1_000_000_000);
    }

    private static void generateColumn(int chunkX, int chunkZ, int chunkCount, int lod) {
        GenerationData generationData = new GenerationData(chunkX, chunkZ, lod);
        World world = Game.getWorld();

        for (int chunkY = -chunkCount; chunkY < chunkCount; chunkY++) {
            Chunk chunk = new Chunk(chunkX, chunkY, chunkZ, lod);
            WorldGeneration.generate(chunk, generationData);
            world.storeChunk(chunk);
        }
    }

    private static void meshColumn(int chunkX, int chunkZ, int chunkCount, int lod) {
        MeshGenerator meshGenerator = new MeshGenerator();
        World world = Game.getWorld();

        for (int chunkY = -chunkCount + 1; chunkY < chunkCount - 1; chunkY++) {
            Mesh mesh = meshGenerator.generateMeshNoSaving(world.getChunk(chunkX, chunkY, chunkZ, lod));
            if (mesh == null) System.err.printf("Chunk at x:%d, y:%d, z:%d couldn't generate a mesh", chunkX, chunkY, chunkZ);
        }
    }

    private PerformanceTester() {

    }
}
