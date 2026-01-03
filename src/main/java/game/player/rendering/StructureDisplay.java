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

import static org.lwjgl.opengl.GL46.*;

public final class StructureDisplay extends Renderable {

    public StructureDisplay(Vector2f sizeToParent, Vector2f offsetToParent, Structure structure) {
        super(sizeToParent, offsetToParent);
        sizeX = structure.sizeX();
        sizeY = structure.sizeY();
        sizeZ = structure.sizeZ();
        rotation = new Vector3f(35.26439F, -45.0F, 0.0F); // Direction : x == y == z

        Mesh mesh = new MeshGenerator().generateMesh(structure);
        opaqueModel = ObjectLoader.loadOpaqueModel(mesh);
        transparentModel = ObjectLoader.loadTransparentModel(mesh);
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
        glViewport(
                (int) ((position.x + size.x * 0.5F * (1.0F - guiSize)) * Window.getWidth()),
                (int) ((position.y + size.y * 0.5F * (1.0F - guiSize)) * Window.getHeight()),
                (int) (size.x * guiSize * Window.getWidth()),
                (int) (size.y * guiSize * Window.getHeight()));
        glClear(GL_DEPTH_BUFFER_BIT);

        Shader shader = AssetManager.get(Shaders.OPAQUE_GEOMETRY);
        Renderer.setupOpaqueRendering(shader, matrix, 0, 0, 0, 1.0F);
        shader.setUniform("cameraPosition", 0.0F, 0.0F, 0.0F);
        shader.setUniform("flags", 0);
        renderOpaqueModel(opaqueModel, shader);

        shader = AssetManager.get(Shaders.WATER);
        Renderer.setUpWaterRendering(shader, matrix, 0, 0, 0, 1.0F);
        shader.setUniform("flags", 0);
        renderWaterModel(transparentModel, shader);

        shader = AssetManager.get(Shaders.GLASS);
        Renderer.setUpGlassRendering(shader, matrix, 0, 0, 0);
        renderGlassModel(transparentModel, shader);
        glDepthMask(true);

        glViewport(0, 0, Window.getWidth(), Window.getHeight());
    }

    @Override
    public void deleteSelf() {
        if (opaqueModel != null) opaqueModel.delete();
        if (transparentModel != null) transparentModel.delete();
    }

    private static void renderOpaqueModel(OpaqueModel model, Shader shader) {
        if (model.isEmpty()) return;

        shader.setUniform("lodSize", 1);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, model.bufferOrStart());
        glMultiDrawArrays(GL_TRIANGLES, model.indices(), model.vertexCounts());
    }

    private static void renderWaterModel(TransparentModel model, Shader shader) {
        if (model.isWaterEmpty()) return;

        shader.setUniform("cameraPosition", 3000.0F, 4000.0F, 2000.0F);
        shader.setUniform("lodSize", 1);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, model.bufferOrStart());
        glDrawArrays(GL_TRIANGLES, 0, model.waterVertexCount());
    }

    private static void renderGlassModel(TransparentModel model, Shader shader) {
        if (model.isGlassEmpty()) return;

        shader.setUniform("lodSize", 1);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, model.bufferOrStart());
        glDrawArrays(GL_TRIANGLES, model.waterVertexCount(), model.glassVertexCount());
    }

    private final OpaqueModel opaqueModel;
    private final TransparentModel transparentModel;
    private final int sizeX, sizeY, sizeZ;

    private float zoom = 1.0F;
    private final Vector3f rotation;
}
