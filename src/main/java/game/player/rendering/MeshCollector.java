package game.player.rendering;

import game.server.Game;
import game.utils.Utils;

import org.joml.Vector3i;
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;
import java.util.Arrays;

import static game.utils.Constants.*;

public final class MeshCollector {

    public static final int INDIRECT_COMMAND_SIZE = 16;

    public MeshCollector() {
        opaqueIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, opaqueIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) RenderingOptimizer.CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE * LOD_COUNT * 6, GL46.GL_DYNAMIC_DRAW);

        waterIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, waterIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) RenderingOptimizer.CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE * LOD_COUNT, GL46.GL_DYNAMIC_DRAW);

        glassIndirectBuffer = GL46.glGenBuffers();
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, glassIndirectBuffer);
        GL46.glBufferData(GL46.GL_DRAW_INDIRECT_BUFFER, (long) RenderingOptimizer.CHUNKS_PER_LOD * INDIRECT_COMMAND_SIZE * LOD_COUNT, GL46.GL_DYNAMIC_DRAW);
    }

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
            for (OpaqueModel model : toDeleteOpaqueModels) delete(model);
            toDeleteOpaqueModels.clear();
        }
        synchronized (toDeleteTransparentModels) {
            for (TransparentModel model : toDeleteTransparentModels) delete(model);
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
        GL46.glDeleteBuffers(opaqueIndirectBuffer);
        GL46.glDeleteBuffers(waterIndirectBuffer);
        GL46.glDeleteBuffers(glassIndirectBuffer);
    }

    public void removeAll() {
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            for (OpaqueModel model : opaqueModels[lod]) if (model != null) delete(model);
            for (TransparentModel model : transparentModels[lod]) if (model != null) delete(model);

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

    public boolean isNonEmptyModelPresent(int lodModelX, int lodModelY, int lodModelZ, int lod) {
        OpaqueModel opaqueModel = getOpaqueModel(Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod), lod);
        if (opaqueModel != null && !opaqueModel.isEmpty()) return true;
        TransparentModel transparentModel = getTransparentModel(Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ, lod), lod);
        return transparentModel != null && !transparentModel.isEmpty();
    }


    private void deleteMesh(int chunkIndex, int lod) {
        OpaqueModel opaqueModel = getOpaqueModel(chunkIndex, lod);
        TransparentModel transparentModel = getTransparentModel(chunkIndex, lod);

        setOpaqueModel(null, chunkIndex, lod);
        setTransparentModel(null, chunkIndex, lod);
        setMeshed(false, chunkIndex, lod);

        if (opaqueModel != null) delete(opaqueModel);
        if (transparentModel != null) delete(transparentModel);
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
        int lodOffset = mesh.lod() * RenderingOptimizer.CHUNKS_PER_LOD;
        int start = allocator.memAlloc(mesh.getOpaqueByteSize());
        if (start == -1) {
            GL46.glNamedBufferSubData(opaqueIndirectBuffer, (lodOffset + mesh.index()) * INDIRECT_COMMAND_SIZE * 6L, new int[6 * INDIRECT_COMMAND_SIZE / 4]);
            return new OpaqueModel(mesh.getWorldCoordinate(), null, start, mesh.lod(), false);
        }

        OpaqueModel model = new OpaqueModel(mesh.getWorldCoordinate(), mesh.vertexCounts(), start, mesh.lod(), false);
        int[] indirectCommands = new int[]{
                model.vertexCounts()[NORTH], 1, model.indices()[NORTH], 0,
                model.vertexCounts()[TOP], 1, model.indices()[TOP], 0,
                model.vertexCounts()[WEST], 1, model.indices()[WEST], 0,
                model.vertexCounts()[SOUTH], 1, model.indices()[SOUTH], 0,
                model.vertexCounts()[BOTTOM], 1, model.indices()[BOTTOM], 0,
                model.vertexCounts()[EAST], 1, model.indices()[EAST], 0
        };
        GL46.glNamedBufferSubData(opaqueIndirectBuffer, (lodOffset + mesh.index()) * INDIRECT_COMMAND_SIZE * 6L, indirectCommands);
        GL46.glNamedBufferSubData(allocator.getBuffer(), start, mesh.opaqueVertices());
        return model;
    }

    private TransparentModel loadTransparentModel(Mesh mesh) {
        int lodOffset = mesh.lod() * RenderingOptimizer.CHUNKS_PER_LOD;
        int start = allocator.memAlloc(mesh.getTransparentByteSize());
        if (start == -1) {
            GL46.glNamedBufferSubData(waterIndirectBuffer, (long) (lodOffset + mesh.index()) * INDIRECT_COMMAND_SIZE, new int[INDIRECT_COMMAND_SIZE / 4]);
            GL46.glNamedBufferSubData(glassIndirectBuffer, (long) (lodOffset + mesh.index()) * INDIRECT_COMMAND_SIZE, new int[INDIRECT_COMMAND_SIZE / 4]);
            return new TransparentModel(mesh.getWorldCoordinate(), mesh.waterVertexCount(), mesh.glassVertexCount(), start, mesh.lod());
        }

        TransparentModel model = new TransparentModel(mesh.getWorldCoordinate(), mesh.waterVertexCount(), mesh.glassVertexCount(), start, mesh.lod());
        int[] waterIndirectCommand = new int[]{model.waterVertexCount(), 1, model.waterIndex(), 0};
        int[] glassIndirectCommand = new int[]{model.glassVertexCount(), 1, model.glassIndex(), 0};

        GL46.glNamedBufferSubData(waterIndirectBuffer, (long) (lodOffset + mesh.index()) * INDIRECT_COMMAND_SIZE, waterIndirectCommand);
        GL46.glNamedBufferSubData(glassIndirectBuffer, (long) (lodOffset + mesh.index()) * INDIRECT_COMMAND_SIZE, glassIndirectCommand);
        GL46.glNamedBufferSubData(allocator.getBuffer(), start, mesh.transparentVertices());
        return model;
    }

    private void delete(OpaqueModel model) {
        allocator.memFree(model.bufferOrStart());
        int lodOffset = model.LOD() * RenderingOptimizer.CHUNKS_PER_LOD;
        GL46.glNamedBufferSubData(opaqueIndirectBuffer, (lodOffset + model.chunkIndex()) * INDIRECT_COMMAND_SIZE * 6L, new int[6 * INDIRECT_COMMAND_SIZE / 4]);
    }

    private void delete(TransparentModel model) {
        allocator.memFree(model.bufferOrStart());
        int lodOffset = model.LOD() * RenderingOptimizer.CHUNKS_PER_LOD;
        GL46.glNamedBufferSubData(waterIndirectBuffer, (long) (lodOffset + model.chunkIndex()) * INDIRECT_COMMAND_SIZE, new int[INDIRECT_COMMAND_SIZE / 4]);
        GL46.glNamedBufferSubData(glassIndirectBuffer, (long) (lodOffset + model.chunkIndex()) * INDIRECT_COMMAND_SIZE, new int[INDIRECT_COMMAND_SIZE / 4]);
    }

    private final MemoryAllocator allocator = new MemoryAllocator(500_000_000);
    private final ArrayList<Mesh> meshQueue = new ArrayList<>();
    private final ArrayList<OpaqueModel> toDeleteOpaqueModels = new ArrayList<>();
    private final ArrayList<TransparentModel> toDeleteTransparentModels = new ArrayList<>();

    private final OpaqueModel[][] opaqueModels = new OpaqueModel[LOD_COUNT][RenderingOptimizer.CHUNKS_PER_LOD];
    private final TransparentModel[][] transparentModels = new TransparentModel[LOD_COUNT][RenderingOptimizer.CHUNKS_PER_LOD];
    private final long[][] isMeshed = new long[LOD_COUNT][RenderingOptimizer.CHUNKS_PER_LOD / 64];

    public final int opaqueIndirectBuffer, waterIndirectBuffer, glassIndirectBuffer;
}
