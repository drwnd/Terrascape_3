import game.player.rendering.Mesh;
import game.player.rendering.MeshGenerator;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.GenerationData;
import game.server.generation.WorldGeneration;
import game.settings.IntSettings;

public final class PerformanceTester {

    private static final int CHUNK_COUNT_XZ = 128;
    private static final int CHUNK_COUNT_Y = 32;

    public static void main(String[] args) {
        IntSettings.RENDER_DISTANCE.setValue(CHUNK_COUNT_XZ / 2 - 3);
        IntSettings.LOD_COUNT.setValue(Integer.numberOfTrailingZeros(CHUNK_COUNT_XZ) + 1);
        Game.setTemporaryWorld(new World(0x9EF6E7FAF3299DDDL, null, null, true));
        long totalStart = System.nanoTime();

        int chunkCountY = CHUNK_COUNT_Y;
        int chunkCountXZ = CHUNK_COUNT_XZ;

        long generationStart = System.nanoTime();
        for (int chunkX = 28; chunkX < chunkCountXZ + 28; chunkX++)
            for (int chunkZ = 116; chunkZ < chunkCountXZ + 116; chunkZ++) generateColumn(chunkX, chunkZ, chunkCountY / 2);
        long generationTime = System.nanoTime() - generationStart;

        System.out.printf("Generated lod %d in %dms %n", 0, generationTime / 1_000_000);

        long meshingStart = System.nanoTime();
        for (int chunkX = 29; chunkX < chunkCountXZ + 27; chunkX++)
            for (int chunkZ = 117; chunkZ < chunkCountXZ + 115; chunkZ++) meshColumn(chunkX, chunkZ, chunkCountY / 2);
        long meshingTime = System.nanoTime() - meshingStart;

        System.out.printf("Meshed lod %d in %dms %n", 0, meshingTime / 1_000_000);


        long totalTime = System.nanoTime() - totalStart;
        System.out.printf("Total time %ds%n", totalTime / 1_000_000_000);
    }

    private static void generateColumn(int chunkX, int chunkZ, int chunkCount) {
        GenerationData generationData = new GenerationData(chunkX, chunkZ, 0);
        World world = Game.getWorld();

        for (int chunkY = -chunkCount; chunkY < chunkCount; chunkY++) {
            Chunk chunk = new Chunk(chunkX, chunkY, chunkZ, 0);
            WorldGeneration.generate(chunk, generationData);
            world.storeChunk(chunk);
        }
    }

    private static void meshColumn(int chunkX, int chunkZ, int chunkCount) {
        MeshGenerator meshGenerator = new MeshGenerator();
        World world = Game.getWorld();

        for (int chunkY = -chunkCount + 1; chunkY < chunkCount - 1; chunkY++) {
            Mesh mesh = meshGenerator.generateMesh(world.getChunk(chunkX, chunkY, chunkZ, 0));
            if (mesh == null) System.err.printf("Chunk at x:%d, y:%d, z:%d couldn't generate a mesh", chunkX, chunkY, chunkZ);
        }
    }

    private PerformanceTester() {

    }
}
