package player.rendering;

import assets.AssetManager;
import assets.identifiers.ShaderIdentifier;
import assets.identifiers.TextureIdentifier;
import assets.identifiers.VertexArrayIdentifier;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;
import player.Hotbar;
import player.Player;
import renderables.Renderable;
import renderables.UiElement;
import rendering_api.Window;
import rendering_api.shaders.Shader;
import server.Game;
import settings.FloatSetting;
import settings.ToggleSetting;
import utils.Position;
import utils.Transformation;
import utils.Utils;

import java.util.ArrayList;

import static utils.Constants.*;

public final class Renderer extends Renderable {
    public Renderer() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));
        setAllowFocusScaling(false);
        debugLines = DebugScreenLine.getDebugLines();

        crosshair = new UiElement(new Vector2f(), new Vector2f(), TextureIdentifier.CROSSHAIR);
        crosshair.setScaleWithGuiSize(false);
        crosshair.setAllowFocusScaling(false);

        hotbar = new UiElement(new Vector2f(), new Vector2f(), TextureIdentifier.HOTBAR);
        hotbar.setScaleWithGuiSize(false);
        hotbar.setAllowFocusScaling(false);

        hotBarSelectionIndicator = new UiElement(new Vector2f(), new Vector2f(), TextureIdentifier.HOTBAR_SELECTION_INDICATOR);
        hotBarSelectionIndicator.setScaleWithGuiSize(false);
        hotBarSelectionIndicator.setAllowFocusScaling(false);

        addRenderable(crosshair);
        addRenderable(hotbar);                      // Order important here
        addRenderable(hotBarSelectionIndicator);    // Order important here
    }

    public void toggleDebugScreen() {
        debugScreenOpen = !debugScreenOpen;
    }

    public ArrayList<Long> getFrameTimes() {
        return frameTimes;
    }

    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        Player player = Game.getPlayer();
        Camera camera = player.getCamera();
        player.updateFrame();

        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera);
        Position cameraPosition = player.getCamera().getPosition();

        setupRenderState();
        renderSkybox(camera);
        renderOpaqueGeometry(cameraPosition, projectionViewMatrix, player);
        renderWater(cameraPosition, projectionViewMatrix, player);
        renderDebugInfo();
    }

    private void setupRenderState() {
        Game.getPlayer().getCamera().updateProjectionMatrix();
        GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, ToggleSetting.X_RAY.value() ? GL46.GL_LINE : GL46.GL_FILL);
        GLFW.glfwSwapInterval(ToggleSetting.V_SYNC.value() ? 1 : 0);

        float crosshairSize = FloatSetting.CROSSHAIR_SIZE.value();
        crosshair.setOffsetToParent(new Vector2f(0.5f - crosshairSize * 0.5f, 0.5f - crosshairSize * 0.5f * Window.getAspectRatio()));
        crosshair.setSizeToParent(new Vector2f(crosshairSize, crosshairSize * Window.getAspectRatio()));

        float hotbarSize = FloatSetting.HOTBAR_SIZE.value();
        hotbar.setOffsetToParent(new Vector2f(0.5f - hotbarSize * Hotbar.LENGTH * 0.5f, 0));
        hotbar.setSizeToParent(new Vector2f(hotbarSize * Hotbar.LENGTH, hotbarSize * Window.getAspectRatio()));

        float hotbarIndicatorScaler = FloatSetting.HOTBAR_INDICATOR_SCALER.value();
        float scalingOffset = -(hotbarSize * hotbarIndicatorScaler - hotbarSize) * 0.5f;
        hotBarSelectionIndicator.setOffsetToParent(new Vector2f(
                0.5f + hotbarSize * (Game.getPlayer().getHotbar().getSelectedSlot() - Hotbar.LENGTH * 0.5f) + scalingOffset, scalingOffset * Window.getAspectRatio()));
        hotBarSelectionIndicator.setSizeToParent(new Vector2f(hotbarSize * hotbarIndicatorScaler, hotbarSize * hotbarIndicatorScaler * Window.getAspectRatio()));
    }

    private static void renderSkybox(Camera camera) {
        GL46.glDisable(GL46.GL_BLEND);
        Shader shader = AssetManager.getShader(ShaderIdentifier.SKYBOX);

        shader.bind();
        shader.setUniform("textureAtlas1", 0);
        shader.setUniform("textureAtlas2", 1);
        shader.setUniform("time", 1.0f);
        shader.setUniform("projectionViewMatrix", Transformation.createProjectionRotationMatrix(camera));

        GL46.glBindVertexArray(AssetManager.getVertexArray(VertexArrayIdentifier.SKYBOX).getID());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.getTexture(TextureIdentifier.NIGHT_SKY).getID());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.getTexture(TextureIdentifier.DAY_SKY).getID());

        GL46.glDepthMask(false);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);

        GL46.glDrawElements(GL46.GL_TRIANGLES, SKY_BOX_INDICES.length, GL46.GL_UNSIGNED_INT, 0);

        GL46.glDepthMask(true);
    }

    private static void renderOpaqueGeometry(Position playerPosition, Matrix4f projectionViewMatrix, Player player) {
        int playerChunkX = Utils.floor(playerPosition.intPosition().x) >> CHUNK_SIZE_BITS;
        int playerChunkY = Utils.floor(playerPosition.intPosition().y) >> CHUNK_SIZE_BITS;
        int playerChunkZ = Utils.floor(playerPosition.intPosition().z) >> CHUNK_SIZE_BITS;

        Shader shader = AssetManager.getShader(ShaderIdentifier.OPAQUE);
        shader.bind();
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("iCameraPosition",
                playerPosition.intPosition().x & ~CHUNK_SIZE_MASK,
                playerPosition.intPosition().y & ~CHUNK_SIZE_MASK,
                playerPosition.intPosition().z & ~CHUNK_SIZE_MASK);
        shader.setUniform("textureAtlas", 0);
        shader.setUniform("propertiesTexture", 1);

        GL46.glBindVertexArray(AssetManager.getVertexArray(VertexArrayIdentifier.SKYBOX).getID()); // Just bind something IDK
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.getTexture(TextureIdentifier.MATERIALS).getID());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.getTexture(TextureIdentifier.PROPERTIES).getID());

        for (OpaqueModel model : player.getMeshCollector().getOpaqueModels(0)) {
            if (model == null || !model.containsGeometry()) continue;
            int[] toRenderVertexCounts = model.getVertexCounts(playerChunkX, playerChunkY, playerChunkZ);

            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
            shader.setUniform("worldPos", model.X(), model.Y(), model.Z(), 1 << model.LOD());

            GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
        }
    }

    private static void renderWater(Position playerPosition, Matrix4f projectionViewMatrix, Player player) {
        Shader shader = AssetManager.getShader(ShaderIdentifier.WATER);
        shader.bind();
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("iCameraPosition",
                playerPosition.intPosition().x & ~CHUNK_SIZE_MASK,
                playerPosition.intPosition().y & ~CHUNK_SIZE_MASK,
                playerPosition.intPosition().z & ~CHUNK_SIZE_MASK);
        shader.setUniform("cameraPosition", playerPosition.getInChunkPosition());
        shader.setUniform("textureAtlas", 0);
        shader.setUniform("time", 1.0f);

        GL46.glBindVertexArray(AssetManager.getVertexArray(VertexArrayIdentifier.SKYBOX).getID()); // Just bind something IDK
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.getTexture(TextureIdentifier.MATERIALS).getID());

        for (TransparentModel model : player.getMeshCollector().getTransparentModels(0)) {
            if (model == null || !model.containsWater()) continue;

            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
            shader.setUniform("worldPos", model.X(), model.Y(), model.Z(), 1 << model.LOD());

            GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.waterVertexCount() * (6 / 2));
        }
    }


    private void renderDebugInfo() {
        int textLine = 0;
        for (DebugScreenLine debugLine : debugLines) if (debugLine.shouldShow(debugScreenOpen)) debugLine.render(++textLine);
    }

    private boolean debugScreenOpen = false;
    private final ArrayList<Long> frameTimes = new ArrayList<>();
    private final ArrayList<DebugScreenLine> debugLines;
    private final UiElement crosshair, hotbar, hotBarSelectionIndicator;
}
