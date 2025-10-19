package game.player.rendering;

import core.assets.AssetManager;
import core.rendering_api.shaders.Shader;
import core.settings.FloatSetting;
import core.settings.ToggleSetting;
import core.renderables.Renderable;
import core.renderables.UiElement;
import core.rendering_api.Window;

import game.assets.Shaders;
import game.assets.TextureArrays;
import game.assets.Textures;
import game.assets.VertexArrays;
import game.player.Player;
import game.server.Game;
import game.server.Server;
import game.utils.Position;
import game.utils.Transformation;
import game.utils.Utils;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;

import java.util.ArrayList;

import static game.utils.Constants.*;

public final class Renderer extends Renderable {

    public int renderedOpaqueModels, renderedWaterModels, renderedGlassModels;

    public Renderer(Player player) {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        this.player = player;
        setAllowFocusScaling(false);
        debugLines = DebugScreenLine.getDebugLines();

        crosshair = new UiElement(new Vector2f(), new Vector2f(), Textures.CROSSHAIR);
        crosshair.setScaleWithGuiSize(false);
        crosshair.setAllowFocusScaling(false);

        addRenderable(crosshair);
        addRenderable(new BreakPlaceOptionsDisplay());
    }

    public void toggleDebugScreen() {
        debugScreenOpen = !debugScreenOpen;
    }

    public ArrayList<Long> getFrameTimes() {
        return frameTimes;
    }

    public float getRenderTime() {
        Server server = Game.getServer();
        float renderTime = server.getDayTime() + FloatSetting.TIME_SPEED.value() * server.getCurrentGameTickFraction();
        if (renderTime > 1.0F) renderTime -= 2.0F;
        return renderTime;
    }


