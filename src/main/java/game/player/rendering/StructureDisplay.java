package game.player.rendering;

import core.assets.AssetManager;
import core.renderables.Renderable;
import core.rendering_api.Window;
import core.rendering_api.shaders.Shader;
import core.settings.FloatSetting;

import game.assets.Shaders;
import game.server.generation.Structure;
import game.utils.Transformation;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;

public final class StructureDisplay extends Renderable {
    public StructureDisplay(Vector2f sizeToParent, Vector2f offsetToParent, Structure structure) {
        super(sizeToParent, offsetToParent);
        sizeX = structure.sizeX();
        sizeY = structure.sizeY();
        sizeZ = structure.sizeZ();
        rotation = new Vector3f(35.26439F, -45.0F, 0.0F); // Direction : x == y == z

        ArrayList<Mesh> meshes = new MeshGenerator().generateMesh(structure);

        opaqueModels = new OpaqueModel[meshes.size()];
        transparentModels = new TransparentModel[meshes.size()];

        for (int index = 0; index < meshes.size(); index++) {
            Mesh mesh = meshes.get(index);
            opaqueModels[index] = ObjectLoader.loadOpaqueModel(mesh);
            transparentModels[index] = ObjectLoader.loadTransparentModel(mesh);
        }
    }

    public void changeZoom(float factor) {
        zoom *= factor;
    }

    public void rotate(Vector2i cursorMovement) {
        float sensitivityFactor = FloatSetting.SENSITIVITY.value() * 0.6F + 0.2F;
        sensitivityFactor = 5.0F * sensitivityFactor * sensitivityFactor * sensitivityFactor;
        float rotationYaw = cursorMovement.x * sensitivityFactor;
        float rotationPitch = cursorMovement.y * sensitivityFactor;

        rotation.x -= rotationPitch;
        rotation.y += rotationYaw;

        rotation.x = Math.max(-89.9F, Math.min(rotation.x, 89.9F)); // Looking directly up or down breaks the lookAt transform
        rotation.y %= 360.0F;
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0F;
        Matrix4f matrix = Transformation.getStructureDisplayMatrix(sizeX, sizeY, sizeZ, zoom, rotation);
        GL46.glViewport(
                (int) ((position.x + size.x * 0.5F * (1.0F - guiSize)) * Window.getWidth()),
                (int) ((position.y + size.y * 0.5F * (1.0F - guiSize)) * Window.getHeight()),
                (int) (size.x * guiSize * Window.getWidth()),
                (int) (size.y * guiSize * Window.getHeight()));
        GL46.glClear(GL46.GL_DEPTH_BUFFER_BIT);

        Shader shader = AssetManager.get(Shaders.OPAQUE);
        Renderer.setupOpaqueRendering(shader, matrix, 0, 0, 0, 1.0F);
        for (OpaqueModel opaqueModel : opaqueModels) renderOpaqueModel(opaqueModel, shader);

        shader = AssetManager.get(Shaders.WATER);
        Renderer.setUpWaterRendering(shader, matrix, 0, 0, 0, 1.0F);
        for (TransparentModel transparentModel : transparentModels) renderWaterModel(transparentModel, shader);

        shader = AssetManager.get(Shaders.GLASS);
        Renderer.setUpGlassRendering(shader, matrix, 0, 0, 0);
        for (TransparentModel transparentModel : transparentModels) renderGlassModel(transparentModel, shader);
        GL46.glDepthMask(true);

        GL46.glViewport(0, 0, Window.getWidth(), Window.getHeight());
    }

    @Override
    public void deleteSelf() {
        for (OpaqueModel opaqueModel : opaqueModels) if (opaqueModel != null) opaqueModel.delete();
        for (TransparentModel transparentModel : transparentModels) if (transparentModel != null) transparentModel.delete();
    }

    private void renderOpaqueModel(OpaqueModel model, Shader shader) {
        if (!model.containsGeometry()) return;

        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
        shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());
        GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), model.vertexCounts());
    }

    private void renderWaterModel(TransparentModel model, Shader shader) {
        if (!model.containsWater()) return;

        shader.setUniform("cameraPosition", 3000.0F, 4000.0F, 2000.0F);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
        shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());
        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.waterVertexCount() * (6 / 2));
    }

    private void renderGlassModel(TransparentModel model, Shader shader) {
        if (!model.containsGlass()) return;
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
        shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());
        shader.setUniform("indexOffset", model.waterVertexCount() >> 1);
        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.glassVertexCount() * (6 / 2));
    }

    private final OpaqueModel[] opaqueModels;
    private final TransparentModel[] transparentModels;
    private final int sizeX, sizeY, sizeZ;

    private float zoom = 1.0F;
    private final Vector3f rotation;
}
