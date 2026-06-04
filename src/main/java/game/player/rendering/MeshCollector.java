package game.player.rendering;

import core.utils.Vector3l;

import game.server.Game;
import game.utils.Position;
import game.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import static game.player.rendering.MeshGenerator.INTS_PER_VERTEX;
import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public final class MeshCollector {

    public void uploadAllMeshes() {
        Vector3l playerChunkCoordinate = Game.getPlayer().getPosition().getChunkCoordinate();
        synchronized (meshQueue) {
            for (Mesh mesh : meshQueue) {
                long playerChunkX = playerChunkCoordinate.x >> mesh.lod();
                long playerChunkY = playerChunkCoordinate.y >> mesh.lod();
                long playerChunkZ = playerChunkCoordinate.z >> mesh.lod();
                if (Utils.outsideRenderKeepDistance(playerChunkX, playerChunkY, playerChunkZ, mesh.chunkX(), mesh.chunkY(), mesh.chunkZ(), mesh.lod()))
                    continue;

                upload(mesh);
            }
            meshQueue.clear();
        }
    }

    public void deleteOldMeshes() {
        synchronized (toDeleteOpaqueModels) {
            for (OpaqueModel model : toDeleteOpaqueModels) allocator.memFree(model.bufferOrStart());
            toDeleteOpaqueModels.clear();
        }
        synchronized (toDeleteTransparentModels) {
            for (TransparentModel model : toDeleteTransparentModels) allocator.memFree(model.bufferOrStart());
            toDeleteTransparentModels.clear();
        }
    }

    public void queueMesh(Mesh mesh) {
        synchronized (meshQueue) {
            meshQueue.add(mesh);
        }
    }

    public boolean isMeshed(int chunkIndex, int lod) {
        return (isMeshed[lod][chunkIndex >> 6] & 1L << chunkIndex) != 0;
    }

    public void setMeshed(boolean meshed, int chunkIndex, int lod) {
        if (meshed) isMeshed[lod][chunkIndex >> 6] |= 1L << chunkIndex;
        else isMeshed[lod][chunkIndex >> 6] &= ~(1L << chunkIndex);
    }

    public void removeMesh(int chunkIndex, int lod) {
        OpaqueModel opaqueModel = getOpaqueModel(chunkIndex, lod);
        if (opaqueModel != null) {
            synchronized (toDeleteOpaqueModels) {
                toDeleteOpaqueModels.add(opaqueModel);
            }
            setOpaqueModel(null, chunkIndex, lod);
        }

        TransparentModel transparentModel = getTransparentModel(chunkIndex, lod);
        if (transparentModel != null) {
            synchronized (toDeleteTransparentModels) {
                toDeleteTransparentModels.add(transparentModel);
            }
            setTransparentModel(null, chunkIndex, lod);
        }
        setMeshed(false, chunkIndex, lod);
    }

    public OpaqueModel getOpaqueModel(int chunkIndex, int lod) {
        return opaqueModels[lod][chunkIndex];
    }

    public TransparentModel getTransparentModel(int chunkIndex, int lod) {
        return transparentModels[lod][chunkIndex];
    }

    public AABB getOccluder(int chunkIndex, int lod) {
        return occluders[lod][chunkIndex];
    }

    public AABB getOccludee(int chunkIndex, int lod) {
        return occludees[lod][chunkIndex];
    }

    public boolean isModelPresent(long lodModelX, long lodModelY, long lodModelZ, int lod) {
        return getOpaqueModel(Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod), lod) != null;
    }

    public int getBuffer() {
        return allocator.getBuffer();
    }

    public MemoryAllocator getAllocator() {
        return allocator;
    }

    public void cleanUp() {
        allocator.cleanUp();
    }

    public void removeAll() {
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            for (OpaqueModel model : opaqueModels[lod]) if (model != null) allocator.memFree(model.bufferOrStart());
            for (TransparentModel model : transparentModels[lod]) if (model != null) allocator.memFree(model.bufferOrStart());

            Arrays.fill(opaqueModels[lod], null);
            Arrays.fill(transparentModels[lod], null);

            Arrays.fill(isMeshed[lod], 0L);
        }
    }

    public boolean isIsolated(long chunkX, long chunkY, long chunkZ, int lod) {
        OpaqueModel model;
        return ((model = getOpaqueModel(Utils.getChunkIndex(chunkX - 1, chunkY, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX + 1, chunkY, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY - 1, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY + 1, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY, chunkZ - 1, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY, chunkZ + 1, lod), lod)) == null || model.isEmpty());
    }

    public void sortNearestWaterModel(Position cameraPosition) {
        int chunkIndex = Utils.getChunkIndex(
                cameraPosition.longX >>> CHUNK_SIZE_BITS,
                cameraPosition.longY >>> CHUNK_SIZE_BITS,
                cameraPosition.longZ >>> CHUNK_SIZE_BITS, 0);
        TransparentModel model = getTransparentModel(chunkIndex, 0);
        if (model == null || model.isWaterEmpty()) return;

        int cameraX = (int) cameraPosition.longX, cameraY = (int) cameraPosition.longY, cameraZ = (int) cameraPosition.longZ;
        int[] verticesData = model.waterVertices();
        Vertex[] vertices = new Vertex[verticesData.length / INTS_PER_VERTEX];

        for (int index = 0; index < verticesData.length; index += INTS_PER_VERTEX) {
            vertices[index >> 2] = new Vertex(verticesData[index], verticesData[index + 1], verticesData[index + 2], verticesData[index + 3]);
        }

        Arrays.sort(vertices, (a, b) -> b.manhattanDistanceFrom(cameraX, cameraY, cameraZ) - a.manhattanDistanceFrom(cameraX, cameraY, cameraZ));

        for (int index = 0; index < verticesData.length; index += INTS_PER_VERTEX) {
            Vertex vertex = vertices[index >> 2];
            verticesData[index] = vertex.x;
            verticesData[index + 1] = vertex.y;
            verticesData[index + 2] = vertex.z;
            verticesData[index + 3] = vertex.textureData;
        }
        glNamedBufferSubData(allocator.getBuffer(), model.bufferOrStart(), verticesData);
    }


    private void deleteMesh(int chunkIndex, int lod) {
        OpaqueModel opaqueModel = getOpaqueModel(chunkIndex, lod);
        TransparentModel transparentModel = getTransparentModel(chunkIndex, lod);

        setOpaqueModel(null, chunkIndex, lod);
        setTransparentModel(null, chunkIndex, lod);
        setMeshed(false, chunkIndex, lod);

        if (opaqueModel != null) allocator.memFree(opaqueModel.bufferOrStart());
        if (transparentModel != null) allocator.memFree(transparentModel.bufferOrStart());
    }

    private void setOpaqueModel(OpaqueModel model, int index, int lod) {
        opaqueModels[lod][index] = model;
    }

    private void setTransparentModel(TransparentModel model, int index, int lod) {
        transparentModels[lod][index] = model;
    }

    private void upload(Mesh mesh) {
        int chunkIndex = Utils.getChunkIndex(mesh.chunkX(), mesh.chunkY(), mesh.chunkZ(), mesh.lod());
        deleteMesh(chunkIndex, mesh.lod());

        OpaqueModel opaqueModel = loadOpaqueModel(mesh);
        setOpaqueModel(opaqueModel, chunkIndex, mesh.lod());

        TransparentModel transparentModel = loadTransparentModel(mesh);
        setTransparentModel(transparentModel, chunkIndex, mesh.lod());
        setMeshed(true, chunkIndex, mesh.lod());

        occluders[mesh.lod()][chunkIndex] = mesh.occluder();
        occludees[mesh.lod()][chunkIndex] = mesh.occludee();
    }

    private OpaqueModel loadOpaqueModel(Mesh mesh) {
        int start = allocator.memAlloc(mesh.getOpaqueByteSize());
        if (start == -1) return new OpaqueModel(mesh.getWorldCoordinate(), null, -1, mesh.lod(), false);

        glNamedBufferSubData(allocator.getBuffer(), start, mesh.opaqueVertices());
        return new OpaqueModel(mesh.getWorldCoordinate(), mesh.vertexCounts(), start, mesh.lod(), false);
    }

    private TransparentModel loadTransparentModel(Mesh mesh) {
        int start = allocator.memAlloc(mesh.getTransparentByteSize());
        if (start == -1) return new TransparentModel(mesh.getWorldCoordinate(), 0, 0, -1, mesh.lod(), null);

        glNamedBufferSubData(allocator.getBuffer(), start, mesh.transparentVertices());
        return new TransparentModel(mesh.getWorldCoordinate(), mesh.waterVertexCount(), mesh.glassVertexCount(), start, mesh.lod(), mesh.getWaterVertices());
    }

    private final MemoryAllocator allocator = new MemoryAllocator(1 << 29);
    private final ArrayList<Mesh> meshQueue = new ArrayList<>();
    private final ArrayList<OpaqueModel> toDeleteOpaqueModels = new ArrayList<>();
    private final ArrayList<TransparentModel> toDeleteTransparentModels = new ArrayList<>();

    private final OpaqueModel[][] opaqueModels = new OpaqueModel[LOD_COUNT][CHUNKS_PER_LOD];
    private final TransparentModel[][] transparentModels = new TransparentModel[LOD_COUNT][CHUNKS_PER_LOD];
    private final AABB[][] occluders = new AABB[LOD_COUNT][CHUNKS_PER_LOD];
    private final AABB[][] occludees = new AABB[LOD_COUNT][CHUNKS_PER_LOD];
    private final long[][] isMeshed = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];

    private record Vertex(int x, int y, int z, int textureData) {
        public int manhattanDistanceFrom(int cameraX, int cameraY, int cameraZ) {
            return Math.abs(x - cameraX) + Math.abs(y - cameraY) + Math.abs(z - cameraZ);
        }
    }
}