    public static void setupOpaqueRendering(Shader shader, Matrix4f matrix, int x, int y, int z, float time) {
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("propertiesTextures", 1);
        shader.setUniform("nightBrightness", FloatSetting.NIGHT_BRIGHTNESS.value());
        shader.setUniform("time", time);
        shader.setUniform("sunDirection", getSunDirection(time));

        GL46.glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID()); // Just bind something IDK
        GL46.glDepthMask(true);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.MATERIALS).getID());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.PROPERTIES).getID());
    }

    public static void setUpWaterRendering(Shader shader, Matrix4f matrix, int x, int y, int z, float time) {
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("nightBrightness", FloatSetting.NIGHT_BRIGHTNESS.value());
        shader.setUniform("time", time);
        shader.setUniform("sunDirection", getSunDirection(time));

        GL46.glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID()); // Just bind something IDK
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glDepthMask(true);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.MATERIALS).getID());
    }

    public static void setUpGlassRendering(Shader shader, Matrix4f matrix, int x, int y, int z) {
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);
        shader.setUniform("textures", 0);

        GL46.glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID()); // Just bind something IDK
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_ZERO, GL46.GL_SRC_COLOR);
        GL46.glDepthMask(false);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.MATERIALS).getID());
    }


    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        Camera camera = player.getCamera();
        player.updateFrame();
        renderingOptimizer.computeVisibility(player);

        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera);
        Position cameraPosition = player.getCamera().getPosition();

        setupRenderState();
        renderSkybox(camera);
        renderOpaqueGeometry(cameraPosition, projectionViewMatrix);
        renderWater(cameraPosition, projectionViewMatrix);
        renderGlass(cameraPosition, projectionViewMatrix);
        renderDebugInfo();
    }

    @Override
    public void setOnTop() {
        player.setInput();
    }

    @Override
    public void hoverOver(Vector2i pixelCoordinate) {
        if (player.getInventory().isVisible()) player.getInventory().hoverOver(pixelCoordinate);
    }


    private void setupRenderState() {
        long currentTime = System.nanoTime();
        frameTimes.removeIf(frameTime -> currentTime - frameTime > 1_000_000_000L);
        frameTimes.add(currentTime);

        Game.getPlayer().getCamera().updateProjectionMatrix();
        GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, ToggleSetting.X_RAY.value() ? GL46.GL_LINE : GL46.GL_FILL);
        GLFW.glfwSwapInterval(ToggleSetting.V_SYNC.value() ? 1 : 0);

        float crosshairSize = FloatSetting.CROSSHAIR_SIZE.value();
        crosshair.setOffsetToParent(0.5F - crosshairSize * 0.5F, 0.5F - crosshairSize * 0.5F * Window.getAspectRatio());
        crosshair.setSizeToParent(crosshairSize, crosshairSize * Window.getAspectRatio());
    }

    private void renderSkybox(Camera camera) {
        GL46.glDisable(GL46.GL_BLEND);
        Shader shader = AssetManager.get(Shaders.SKYBOX);

        shader.bind();
        shader.setUniform("textureAtlas1", 0);
        shader.setUniform("textureAtlas2", 1);
        shader.setUniform("time", getRenderTime());
        shader.setUniform("projectionViewMatrix", Transformation.createProjectionRotationMatrix(camera));

        GL46.glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID());
        GL46.glEnableVertexAttribArray(0);
        GL46.glEnableVertexAttribArray(1);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.get(Textures.NIGHT_SKY).getID());
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, AssetManager.get(Textures.DAY_SKY).getID());

        GL46.glDepthMask(false);
        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);

        GL46.glDrawElements(GL46.GL_TRIANGLES, SKY_BOX_INDICES.length, GL46.GL_UNSIGNED_INT, 0);

        GL46.glDepthMask(true);
    }

    private void renderOpaqueGeometry(Position playerPosition, Matrix4f projectionViewMatrix) {
        renderedOpaqueModels = 0;

        int playerChunkX = Utils.floor(playerPosition.intX) >> CHUNK_SIZE_BITS;
        int playerChunkY = Utils.floor(playerPosition.intY) >> CHUNK_SIZE_BITS;
        int playerChunkZ = Utils.floor(playerPosition.intZ) >> CHUNK_SIZE_BITS;

        Shader shader = AssetManager.get(Shaders.OPAQUE);
        setupOpaqueRendering(shader, projectionViewMatrix, playerPosition.intX, playerPosition.intY, playerPosition.intZ, getRenderTime());
        shader.setUniform("cameraPosition", playerPosition.getInChunkPosition());
        shader.setUniform("flags", getFlags(playerPosition));

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            long[] lodVisibilityBits = renderingOptimizer.getVisibilityBits()[lod];

            for (OpaqueModel model : player.getMeshCollector().getOpaqueModels(lod)) {
                if (model == null || !model.containsGeometry() || isInvisible(model.chunkX(), model.chunkY(), model.chunkZ(), lodVisibilityBits)) continue;
                int[] toRenderVertexCounts = model.getVertexCounts(playerChunkX, playerChunkY, playerChunkZ);

                GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
                shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());

                GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
                renderedOpaqueModels++;
            }
        }
    }

    private void renderWater(Position playerPosition, Matrix4f projectionViewMatrix) {
        renderedWaterModels = 0;

        Shader shader = AssetManager.get(Shaders.WATER);
        setUpWaterRendering(shader, projectionViewMatrix, playerPosition.intX, playerPosition.intY, playerPosition.intZ, getRenderTime());
        shader.setUniform("cameraPosition", playerPosition.getInChunkPosition());
        shader.setUniform("flags", getFlags(playerPosition));

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            long[] lodVisibilityBits = renderingOptimizer.getVisibilityBits()[lod];

            for (TransparentModel model : player.getMeshCollector().getTransparentModels(lod)) {
                if (model == null || !model.containsWater() || isInvisible(model.chunkX(), model.chunkY(), model.chunkZ(), lodVisibilityBits)) continue;

                GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
                shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());

                GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.waterVertexCount() * (6 / 2));
                renderedWaterModels++;
            }
        }
    }

    private void renderGlass(Position playerPosition, Matrix4f projectionViewMatrix) {
        renderedGlassModels = 0;

        Shader shader = AssetManager.get(Shaders.GLASS);
        setUpGlassRendering(shader, projectionViewMatrix, playerPosition.intX, playerPosition.intY, playerPosition.intZ);

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            long[] lodVisibilityBits = renderingOptimizer.getVisibilityBits()[lod];

            for (TransparentModel model : player.getMeshCollector().getTransparentModels(lod)) {
                if (model == null || !model.containsGlass() || isInvisible(model.chunkX(), model.chunkY(), model.chunkZ(), lodVisibilityBits)) continue;

                GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
                shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());
                shader.setUniform("indexOffset", model.waterVertexCount() >> 1);

                GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.glassVertexCount() * (6 / 2));
                renderedGlassModels++;
            }
        }
    }

    private void renderDebugInfo() {
        int textLine = 0;
        for (DebugScreenLine debugLine : debugLines) if (debugLine.shouldShow(debugScreenOpen)) debugLine.render(++textLine);
    }

    private static boolean isInvisible(int chunkX, int chunkY, int chunkZ, long[] visibilityBits) {
        int index = Utils.getChunkIndex(chunkX, chunkY, chunkZ);
        return (visibilityBits[index >> 6] & 1L << index) == 0;
    }

    private int getFlags(Position playerPosition) {
        boolean headUnderWater = Game.getWorld().getMaterial(playerPosition.intX, playerPosition.intY, playerPosition.intZ, 0) == WATER;
        return headUnderWater ? 1 : 0;
    }

    private static Vector3f getSunDirection(float renderTime) {
        final float downwardsSunPart = FloatSetting.DOWNWARD_SUN_DIRECTION.value();
        final float normalizer = (float) Math.sqrt(1 - downwardsSunPart * downwardsSunPart);

        float alpha = (float) (renderTime * Math.PI);

        return new Vector3f(
                (float) -Math.sin(alpha) * normalizer,
                downwardsSunPart,
                (float) -Math.cos(alpha) * normalizer
        );
    }

    private boolean debugScreenOpen = false;
    private final ArrayList<Long> frameTimes = new ArrayList<>();
    private final ArrayList<DebugScreenLine> debugLines;
    private final UiElement crosshair;
    private final RenderingOptimizer renderingOptimizer = new RenderingOptimizer();
    private final Player player;
}
