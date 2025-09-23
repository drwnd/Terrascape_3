package renderables;

import assets.AssetManager;
import assets.identifiers.ShaderIdentifier;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;
import player.rendering.*;
import rendering_api.Window;
import rendering_api.shaders.Shader;
import server.generation.Structure;
import settings.FloatSetting;
import utils.Transformation;

public final class StructureDisplay extends Renderable {
    public StructureDisplay(Vector2f sizeToParent, Vector2f offsetToParent, Structure structure) {
        super(sizeToParent, offsetToParent);
        sizeX = structure.sizeX();
        sizeY = structure.sizeY();
        sizeZ = structure.sizeZ();

        Mesh mesh = new MeshGenerator().generateMesh(structure);

        opaqueModel = ObjectLoader.loadOpaqueModel(mesh);
        transparentModel = ObjectLoader.loadTransparentModel(mesh);
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        if (!opaqueModel.containsGeometry() && !transparentModel.containsWater() && !transparentModel.containsGlass()) return;

        float guiSize = scalesWithGuiSize() ? FloatSetting.GUI_SIZE.value() : 1.0f;
        Matrix4f matrix = Transformation.getStructureDisplayMatrix(sizeX, sizeY, sizeZ);
        GL46.glViewport(
                (int) ((position.x + size.x * 0.5f * (1.0f - guiSize)) * Window.getWidth()),
                (int) ((position.y + size.y * 0.5f * (1.0f - guiSize)) * Window.getHeight()),
                (int) (size.x * guiSize * Window.getWidth()),
                (int) (size.y * guiSize * Window.getHeight()));

        if (opaqueModel.containsGeometry()) {
            Shader shader = AssetManager.getShader(ShaderIdentifier.OPAQUE);
            Renderer.setupOpaqueRendering(shader, matrix, 0, 0, 0);
            int[] toRenderVertexCounts = opaqueModel.getVertexCounts(1, 1, 1);

            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, opaqueModel.verticesBuffer());
            shader.setUniform("worldPos", 0, 0, 0, 1);
            GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, opaqueModel.getIndices(), toRenderVertexCounts);
        }
        if (transparentModel.containsWater()) {
            Shader shader = AssetManager.getShader(ShaderIdentifier.WATER);
            Renderer.setUpWaterRendering(shader, matrix, 0, 0, 0, 1.0f);
            shader.setUniform("cameraPosition", 300.0f, 400.0f, 200.0f);

            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, transparentModel.verticesBuffer());
            shader.setUniform("worldPos", 0, 0, 0, 1);
            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, transparentModel.waterVertexCount() * (6 / 2));
        }
        GL46.glViewport(0, 0, Window.getWidth(), Window.getHeight());
    }

    @Override
    public void deleteSelf() {
        opaqueModel.delete();
        transparentModel.delete();
    }

    private final OpaqueModel opaqueModel;
    private final TransparentModel transparentModel;
    private final int sizeX, sizeY, sizeZ;
}
