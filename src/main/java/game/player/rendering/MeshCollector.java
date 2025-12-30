package game.player.rendering;

import game.server.Game;
import game.utils.Utils;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;
import java.util.Arrays;

import static game.utils.Constants.*;

public final class MeshCollector {

    public void uploadAllMeshes() {
        Vector3i playerChunkCoordinate = Game.getPlayer().getPosition().getChunkCoordinate();
        synchronized (meshQueue) {
            for (Mesh mesh : meshQueue) {
                int playerChunkX = playerChunkCoordinate.x >> mesh.lod();
                int playerChunkY = playerChunkCoordinate.y >> mesh.lod();
                int playerChunkZ = playerChunkCoordinate.z >> mesh.lod();
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

    public boolean isModelPresent(int lodModelX, int lodModelY, int lodModelZ, int lod) {
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

    public boolean neighborHasModel(int chunkX, int chunkY, int chunkZ) {
        OpaqueModel model;
        return (model = getOpaqueModel(Utils.getChunkIndex(chunkX - 1, chunkY, chunkZ, 0), 0)) != null && !model.isEmpty()
                || (model = getOpaqueModel(Utils.getChunkIndex(chunkX + 1, chunkY, chunkZ, 0), 0)) != null && !model.isEmpty()
                || (model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY - 1, chunkZ, 0), 0)) != null && !model.isEmpty()
                || (model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY + 1, chunkZ, 0), 0)) != null && !model.isEmpty()
                || (model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY, chunkZ - 1, 0), 0)) != null && !model.isEmpty()
                || (model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY, chunkZ + 1, 0), 0)) != null && !model.isEmpty();
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
    }

    private OpaqueModel loadOpaqueModel(Mesh mesh) {
        int start = allocator.memAlloc(mesh.getOpaqueByteSize());
        if (start == -1) return new OpaqueModel(mesh.getWorldCoordinate(), null, start, mesh.lod(), false);

        GL46.glNamedBufferSubData(allocator.getBuffer(), start, mesh.opaqueVertices());
        return new OpaqueModel(mesh.getWorldCoordinate(), mesh.vertexCounts(), start, mesh.lod(), false);
    }

    private TransparentModel loadTransparentModel(Mesh mesh) {
        int start = allocator.memAlloc(mesh.getTransparentByteSize());
        if (start == -1) return new TransparentModel(mesh.getWorldCoordinate(), mesh.waterVertexCount(), mesh.glassVertexCount(), start, mesh.lod());

        GL46.glNamedBufferSubData(allocator.getBuffer(), start, mesh.transparentVertices());
        return new TransparentModel(mesh.getWorldCoordinate(), mesh.waterVertexCount(), mesh.glassVertexCount(), start, mesh.lod());
    }

    private final MemoryAllocator allocator = new MemoryAllocator(500_000_000);
    private final ArrayList<Mesh> meshQueue = new ArrayList<>();
    private final ArrayList<OpaqueModel> toDeleteOpaqueModels = new ArrayList<>();
    private final ArrayList<TransparentModel> toDeleteTransparentModels = new ArrayList<>();

    private final OpaqueModel[][] opaqueModels = new OpaqueModel[LOD_COUNT][CHUNKS_PER_LOD];
    private final TransparentModel[][] transparentModels = new TransparentModel[LOD_COUNT][CHUNKS_PER_LOD];
    private final long[][] isMeshed = new long[LOD_COUNT][CHUNKS_PER_LOD / 64];
}
