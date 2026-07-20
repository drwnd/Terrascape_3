package game.player.rendering;

import core.utils.Vector3l;

import game.server.Game;
import game.settings.IntSettings;
import game.utils.Position;
import game.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;

public final class MeshCollector {

    public MeshCollector() {
        allocator = new MemoryAllocator(1 << 29);
    }

/**
 * Creates a new MeshCollector instance.
 *
 * @param oldMeshCollector parameter
 * @param oldRenderDistance parameter
 */
    public MeshCollector(MeshCollector oldMeshCollector, int oldRenderDistance) {
        oldMeshCollector.deleteOldMeshes();
        oldMeshCollector.uploadAllMeshes();
        allocator = oldMeshCollector.allocator;

        Position playerPosition = Game.getPlayer().getPosition();
        int renderDistance = Math.min(oldRenderDistance, IntSettings.RENDER_DISTANCE.value());

        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            long cameraX = playerPosition.longX >>> CHUNK_SIZE_BITS + lod;
            long cameraY = playerPosition.longY >>> CHUNK_SIZE_BITS + lod;
            long cameraZ = playerPosition.longZ >>> CHUNK_SIZE_BITS + lod;

            for (OpaqueModel model : oldMeshCollector.opaqueModels[lod]) {
                if (model == null) continue;
                if (Utils.outsideChunkKeepDistance(cameraX, cameraY, cameraZ, model.chunkX(), model.chunkY(), model.chunkZ(), lod)) {
                    synchronized (toDeleteOpaqueModels) {
                        toDeleteOpaqueModels.add(model);
                    }
                    continue;
                }
                int index = Utils.getChunkIndex(model.chunkX(), model.chunkY(), model.chunkZ(), lod);
                opaqueModels[lod][index] = model;
            }
            for (TransparentModel model : oldMeshCollector.transparentModels[lod]) {
                if (model == null) continue;
                if (Utils.outsideChunkKeepDistance(cameraX, cameraY, cameraZ, model.chunkX(), model.chunkY(), model.chunkZ(), lod)) {
                    synchronized (toDeleteTransparentModels) {
                        toDeleteTransparentModels.add(model);
                    }
                    continue;
                }
                int index = Utils.getChunkIndex(model.chunkX(), model.chunkY(), model.chunkZ(), lod);
                transparentModels[lod][index] = model;
            }

            for (long chunkX = cameraX - renderDistance - 1; chunkX != cameraX + renderDistance + 2; chunkX++)
                for (long chunkY = cameraY - renderDistance - 1; chunkY != cameraY + renderDistance + 2; chunkY++)
                    for (long chunkZ = cameraZ - renderDistance - 1; chunkZ != cameraZ + renderDistance + 2; chunkZ++) {

                        int oldIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod, oldRenderDistance);
                        int currentIndex = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);

                        if (oldMeshCollector.isMeshed(oldIndex, lod)) setMeshed(true, currentIndex, lod);

                        AABB occluder = oldMeshCollector.getOccluder(oldIndex, lod);
                        AABB occludee = oldMeshCollector.getOccludee(oldIndex, lod);

                        if (occluder != null) occluders[lod][currentIndex] = occluder;
                        if (occludee != null) occludees[lod][currentIndex] = occludee;
                    }
        }
    }

/**
 * Creates a new MeshCollector instance.
 *
 * @param oldMeshCollector parameter
 */
    public MeshCollector(MeshCollector oldMeshCollector) {
        oldMeshCollector.deleteOldMeshes();
        oldMeshCollector.uploadAllMeshes();
        allocator = oldMeshCollector.allocator;
        int lodCount = Math.min(oldMeshCollector.isMeshed.length, isMeshed.length);

        System.arraycopy(oldMeshCollector.isMeshed, 0, isMeshed, 0, lodCount);
        System.arraycopy(oldMeshCollector.opaqueModels, 0, opaqueModels, 0, lodCount);
        System.arraycopy(oldMeshCollector.transparentModels, 0, transparentModels, 0, lodCount);
        System.arraycopy(oldMeshCollector.occluders, 0, occluders, 0, lodCount);
        System.arraycopy(oldMeshCollector.occludees, 0, occludees, 0, lodCount);

        for (int lod = lodCount; lod < oldMeshCollector.opaqueModels.length; lod++) {
            for (OpaqueModel model : oldMeshCollector.opaqueModels[lod]) if (model != null) allocator.memFree(model.bufferOrStart());
            for (TransparentModel model : oldMeshCollector.transparentModels[lod]) if (model != null) allocator.memFree(model.bufferOrStart());
        }
    }

/**
 * Performs upload all meshes.
 */
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

/**
 * Performs delete old meshes.
 */
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

