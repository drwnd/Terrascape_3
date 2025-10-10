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
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;

public final class StructureDisplay extends Renderable {
    public StructureDisplay(Vector2f sizeToParent, Vector2f offsetToParent, Structure structure) {
        super(sizeToParent, offsetToParent);
        sizeX = structure.sizeX();
        sizeY = structure.sizeY();
        sizeZ = structure.sizeZ();

        ArrayList<Mesh> meshes = new MeshGenerator().generateMesh(structure);

        opaqueModels = new OpaqueModel[meshes.size()];
        transparentModels = new TransparentModel[meshes.size()];

        for (int index = 0; index < meshes.size(); index++) {
            Mesh mesh = meshes.get(index);
            opaqueModels[index] = ObjectLoader.loadOpaqueModel(mesh);
            transparentModels[index] = ObjectLoader.loadTransparentModel(mesh);
        }
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0f;
        Matrix4f matrix = Transformation.getStructureDisplayMatrix(sizeX, sizeY, sizeZ);
        GL46.glViewport(
                (int) ((position.x + size.x * 0.5f * (1.0f - guiSize)) * Window.getWidth()),
                (int) ((position.y + size.y * 0.5f * (1.0f - guiSize)) * Window.getHeight()),
                (int) (size.x * guiSize * Window.getWidth()),
                (int) (size.y * guiSize * Window.getHeight()));


        Shader shader = AssetManager.getShader(Shaders.OPAQUE);
        Renderer.setupOpaqueRendering(shader, matrix, 6400, 6400, 6400, 1);
        for (OpaqueModel opaqueModel : opaqueModels) renderOpaqueModel(opaqueModel, shader);


        shader = AssetManager.getShader(Shaders.WATER);
        Renderer.setUpWaterRendering(shader, matrix, 6400, 6400, 6400, 1.0f);
        for (TransparentModel transparentModel : transparentModels) renderWaterModel(transparentModel, shader);


        shader = AssetManager.getShader(Shaders.GLASS);
        Renderer.setUpGlassRendering(shader, matrix, 6400, 6400, 6400);
        for (TransparentModel transparentModel : transparentModels) renderGlassModel(transparentModel, shader);
        GL46.glDepthMask(true);

        GL46.glViewport(0, 0, Window.getWidth(), Window.getHeight());
    }

    @Override
    public void deleteSelf() {
        for (OpaqueModel opaqueModel : opaqueModels) if (opaqueModel != null) opaqueModel.delete();
        for (TransparentModel transparentModel : transparentModels) if (transparentModel != null) transparentModel.delete();
    }

    private void renderOpaqueModel(OpaqueModel opaqueModel, Shader shader) {
        if (!opaqueModel.containsGeometry()) return;
        int[] toRenderVertexCounts = opaqueModel.getVertexCounts(100, 100, 100);

        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, opaqueModel.verticesBuffer());
        shader.setUniform("worldPos", 0, 0, 0, 1);
        GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, opaqueModel.getIndices(), toRenderVertexCounts);
    }

    private void renderWaterModel(TransparentModel transparentModel, Shader shader) {
        if (!transparentModel.containsWater()) return;
        shader.setUniform("cameraPosition", 3000.0f, 4000.0f, 2000.0f);

        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, transparentModel.verticesBuffer());
        shader.setUniform("worldPos", 0, 0, 0, 1);
        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, transparentModel.waterVertexCount() * (6 / 2));
    }

    private void renderGlassModel(TransparentModel transparentModel, Shader shader) {
        if (!transparentModel.containsGlass()) return;
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, transparentModel.verticesBuffer());
        shader.setUniform("worldPos", 0, 0, 0, 1);
        shader.setUniform("indexOffset", transparentModel.waterVertexCount() >> 1);
        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, transparentModel.glassVertexCount() * (6 / 2));
    }

    private final OpaqueModel[] opaqueModels;
    private final TransparentModel[] transparentModels;
    private final int sizeX, sizeY, sizeZ;
}
