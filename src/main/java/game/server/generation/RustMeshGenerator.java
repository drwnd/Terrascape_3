package game.server.generation;

import core.utils.ByteArrayList;
import game.player.rendering.*;
import game.server.*;
import game.utils.Utils;

import static game.utils.Constants.*;

public record RustMeshGenerator(long chunkX, long playerChunkY, long chunkZ, int lod, ByteArrayList[] adjacentChunkLayers) implements Runnable {

    public RustMeshGenerator(long chunkX, long playerChunkY, long chunkZ, int lod) {
        this(chunkX, playerChunkY, chunkZ, lod, new ByteArrayList[]{
                new ByteArrayList(64), new ByteArrayList(64), new ByteArrayList(64),
                new ByteArrayList(64), new ByteArrayList(64), new ByteArrayList(64)
        });
    }

    @Override
    public void run() {
        World world = Game.getWorld();
        MeshCollector meshCollector = Game.getPlayer().getMeshCollector();

        for (long chunkY = playerChunkY - RENDER_DISTANCE; chunkY != playerChunkY + RENDER_DISTANCE + 1; chunkY++) {
            try {
                int chunkIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
                ChunkID expectedId = new ChunkID(chunkX, chunkY, chunkZ, lod);
                Chunk chunk = world.getChunk(chunkIndex, lod);

                if (!JavaMeshGenerator.canMesh(chunk, chunkX, chunkY, chunkZ, lod, expectedId, meshCollector, chunkIndex)) continue;

                Mesh mesh = generateMesh(chunk);
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

    private Mesh generateMesh(Chunk chunk) {
        if (chunk.isAir()) return new Mesh(chunk.X, chunk.Y, chunk.Z, chunk.LOD);

        ChunkNeighbors neighbors = chunk.getNeighbors();
        if (neighbors.areUnGenerated()) {
            Game.getServer().scheduleGeneratorRestart();
            return null;
        }

        byte[] north = fillNeighborSideLayer(neighbors.north(), adjacentChunkLayers[NORTH], SOUTH);
        byte[] top = fillNeighborSideLayer(neighbors.top(), adjacentChunkLayers[TOP], BOTTOM);
        byte[] west = fillNeighborSideLayer(neighbors.west(), adjacentChunkLayers[WEST], EAST);
        byte[] south = fillNeighborSideLayer(neighbors.south(), adjacentChunkLayers[SOUTH], NORTH);
        byte[] bottom = fillNeighborSideLayer(neighbors.bottom(), adjacentChunkLayers[BOTTOM], TOP);
        byte[] east = fillNeighborSideLayer(neighbors.east(), adjacentChunkLayers[EAST], WEST);

        byte[] materialsData;
        synchronized (chunk.getMaterials()) {
            byte[] bytes = chunk.getMaterials().getBytes();
            materialsData = new byte[bytes.length];
            System.arraycopy(bytes, 0, materialsData, 0, materialsData.length);
        }

        AABB occluder = chunk.getMaterials().getOccluder();
        byte[] surfaceEquivalent = chunk.getMaterials().getSurfaceEquivalent().getBytes();

        int xStart = (int) chunk.X << CHUNK_SIZE_BITS;
        int yStart = (int) chunk.Y << CHUNK_SIZE_BITS;
        int zStart = (int) chunk.Z << CHUNK_SIZE_BITS;

        int[] meshData = NativeFunction.generateMesh(materialsData, surfaceEquivalent,
                north, top, west, south, bottom, east,
                xStart, yStart, zStart);

        return loadMesh(meshData, chunk.X, chunk.Y, chunk.Z, chunk.LOD, occluder, AABB.newMaxChunkAABB());
    }

    private static Mesh loadMesh(int[] meshData, long chunkX, long chunkY, long chunkZ, int lod, AABB occluder, AABB occludee) {
        int[] vertexCounts = new int[OpaqueModel.FACE_COUNT];
        System.arraycopy(meshData, 0, vertexCounts, 0, vertexCounts.length);

        int opaqueVertexCount = vertexCountSum(vertexCounts);
        int waterVertexCount = meshData[7];
        int glassVertexCount = meshData[8];
//        if (opaqueVertexCount == 0 && waterVertexCount == 0 && glassVertexCount == 0) return new Mesh(chunkX, chunkY, chunkZ, lod);

        int[] opaqueVertices = new int[opaqueVertexCount * MeshGenerator.VERTICES_PER_QUAD];
        int[] transparentVertices = new int[(waterVertexCount + glassVertexCount) * MeshGenerator.VERTICES_PER_QUAD];

        System.arraycopy(meshData, 9, opaqueVertices, 0, opaqueVertices.length);
        System.arraycopy(meshData, 9 + opaqueVertices.length, transparentVertices, 0, transparentVertices.length);

        return new Mesh(opaqueVertices, vertexCounts, transparentVertices, waterVertexCount, glassVertexCount, chunkX, chunkY, chunkZ, lod, occluder, occludee);
    }

    private static byte[] fillNeighborSideLayer(Chunk neighbor, ByteArrayList data, int side) {
        neighbor.getMaterials().fillSideLayerInto(data, side);
        return data.getData();
    }

    private static int vertexCountSum(int[] vertexCounts) {
        if (vertexCounts == null) return 0;
        int sum = 0;
        for (int vertexCount : vertexCounts) sum += vertexCount;
        return sum;
    }

    private static class NativeFunction {
        public static native int[] generateMesh(byte[] materialsData, byte[] surfaceEquivalent,
                                                byte[] north, byte[] top, byte[] west, byte[] south, byte[] bottom, byte[] east,
                                                int xStart, int yStart, int zStart);
    }
}

