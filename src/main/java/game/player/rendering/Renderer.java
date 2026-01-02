package game.player.rendering;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.assets.Texture;
import core.renderables.Renderable;
import core.renderables.UiElement;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.Shader;
import core.rendering_api.shaders.TextShader;
import core.settings.FloatSetting;
import core.settings.KeySetting;
import core.settings.OptionSetting;
import core.settings.ToggleSetting;
import core.settings.optionSettings.FontOption;

import game.assets.Shaders;
import game.assets.TextureArrays;
import game.assets.Textures;
import game.assets.VertexArrays;
import game.player.ChatTextField;
import game.player.Player;
import game.player.interaction.CubePlaceable;
import game.player.interaction.CuboidPlaceable;
import game.player.interaction.Placeable;
import game.player.interaction.Target;
import game.server.ChatMessage;
import game.server.Chunk;
import game.server.Game;
import game.server.Server;
import game.utils.Position;
import game.utils.Transformation;
import game.utils.Utils;

import org.joml.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;

import java.awt.*;
import java.lang.Math;
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

        createTextures(Window.getWidth(), Window.getHeight());
        createFrameBuffers();
    }

    public void toggleDebugScreen() {
        debugScreenOpen = !debugScreenOpen;
    }

    public ArrayList<Long> getFrameTimes() {
        return frameTimes;
    }

    public static float getRenderTime() {
        Server server = Game.getServer();
        float renderTime = server.getDayTime() + FloatSetting.TIME_SPEED.value() * server.getCurrentGameTickFraction();
        if (renderTime > 1.0F) renderTime -= 2.0F;
        return renderTime;
    }

    public void updateGameTick() {
        messages = Game.getServer().getMessages();
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

        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_CULL_FACE);
        GL46.glEnable(GL46.GL_STENCIL_TEST);
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glDepthMask(true);
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

        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_STENCIL_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
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

        GL46.glEnable(GL46.GL_DEPTH_TEST);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glEnable(GL46.GL_STENCIL_TEST);
        GL46.glBlendFunc(GL46.GL_ZERO, GL46.GL_SRC_COLOR);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDepthMask(false);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.MATERIALS).getID());
    }


    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        Camera camera = player.getCamera();
        player.updateFrame();
        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera);
        Position cameraPosition = player.getCamera().getPosition();
        if (!Input.isKeyPressed(KeySetting.SKIP_COMPUTING_VISIBILITY)) renderingOptimizer.computeVisibility(player, cameraPosition, projectionViewMatrix);

        setupRenderState();

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer);
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT | GL46.GL_STENCIL_BUFFER_BIT);

        renderSkybox(camera);
        renderOpaqueGeometry(cameraPosition, projectionViewMatrix);
        renderOpaqueParticles(cameraPosition, projectionViewMatrix);

        if (ToggleSetting.USE_AMBIENT_OCCLUSION.value()) {
            GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFramebuffer);
            GL46.glClear(GL46.GL_COLOR_BUFFER_BIT);
            GL46.glDisable(GL46.GL_STENCIL_TEST);

            computeAmbientOcclusion();

            GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer);

            applyAmbientOcclusion();
        }

        renderWater(cameraPosition, projectionViewMatrix);
        renderGlass(cameraPosition, projectionViewMatrix);
        renderTransparentParticles(cameraPosition, projectionViewMatrix);
        renderVolumeIndicator(cameraPosition, projectionViewMatrix);

        GL46.glDisable(GL46.GL_STENCIL_TEST);
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0);
        GL46.glBlitNamedFramebuffer(framebuffer, 0,
                0, 0, Window.getWidth(), Window.getHeight(),
                0, 0, Window.getWidth(), Window.getHeight(),
                GL46.GL_COLOR_BUFFER_BIT, GL46.GL_NEAREST);
        GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_FILL);

        renderOccluders(cameraPosition, projectionViewMatrix);
        renderOccludees(cameraPosition, projectionViewMatrix);
        renderOccluderDepthMap();

        renderChat();
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

    @Override
    protected void resizeSelfTo(int width, int height) {
        if (width == 0 || height == 0) return;

        deleteFrameBuffers();
        deleteTextures();

        createTextures(width, height);
        createFrameBuffers();
    }

    @Override
    public void deleteSelf() {
        deleteFrameBuffers();
        deleteTextures();
        renderingOptimizer.cleanUp();
    }


    private void createTextures(int width, int height) {
        colorTexture = ObjectLoader.createTexture2D(GL46.GL_RGBA8, width, height, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, GL46.GL_NEAREST);
        sideTexture = ObjectLoader.createTexture2D(GL46.GL_R8I, width, height, GL46.GL_RED_INTEGER, GL46.GL_UNSIGNED_BYTE, GL46.GL_NEAREST);

        ssaoTexture = ObjectLoader.createTexture2D(GL46.GL_RED, width, height, GL46.GL_RED, GL46.GL_FLOAT, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);

        depthTexture = ObjectLoader.createTexture2D(GL46.GL_DEPTH32F_STENCIL8, width, height, GL46.GL_DEPTH_STENCIL, GL46.GL_FLOAT_32_UNSIGNED_INT_24_8_REV, GL46.GL_LINEAR);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_BORDER);
        GL46.glTexParameterfv(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        int noiseSamples = 4 * 4 * 3;
        float[] noise = new float[noiseSamples];
        for (int i = 0; i < noiseSamples; i += 3) {
            noise[i] = (float) (Math.random() * 2 - 1);
            noise[i + 1] = (float) (Math.random() * 2 - 1);
            noise[i + 2] = 0.0F;
        }
        noiseTexture = GL46.glCreateTextures(GL46.GL_TEXTURE_2D);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, noiseTexture);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGB, 4, 4, 0, GL46.GL_RGB, GL46.GL_FLOAT, noise);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_NEAREST);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_REPEAT);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_REPEAT);
    }

    private void createFrameBuffers() {
        framebuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, colorTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT1, GL46.GL_TEXTURE_2D, sideTexture, 0);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_TEXTURE_2D, depthTexture, 0);
        GL46.glDrawBuffers(new int[]{GL46.GL_COLOR_ATTACHMENT0, GL46.GL_COLOR_ATTACHMENT1});
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));

        ssaoFramebuffer = GL46.glCreateFramebuffers();
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFramebuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, ssaoTexture, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("SSAO Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0);
    }

    private void deleteTextures() {
        GL46.glDeleteTextures(colorTexture);
        GL46.glDeleteTextures(depthTexture);
        GL46.glDeleteTextures(noiseTexture);
        GL46.glDeleteTextures(ssaoTexture);
        GL46.glDeleteTextures(sideTexture);
    }

    private void deleteFrameBuffers() {
        GL46.glDeleteFramebuffers(framebuffer);
        GL46.glDeleteFramebuffers(ssaoFramebuffer);
    }

    private void setupRenderState() {
        long currentTime = System.nanoTime();
        frameTimes.removeIf(frameTime -> currentTime - frameTime > 1_000_000_000L);
        frameTimes.add(currentTime);

        Game.getPlayer().getCamera().updateProjectionMatrix();

        GL46.glStencilFunc(GL46.GL_ALWAYS, 0, 0xFF);
        GL46.glStencilOp(GL46.GL_KEEP, GL46.GL_KEEP, GL46.GL_REPLACE);
        GL46.glDisable(GL46.GL_STENCIL_TEST);
        GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, ToggleSetting.X_RAY.value() ? GL46.GL_LINE : GL46.GL_FILL);
        if (vSync != ToggleSetting.V_SYNC.value()) {
            vSync = ToggleSetting.V_SYNC.value();
            GLFW.glfwSwapInterval(vSync ? 1 : 0);
        }

        float crosshairSize = FloatSetting.CROSSHAIR_SIZE.value();
        crosshair.setOffsetToParent(0.5F - crosshairSize * 0.5F, 0.5F - crosshairSize * 0.5F * Window.getAspectRatio());
        crosshair.setSizeToParent(crosshairSize, crosshairSize * Window.getAspectRatio());
    }

    private static void renderSkybox(Camera camera) {
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
        GL46.glDisable(GL46.GL_BLEND);

        GL46.glDrawElements(GL46.GL_TRIANGLES, SKY_BOX_INDICES.length, GL46.GL_UNSIGNED_INT, 0);

        GL46.glDepthMask(true);
    }

    private void renderOpaqueGeometry(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedOpaqueModels = 0;

        Shader shader = AssetManager.get(Shaders.OPAQUE_GEOMETRY);
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, getRenderTime());
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        shader.setUniform("flags", getFlags(cameraPosition));
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getOpaqueIndirectBuffer());

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            GL46.glStencilFunc(GL46.GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);

            long start = renderingOptimizer.getOpaqueLodStart(lod);
            int drawCount = renderingOptimizer.getOpaqueLodDrawCount(lod);
            renderedOpaqueModels += drawCount / 6;

            GL46.glMultiDrawArraysIndirect(GL46.GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    private void renderOpaqueParticles(Position cameraPosition, Matrix4f projectionViewMatrix) {
        Shader shader = AssetManager.get(Shaders.OPAQUE_PARTICLE);
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, getRenderTime());
        GL46.glDisable(GL46.GL_STENCIL_TEST);
        long currentTick = Game.getServer().getCurrentGameTick();
        shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());

        for (ParticleEffect particleEffect : player.getParticleCollector().getParticleEffects()) {
            if (!particleEffect.isOpaque()) continue;
            shader.setUniform("lifeTimeTicks", particleEffect.lifeTimeTicks());
            shader.setUniform("aliveTicks", (int) (currentTick - particleEffect.spawnTick()));
            shader.setUniform("startPosition", particleEffect.x(), particleEffect.y(), particleEffect.z());
            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particleEffect.buffer());
            GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, particleEffect.count());
        }
    }

    private void computeAmbientOcclusion() {
        Matrix4f viewMatrix = Transformation.createViewMatrix(player.getCamera());
        Matrix4f projectionMatrix = player.getCamera().getProjectionMatrix();
        Matrix4f projectionInverse = new Matrix4f(projectionMatrix).invert();

        GuiShader shader = (GuiShader) AssetManager.get(Shaders.SSAO);
        shader.bind();
        shader.setUniform("depthTexture", 0);
        shader.setUniform("noiseTexture", 1);
        shader.setUniform("sideTexture", 2);
        shader.setUniform("projectionMatrix", projectionMatrix);
        shader.setUniform("projectionInverse", projectionInverse);
        shader.setUniform("viewMatrix", viewMatrix);
        shader.setUniform("noiseScale", Window.getWidth() >> 2, Window.getHeight() >> 2);

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, depthTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, noiseTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE2);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, sideTexture);
        GL46.glDisable(GL46.GL_BLEND);

        shader.flipNextDrawVertically();
        shader.drawFullScreenQuad();
    }

    private void applyAmbientOcclusion() {
        GuiShader shader = (GuiShader) AssetManager.get(Shaders.AO_APPLIER);
        shader.bind();
        shader.setUniform("colorTexture", 0);
        shader.setUniform("ssaoTexture", 1);
        shader.setUniform("screenSize", Window.getWidth(), Window.getHeight());

        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, colorTexture);
        GL46.glActiveTexture(GL46.GL_TEXTURE1);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, ssaoTexture);

        shader.flipNextDrawVertically();
        shader.drawFullScreenQuad();
    }

    private void renderWater(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedWaterModels = 0;

        Shader shader = AssetManager.get(Shaders.WATER);
        setUpWaterRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, getRenderTime());
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        shader.setUniform("flags", getFlags(cameraPosition));
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getWaterIndirectBuffer());

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            GL46.glStencilFunc(GL46.GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);

            long start = renderingOptimizer.getWaterLodStart(lod);
            int drawCount = renderingOptimizer.getWaterLodDrawCount(lod);
            renderedWaterModels += drawCount;

            GL46.glMultiDrawArraysIndirect(GL46.GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    private void renderGlass(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedGlassModels = 0;

        Shader shader = AssetManager.get(Shaders.GLASS);
        setUpGlassRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ);
        GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        GL46.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getGlassIndirectBuffer());

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            GL46.glStencilFunc(GL46.GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);

            long start = renderingOptimizer.getGlassLodStart(lod);
            int drawCount = renderingOptimizer.getGlassLodDrawCount(lod);
            renderedGlassModels += drawCount;

            GL46.glMultiDrawArraysIndirect(GL46.GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    private void renderTransparentParticles(Position cameraPosition, Matrix4f projectionViewMatrix) {
        Shader shader = AssetManager.get(Shaders.TRANSPARENT_PARTICLE);
        setUpGlassRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ);
        GL46.glDisable(GL46.GL_STENCIL_TEST);
        long currentTick = Game.getServer().getCurrentGameTick();
        shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());

        for (ParticleEffect particleEffect : player.getParticleCollector().getParticleEffects()) {
            if (particleEffect.isOpaque()) continue;
            shader.setUniform("lifeTimeTicks", particleEffect.lifeTimeTicks());
            shader.setUniform("aliveTicks", (int) (currentTick - particleEffect.spawnTick()));
            shader.setUniform("startPosition", particleEffect.x(), particleEffect.y(), particleEffect.z());
            GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, particleEffect.buffer());
            GL46.glDrawArraysInstanced(GL46.GL_TRIANGLES, 0, 36, particleEffect.count());
        }
    }

    private void renderVolumeIndicator(Position cameraPositon, Matrix4f projectionViewMatrix) {
        Target startTarget = player.getInteractionHandler().getStartTarget();
        Target currentTarget = Target.getPlayerTarget();
        if (startTarget == null || currentTarget == null) return;

        Placeable placeable = player.getHeldPlaceable();
        byte material = placeable instanceof CubePlaceable ? ((CubePlaceable) placeable).getMaterial() : AIR;

        Vector3i startPositon = material == AIR ? startTarget.position() : startTarget.offsetPosition();
        Vector3i endPosition = material == AIR ? currentTarget.position() : currentTarget.offsetPosition();

        Vector3i minPosition = Utils.min(startPositon, endPosition);
        Vector3i maxPosition = Utils.max(startPositon, endPosition);
        CuboidPlaceable.offsetPositions(minPosition, maxPosition);
        maxPosition.add(1, 1, 1);

        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_STENCIL_TEST);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glDepthMask(false);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.MATERIALS).getID());

        Shader shader = AssetManager.get(Shaders.VOLUME_INDICATOR);
        shader.bind();
        shader.setUniform("iCameraPosition",
                cameraPositon.intX & ~CHUNK_SIZE_MASK,
                cameraPositon.intY & ~CHUNK_SIZE_MASK,
                cameraPositon.intZ & ~CHUNK_SIZE_MASK);
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("minPosition", minPosition);
        shader.setUniform("maxPosition", maxPosition);
        shader.setUniform("textures", 0);
        shader.setUniform("material", material);

        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, 36);
    }

    private void renderChat() {
        long currentTime = System.nanoTime();

        ChatTextField chatTextField = firstChildOf(ChatTextField.class);
        Vector2f defaultTextSize = ((FontOption) OptionSetting.FONT.value()).getDefaultTextSize();
        Vector2f position = new Vector2f();
        TextShader shader = (TextShader) AssetManager.get(CoreShaders.TEXT);
        shader.bind();
        float lineSeparation = defaultTextSize.y * FloatSetting.TEXT_SIZE.value();
        float chatMessageDuration = FloatSetting.CHAT_MESSAGE_DURATION.value();
        float chatHeight = chatTextField == null || !chatTextField.isVisible() ? 0.0F : chatTextField.getSizeToParent().y + chatTextField.getOffsetToParent().y;

        int lineCount = 0;
        ArrayList<ChatMessage> messages = this.messages;
        for (int messageIndex = messages.size() - 1; messageIndex >= 0; messageIndex--) {
            ChatMessage chatMessage = messages.get(messageIndex);
            if (!player.isChatOpen() && (currentTime - chatMessage.timestamp()) / 1_000_000_000D > chatMessageDuration) return;

            int messageLines = chatMessage.lines().length;
            if (messageLines == 0) continue;
            Color color = chatMessage.color().getColor();
            String prefix = chatMessage.prefix();
            float prefixSize = TextShader.getTextLength(prefix, defaultTextSize.x, false);

            position.set(0.0F, (lineCount + messageLines - 1) * lineSeparation + chatHeight);
            shader.drawText(position, prefix, color, true, false);

            lineCount += messageLines;
            for (String line : chatMessage.lines()) {
                lineCount--;
                position.set(prefixSize, lineCount * lineSeparation + chatHeight);
                if (position.y >= 1.0F) return;
                shader.drawText(position, line, color, true, false);
            }
            lineCount += messageLines;
        }
    }

    private void renderDebugInfo() {
        int textLine = 0;
        for (DebugScreenLine debugLine : debugLines) if (debugLine.shouldShow(debugScreenOpen)) debugLine.render(++textLine);
    }

    private void renderOccluders(Position cameraPositon, Matrix4f projectionViewMatrix) {
        if (!ToggleSetting.RENDER_OCCLUDERS.value()) return;

        Shader shader = AssetManager.get(Shaders.VOLUME_INDICATOR);
        shader.bind();
        setUpVolumeRendering(cameraPositon, projectionViewMatrix, shader);
        MeshCollector meshCollector = player.getMeshCollector();

        for (Chunk chunk : Game.getWorld().getLod(0)) {
            if (chunk == null || !meshCollector.neighborHasModel(chunk.X, chunk.Y, chunk.Z, 0)) continue;

            AABB occluder = meshCollector.getOccluder(chunk.INDEX, chunk.LOD);
            if (occluder == null) continue;
            renderVolume(shader, chunk, occluder);
        }
    }

    private void renderOccludees(Position cameraPositon, Matrix4f projectionViewMatrix) {
        if (!ToggleSetting.RENDER_OCCLUDEES.value()) return;

        Shader shader = AssetManager.get(Shaders.VOLUME_INDICATOR);
        shader.bind();
        setUpVolumeRendering(cameraPositon, projectionViewMatrix, shader);
        MeshCollector meshCollector = player.getMeshCollector();

        for (Chunk chunk : Game.getWorld().getLod(0)) {
            if (chunk == null) continue;
            OpaqueModel opaqueModel = meshCollector.getOpaqueModel(chunk.INDEX, 0);
            if (opaqueModel == null || opaqueModel.isEmpty()) continue;

            AABB occludee = meshCollector.getOccludee(chunk.INDEX, chunk.LOD);
            if (occludee == null) continue;
            renderVolume(shader, chunk, occludee);
        }
    }

    private void renderOccluderDepthMap() {
        if (!ToggleSetting.RENDER_OCCLUDER_DEPTH_MAP.value()) return;
        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI);
        shader.bind();
        shader.flipNextDrawVertically();
        GL46.glDisable(GL46.GL_BLEND);
        Texture texture = new Texture(renderingOptimizer.depthTexture);
        shader.drawQuad(new Vector2f(0.0F, 0.0F), new Vector2f(0.5F, 0.5F), texture);
    }

    private static void renderVolume(Shader shader, Chunk chunk, AABB aabb) {
        if (aabb.maxX < aabb.minX || aabb.maxY < aabb.minY || aabb.maxZ < aabb.minZ) return;

        int x = chunk.X << CHUNK_SIZE_BITS;
        int y = chunk.Y << CHUNK_SIZE_BITS;
        int z = chunk.Z << CHUNK_SIZE_BITS;

        shader.setUniform("minPosition", x + aabb.minX, y + aabb.minY, z + aabb.minZ);
        shader.setUniform("maxPosition", x + aabb.maxX, y + aabb.maxY, z + aabb.maxZ);

        GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, 36);
    }

    private static void setUpVolumeRendering(Position cameraPositon, Matrix4f projectionViewMatrix, Shader shader) {
        GL46.glDisable(GL46.GL_DEPTH_TEST);
        GL46.glDisable(GL46.GL_CULL_FACE);
        GL46.glDisable(GL46.GL_STENCIL_TEST);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glDepthMask(false);
        GL46.glActiveTexture(GL46.GL_TEXTURE0);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.MATERIALS).getID());

        shader.setUniform("iCameraPosition",
                cameraPositon.intX & ~CHUNK_SIZE_MASK,
                cameraPositon.intY & ~CHUNK_SIZE_MASK,
                cameraPositon.intZ & ~CHUNK_SIZE_MASK);
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("material", 0);
        shader.setUniform("textures", 0);
    }

    private static int getFlags(Position cameraPosition) {
        boolean headUnderWater = Game.getWorld().getMaterial(cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, 0) == WATER;
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

    private boolean debugScreenOpen = false, vSync = true;
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private final ArrayList<Long> frameTimes = new ArrayList<>();
    private final ArrayList<DebugScreenLine> debugLines;
    private final UiElement crosshair;
    private final RenderingOptimizer renderingOptimizer = new RenderingOptimizer();
    private final Player player;

    private int framebuffer, ssaoFramebuffer;
    private int colorTexture, depthTexture, ssaoTexture, noiseTexture, sideTexture;
}
