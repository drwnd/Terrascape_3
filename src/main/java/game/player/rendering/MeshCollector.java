package game.player.rendering;

import game.server.Game;
import game.utils.Utils;

import org.joml.Vector3i;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class MeshCollector {

    public void uploadAllMeshes() {
        Vector3i playerChunkCoordinate = Game.getPlayer().getPosition().getChunkCoordinate();
        synchronized (meshQueue) {
            for (Mesh mesh : meshQueue) {
                int playerChunkX = playerChunkCoordinate.x >> mesh.lod();
                int playerChunkY = playerChunkCoordinate.y >> mesh.lod();
                int playerChunkZ = playerChunkCoordinate.z >> mesh.lod();
                if (Utils.outsideRenderKeepDistance(playerChunkX, playerChunkY, playerChunkZ, mesh.chunkX(), mesh.chunkY(), mesh.chunkZ()))
                    continue;

                upload(mesh);
            }
            meshQueue.clear();
        }
    }

    public void deleteOldMeshes() {
        synchronized (toDeleteOpaqueModels) {
            for (OpaqueModel opaqueModel : toDeleteOpaqueModels) opaqueModel.delete();
            toDeleteOpaqueModels.clear();
        }
        synchronized (toDeleteTransparentModels) {
            for (TransparentModel transparentModel : toDeleteTransparentModels) transparentModel.delete();
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

    public OpaqueModel[] getOpaqueModels(int lod) {
        return opaqueModels[lod];
    }

    public TransparentModel[] getTransparentModels(int lod) {
        return transparentModels[lod];
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
        return getOpaqueModel(Utils.getChunkIndex(lodModelX, lodModelY, lodModelZ), lod) != null;
    }


    private void deleteMesh(int chunkIndex, int lod) {
        OpaqueModel opaqueModel = getOpaqueModel(chunkIndex, lod);
        TransparentModel transparentModel = getTransparentModel(chunkIndex, lod);

        setOpaqueModel(null, chunkIndex, lod);
        setTransparentModel(null, chunkIndex, lod);
        setMeshed(false, chunkIndex, lod);

        if (opaqueModel != null) opaqueModel.delete();
        if (transparentModel != null) transparentModel.delete();
    }

    private void setOpaqueModel(OpaqueModel opaqueModel, int index, int lod) {
        opaqueModels[lod][index] = opaqueModel;
    }

    private void setTransparentModel(TransparentModel transparentModel, int index, int lod) {
        transparentModels[lod][index] = transparentModel;
    }

    private void upload(Mesh mesh) {
        int chunkIndex = Utils.getChunkIndex(mesh.chunkX(), mesh.chunkY(), mesh.chunkZ());
        deleteMesh(chunkIndex, mesh.lod());

        OpaqueModel opaqueModel = ObjectLoader.loadOpaqueModel(mesh);
        setOpaqueModel(opaqueModel, chunkIndex, mesh.lod());

        TransparentModel transparentModel = ObjectLoader.loadTransparentModel(mesh);
        setTransparentModel(transparentModel, chunkIndex, mesh.lod());
        setMeshed(true, chunkIndex, mesh.lod());
    }

    private final ArrayList<Mesh> meshQueue = new ArrayList<>();
    private final ArrayList<OpaqueModel> toDeleteOpaqueModels = new ArrayList<>();

    private final ArrayList<TransparentModel> toDeleteTransparentModels = new ArrayList<>();
    private final OpaqueModel[][] opaqueModels = new OpaqueModel[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];
    private final TransparentModel[][] transparentModels = new TransparentModel[LOD_COUNT][RENDERED_WORLD_WIDTH * RENDERED_WORLD_HEIGHT * RENDERED_WORLD_WIDTH];
    private final long[][] isMeshed = new long[LOD_COUNT][opaqueModels[0].length / 64 + 1];
}