/**
 * Performs queue mesh.
 *
 * @param mesh parameter
 */
    public void queueMesh(Mesh mesh) {
        synchronized (meshQueue) {
            meshQueue.add(mesh);
        }
    }

    public boolean isMeshed(int chunkIndex, int lod) {
        return (isMeshed[lod][chunkIndex >> 6] & 1L << chunkIndex) != 0;
    }

/**
 * Sets meshed.
 *
 * @param meshed parameter
 * @param chunkIndex X coordinate in local block coordinates
 * @param lod parameter
 */
    public void setMeshed(boolean meshed, int chunkIndex, int lod) {
        if (meshed) isMeshed[lod][chunkIndex >> 6] |= 1L << chunkIndex;
        else isMeshed[lod][chunkIndex >> 6] &= ~(1L << chunkIndex);
    }

/**
 * Removes mesh.
 *
 * @param chunkIndex X coordinate in local block coordinates
 * @param lod parameter
 */
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

/**
 * Removes all.
 */
    public void removeAll() {
        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            for (OpaqueModel model : opaqueModels[lod]) if (model != null) allocator.memFree(model.bufferOrStart());
            for (TransparentModel model : transparentModels[lod]) if (model != null) allocator.memFree(model.bufferOrStart());

            Arrays.fill(opaqueModels[lod], null);
            Arrays.fill(transparentModels[lod], null);

            Arrays.fill(isMeshed[lod], 0L);
        }
    }

/**
 * Checks whether isolated.
 *
 * @param chunkX X coordinate in local block coordinates
 * @param chunkY Y coordinate in local block coordinates
 * @param chunkZ Z coordinate in local block coordinates
 * @param lod parameter
 * @return true if the condition holds
 */
    public boolean isIsolated(long chunkX, long chunkY, long chunkZ, int lod) {
        OpaqueModel model;
        return ((model = getOpaqueModel(Utils.getChunkIndex(chunkX - 1, chunkY, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX + 1, chunkY, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY - 1, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY + 1, chunkZ, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY, chunkZ - 1, lod), lod)) == null || model.isEmpty())
                && ((model = getOpaqueModel(Utils.getChunkIndex(chunkX, chunkY, chunkZ + 1, lod), lod)) == null || model.isEmpty());
    }


/**
 * Performs delete mesh.
 *
 * @param chunkIndex X coordinate in local block coordinates
 * @param lod parameter
 */
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

/**
 * Performs upload.
 *
 * @param mesh parameter
 */
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

/**
 * Performs load opaque model.
 *
 * @param mesh parameter
 * @return result
 */
    private OpaqueModel loadOpaqueModel(Mesh mesh) {
        int start = allocator.memAlloc(mesh.getOpaqueByteSize());
        if (start == -1) return new OpaqueModel(mesh.getWorldCoordinate(), null, -1, mesh.lod(), false);

        glNamedBufferSubData(allocator.getBuffer(), start, mesh.opaqueVertices());
        return new OpaqueModel(mesh.getWorldCoordinate(), mesh.vertexCounts(), start, mesh.lod(), false);
    }

/**
 * Performs load transparent model.
 *
 * @param mesh parameter
 * @return result
 */
    private TransparentModel loadTransparentModel(Mesh mesh) {
        int start = allocator.memAlloc(mesh.getTransparentByteSize());
        if (start == -1) return new TransparentModel(mesh.getWorldCoordinate(), 0, 0, -1, mesh.lod());

        glNamedBufferSubData(allocator.getBuffer(), start, mesh.transparentVertices());
        return new TransparentModel(mesh.getWorldCoordinate(), mesh.transparentVertexCount(), mesh.glassVertexCount(), start, mesh.lod());
    }

    private final MemoryAllocator allocator;
    private final ArrayList<Mesh> meshQueue = new ArrayList<>();
    private final ArrayList<OpaqueModel> toDeleteOpaqueModels = new ArrayList<>();
    private final ArrayList<TransparentModel> toDeleteTransparentModels = new ArrayList<>();

    private final OpaqueModel[][] opaqueModels = new OpaqueModel[Game.getWorld().LOD_COUNT][Game.getWorld().CHUNKS_PER_LOD];
    private final TransparentModel[][] transparentModels = new TransparentModel[Game.getWorld().LOD_COUNT][Game.getWorld().CHUNKS_PER_LOD];
    private final AABB[][] occluders = new AABB[Game.getWorld().LOD_COUNT][Game.getWorld().CHUNKS_PER_LOD];
    private final AABB[][] occludees = new AABB[Game.getWorld().LOD_COUNT][Game.getWorld().CHUNKS_PER_LOD];
    private final long[][] isMeshed = new long[Game.getWorld().LOD_COUNT][Game.getWorld().CHUNKS_PER_LOD / 64];
}
