import game.player.rendering.Mesh;
import game.player.rendering.MeshGenerator;
import game.server.Chunk;
import game.server.Game;
import game.server.World;
import game.server.generation.GenerationData;
import game.server.generation.RustMeshGenerator;
import game.server.generation.WorldGeneration;
import game.settings.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class PerformanceTester {

    private static final int CHUNK_COUNT = 16;
    private static final boolean USE_RUST_FUNCTIONS = true;

    public static void main(String[] args) {
        if (USE_RUST_FUNCTIONS) {
            Path path = Paths.get("src/rust_functions/lib/target/release/lib.dll");
            System.out.println("loading dll at " + path.toAbsolutePath());
            try {
                System.load(path.toAbsolutePath().toString());
            } catch (Exception exception) {
                System.err.println("Failed to load dll");
                exception.printStackTrace();
                return;
            }
        }

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

        System.out.printf("Rust time: %d%n", RustMeshGenerator.rustTime / 1_000_000);
        System.out.printf("Java time: %d%n", RustMeshGenerator.javaTime / 1_000_000);
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
        if (USE_RUST_FUNCTIONS) {
            RustMeshGenerator meshGenerator = new RustMeshGenerator(chunkX, 0, chunkZ, lod);
            World world = Game.getWorld();

            for (int chunkY = -chunkCount + 1; chunkY < chunkCount - 1; chunkY++) {
                Mesh mesh = meshGenerator.generateMesh(world.getChunk(chunkX, chunkY, chunkZ, lod));
                if (mesh == null) System.err.printf("Chunk at x:%d, y:%d, z:%d couldn't generate a mesh", chunkX, chunkY, chunkZ);
            }

        } else {
            MeshGenerator meshGenerator = new MeshGenerator();
            World world = Game.getWorld();

            for (int chunkY = -chunkCount + 1; chunkY < chunkCount - 1; chunkY++) {
                Mesh mesh = meshGenerator.generateMesh(world.getChunk(chunkX, chunkY, chunkZ, lod));
                if (mesh == null) System.err.printf("Chunk at x:%d, y:%d, z:%d couldn't generate a mesh", chunkX, chunkY, chunkZ);
            }
        }
    }

    private PerformanceTester() {

    }
}
