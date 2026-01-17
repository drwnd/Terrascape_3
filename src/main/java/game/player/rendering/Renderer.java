package game.player.rendering;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.assets.Texture;
import core.assets.TextureArray;
import core.renderables.Renderable;
import core.renderables.UiElement;
import core.rendering_api.Window;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.Shader;
import core.rendering_api.shaders.TextShader;
import core.settings.FloatSetting;
import core.settings.OptionSetting;
import core.settings.ToggleSetting;
import core.settings.optionSettings.FontOption;
import core.settings.optionSettings.TexturePack;

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

import java.awt.*;
import java.lang.Math;
import java.util.ArrayList;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.glfw.GLFW.*;

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
        TextureArray materialsTexture = AssetManager.get(TexturePack.get(TextureArrays.MATERIALS));
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("propertiesTextures", 1);
        shader.setUniform("nightBrightness", FloatSetting.NIGHT_BRIGHTNESS.value());
        shader.setUniform("time", time);
        shader.setUniform("sunDirection", getSunDirection(time));
        shader.setUniform("textureSizes", materialsTexture.getTextureSizes());
        shader.setUniform("maxTextureSize", materialsTexture.getMaxTextureSize());

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glEnable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDepthMask(true);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, materialsTexture.getID());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D_ARRAY, AssetManager.get(TexturePack.get(TextureArrays.PROPERTIES)).getID());
    }

    public static void setUpWaterRendering(Shader shader, Matrix4f matrix, int x, int y, int z, float time) {
        TextureArray materialsTexture = AssetManager.get(TexturePack.get(TextureArrays.MATERIALS));
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("nightBrightness", FloatSetting.NIGHT_BRIGHTNESS.value());
        shader.setUniform("time", time);
        shader.setUniform("sunDirection", getSunDirection(time));
        shader.setUniform("textureSizes", materialsTexture.getTextureSizes());
        shader.setUniform("maxTextureSize", materialsTexture.getMaxTextureSize());

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glEnable(GL_STENCIL_TEST);
        glDisable(GL_CULL_FACE);
        glDepthMask(true);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, materialsTexture.getID());
    }

    public static void setUpGlassRendering(Shader shader, Matrix4f matrix, int x, int y, int z) {
        TextureArray materialsTexture = AssetManager.get(TexturePack.get(TextureArrays.MATERIALS));
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("textureSizes", materialsTexture.getTextureSizes());
        shader.setUniform("maxTextureSize", materialsTexture.getMaxTextureSize());

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glEnable(GL_STENCIL_TEST);
        glBlendFunc(GL_ZERO, GL_SRC_COLOR);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, materialsTexture.getID());
    }

    private void setUpShadowMappedRendering(Matrix4f sunMatrix, Shader shader) {
        shader.setUniform("shadowMap", 2);
        shader.setUniform("shadowColor", 3);
        shader.setUniform("sunMatrix", sunMatrix);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, shadowTexture);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, shadowColorTexture);
    }


    @Override
    protected void renderSelf(Vector2f position, Vector2f size) {
        Camera camera = player.getCamera();
        player.updateFrame();
        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera);
        Matrix4f sunMatrix = Transformation.getSunMatrix(getRenderTime());
        Position cameraPosition = player.getCamera().getPosition();

        if (ToggleSetting.CULLING_COMPUTATION.value()) renderingOptimizer.computeVisibility(player, cameraPosition, projectionViewMatrix);
        if (ToggleSetting.USE_SHADOW_MAPPING.value()) computeShadowMap(cameraPosition, sunMatrix);

        setupRenderState();

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        renderSkybox(camera);
        renderOpaqueGeometry(cameraPosition, projectionViewMatrix, sunMatrix);
        renderOpaqueParticles(cameraPosition, projectionViewMatrix, sunMatrix);

        if (ToggleSetting.USE_AMBIENT_OCCLUSION.value()) {
            glBindFramebuffer(GL_FRAMEBUFFER, ssaoFramebuffer);
            glClear(GL_COLOR_BUFFER_BIT);
            glDisable(GL_STENCIL_TEST);

            computeAmbientOcclusion();

            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

            applyAmbientOcclusion();
        }

        renderWater(cameraPosition, projectionViewMatrix, sunMatrix);
        renderGlass(cameraPosition, projectionViewMatrix);
        renderGlassParticles(cameraPosition, projectionViewMatrix);
        renderVolumeIndicator(cameraPosition, projectionViewMatrix);

        glDisable(GL_STENCIL_TEST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBlitNamedFramebuffer(framebuffer, 0,
                0, 0, Window.getWidth(), Window.getHeight(),
                0, 0, Window.getWidth(), Window.getHeight(),
                GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

        if (ToggleSetting.RENDER_OCCLUDERS.value()) renderOccluders(cameraPosition, projectionViewMatrix);
        if (ToggleSetting.RENDER_OCCLUDEES.value()) renderOccludees(cameraPosition, projectionViewMatrix);
        if (ToggleSetting.RENDER_OCCLUDER_DEPTH_MAP.value()) renderDebugTexture(renderingOptimizer.getDepthTexture());
        if (ToggleSetting.RENDER_SHADOW_MAP.value()) renderDebugTexture(shadowTexture);
        if (ToggleSetting.RENDER_SHADOW_COLORS.value()) renderDebugTexture(shadowColorTexture);

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
        colorTexture = ObjectLoader.createTexture2D(GL_RGBA8, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST);
        sideTexture = ObjectLoader.createTexture2D(GL_R8I, width, height, GL_RED_INTEGER, GL_UNSIGNED_BYTE, GL_NEAREST);
        shadowColorTexture = ObjectLoader.createTexture2D(GL_RGB8, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, GL_RGB, GL_UNSIGNED_BYTE, GL_NEAREST);

        ssaoTexture = ObjectLoader.createTexture2D(GL_RED, width, height, GL_RED, GL_FLOAT, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        depthTexture = ObjectLoader.createTexture2D(GL_DEPTH32F_STENCIL8, width, height, GL_DEPTH_STENCIL, GL_FLOAT_32_UNSIGNED_INT_24_8_REV, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        shadowTexture = ObjectLoader.createTexture2D(GL_DEPTH_COMPONENT32F, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, GL_DEPTH_COMPONENT, GL_FLOAT, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        int noiseSamples = 4 * 4 * 3;
        float[] noise = new float[noiseSamples];
        for (int i = 0; i < noiseSamples; i += 3) {
            noise[i] = (float) (Math.random() * 2 - 1);
            noise[i + 1] = (float) (Math.random() * 2 - 1);
            noise[i + 2] = 0.0F;
        }
        noiseTexture = glCreateTextures(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, 4, 4, 0, GL_RGB, GL_FLOAT, noise);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }

    private void createFrameBuffers() {
        framebuffer = glCreateFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, sideTexture, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));

        ssaoFramebuffer = glCreateFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, ssaoFramebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ssaoTexture, 0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("SSAO Frame buffer not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));

        shadowFramebuffer = glCreateFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowTexture, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, shadowColorTexture, 0);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0});
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Shadow Frame buffer not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void deleteTextures() {
        glDeleteTextures(colorTexture);
        glDeleteTextures(depthTexture);
        glDeleteTextures(noiseTexture);
        glDeleteTextures(ssaoTexture);
        glDeleteTextures(sideTexture);
        glDeleteTextures(shadowTexture);
        glDeleteTextures(shadowColorTexture);
    }

    private void deleteFrameBuffers() {
        glDeleteFramebuffers(framebuffer);
        glDeleteFramebuffers(ssaoFramebuffer);
        glDeleteFramebuffers(shadowFramebuffer);
    }

    private void setupRenderState() {
        long currentTime = System.nanoTime();
        frameTimes.removeIf(frameTime -> currentTime - frameTime > 1_000_000_000L);
        frameTimes.add(currentTime);

        Game.getPlayer().getCamera().updateProjectionMatrix();

        glStencilFunc(GL_ALWAYS, 0, 0xFF);
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        glDisable(GL_STENCIL_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, ToggleSetting.X_RAY.value() ? GL_LINE : GL_FILL);
        if (vSync != ToggleSetting.V_SYNC.value()) {
            vSync = ToggleSetting.V_SYNC.value();
            glfwSwapInterval(vSync ? 1 : 0);
        }

        float crosshairSize = FloatSetting.CROSSHAIR_SIZE.value();
        crosshair.setOffsetToParent(0.5F - crosshairSize * 0.5F, 0.5F - crosshairSize * 0.5F * Window.getAspectRatio());
        crosshair.setSizeToParent(crosshairSize, crosshairSize * Window.getAspectRatio());
    }

    private static void renderSkybox(Camera camera) {
        Shader shader = AssetManager.get(Shaders.SKYBOX);

        shader.bind();
        shader.setUniform("dayTexture", 0);
        shader.setUniform("nightTexture", 1);
        shader.setUniform("time", getRenderTime());
        shader.setUniform("projectionViewMatrix", Transformation.createProjectionRotationMatrix(camera));

        glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, AssetManager.get(TexturePack.get(Textures.DAY_SKY)).getID());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, AssetManager.get(TexturePack.get(Textures.NIGHT_SKY)).getID());

        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        glDrawElements(GL_TRIANGLES, SKY_BOX_INDICES.length, GL_UNSIGNED_INT, 0);

        glDepthMask(true);
    }

    private void computeShadowMap(Position cameraPosition, Matrix4f sunMatrix) {
        glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebuffer);
        glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glColorMask(false, false, false, false);
        glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        glDepthMask(true);

        if (ToggleSetting.CHUNKS_CAST_SHADOWS.value()) {
            Shader shader = AssetManager.get(Shaders.CHUNK_SHADOW);
            shader.bind();
            shader.setUniform("lodSize", 1 << SHADOW_LOD);
            shader.setUniform("projectionViewMatrix", sunMatrix);
            shader.setUniform("iCameraPosition",
                    cameraPosition.intX & ~CHUNK_SIZE_MASK,
                    cameraPosition.intY & ~CHUNK_SIZE_MASK,
                    cameraPosition.intZ & ~CHUNK_SIZE_MASK);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);

            renderingOptimizer.populateOpaqueShadowIndirectBuffer(getRenderTime());

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getShadowIndirectBuffer());

            int drawCount = renderingOptimizer.getShadowDrawCount();
            glMultiDrawArraysIndirect(GL_TRIANGLES, 0, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }

        if (ToggleSetting.PARTICLES_CAST_SHADOWS.value()) {
            long currentTick = Game.getServer().getCurrentGameTick();
            Shader shader = AssetManager.get(Shaders.PARTICLE_SHADOW);
            shader.bind();
            shader.setUniform("projectionViewMatrix", sunMatrix);
            shader.setUniform("iCameraPosition",
                    cameraPosition.intX & ~CHUNK_SIZE_MASK,
                    cameraPosition.intY & ~CHUNK_SIZE_MASK,
                    cameraPosition.intZ & ~CHUNK_SIZE_MASK);
            shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);
            glDisable(GL_STENCIL_TEST);

            renderParticles(shader, currentTick, true);
        }

        glColorMask(true, true, true, true);
        glDepthMask(false);

        if (ToggleSetting.GLASS_CASTS_SHADOWS.value() && ToggleSetting.CHUNKS_CAST_SHADOWS.value()) {
            Shader shader = AssetManager.get(Shaders.GLASS);
            shader.bind();
            shader.setUniform("lodSize", 1 << SHADOW_LOD);
            shader.setUniform("projectionViewMatrix", sunMatrix);
            shader.setUniform("iCameraPosition",
                    cameraPosition.intX & ~CHUNK_SIZE_MASK,
                    cameraPosition.intY & ~CHUNK_SIZE_MASK,
                    cameraPosition.intZ & ~CHUNK_SIZE_MASK);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_ZERO, GL_SRC_COLOR);

            renderingOptimizer.populateGlassShadowIndirectBuffer();

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getShadowIndirectBuffer());

            int drawCount = renderingOptimizer.getShadowDrawCount();
            glMultiDrawArraysIndirect(GL_TRIANGLES, 0, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }

        if (ToggleSetting.GLASS_CASTS_SHADOWS.value() && ToggleSetting.PARTICLES_CAST_SHADOWS.value()) {
            long currentTick = Game.getServer().getCurrentGameTick();
            Shader shader = AssetManager.get(Shaders.GLASS_PARTICLE);
            shader.bind();
            shader.setUniform("projectionViewMatrix", sunMatrix);
            shader.setUniform("iCameraPosition",
                    cameraPosition.intX & ~CHUNK_SIZE_MASK,
                    cameraPosition.intY & ~CHUNK_SIZE_MASK,
                    cameraPosition.intZ & ~CHUNK_SIZE_MASK);
            shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_ZERO, GL_SRC_COLOR);

            renderParticles(shader, currentTick, false);
        }

        glDepthMask(true);
        glViewport(0, 0, Window.getWidth(), Window.getHeight());
    }

    private void renderOpaqueGeometry(Position cameraPosition, Matrix4f projectionViewMatrix, Matrix4f sunMatrix) {
        renderedOpaqueModels = 0;

        Shader shader = AssetManager.get(Shaders.OPAQUE_GEOMETRY);
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, getRenderTime());
        setUpShadowMappedRendering(sunMatrix, shader);
        shader.setUniform("sunMatrix", sunMatrix);
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getOpaqueIndirectBuffer());

        int flags = getFlags(cameraPosition);
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            glStencilFunc(GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);
            shader.setUniform("flags", flags & (lod > SHADOW_LOD ? ~DO_SHADOW_MAPPING_BIT : -1));

            long start = renderingOptimizer.getOpaqueLodStart(lod);
            int drawCount = renderingOptimizer.getOpaqueLodDrawCount(lod);
            renderedOpaqueModels += drawCount / 6;

            glMultiDrawArraysIndirect(GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    private void renderOpaqueParticles(Position cameraPosition, Matrix4f projectionViewMatrix, Matrix4f sunMatrix) {
        Shader shader = AssetManager.get(Shaders.OPAQUE_PARTICLE);
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, getRenderTime());
        setUpShadowMappedRendering(sunMatrix, shader);
        glDisable(GL_STENCIL_TEST);
        long currentTick = Game.getServer().getCurrentGameTick();
        shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());
        shader.setUniform("flags", getFlags(cameraPosition));

        renderParticles(shader, currentTick, true);
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
        shader.setUniform("samples", (int) FloatSetting.AMBIENT_OCCLUSION_SAMPLES.value());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, sideTexture);
        glDisable(GL_BLEND);

        shader.flipNextDrawVertically();
        shader.drawFullScreenQuad();
    }

    private void applyAmbientOcclusion() {
        GuiShader shader = (GuiShader) AssetManager.get(Shaders.AO_APPLIER);
        shader.bind();
        shader.setUniform("colorTexture", 0);
        shader.setUniform("ssaoTexture", 1);
        shader.setUniform("screenSize", Window.getWidth(), Window.getHeight());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, ssaoTexture);

        shader.flipNextDrawVertically();
        shader.drawFullScreenQuad();
    }

    private void renderWater(Position cameraPosition, Matrix4f projectionViewMatrix, Matrix4f sunMatrix) {
        renderedWaterModels = 0;

        Shader shader = AssetManager.get(Shaders.WATER);
        setUpWaterRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, getRenderTime());
        setUpShadowMappedRendering(sunMatrix, shader);
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getWaterIndirectBuffer());

        int flags = getFlags(cameraPosition);
        for (int lod = 0; lod < LOD_COUNT; lod++) {
            glStencilFunc(GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);
            shader.setUniform("flags", flags & (lod > SHADOW_LOD ? ~DO_SHADOW_MAPPING_BIT : -1));

            long start = renderingOptimizer.getWaterLodStart(lod);
            int drawCount = renderingOptimizer.getWaterLodDrawCount(lod);
            renderedWaterModels += drawCount;

            glMultiDrawArraysIndirect(GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    private void renderGlass(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedGlassModels = 0;

        Shader shader = AssetManager.get(Shaders.GLASS);
        setUpGlassRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getGlassIndirectBuffer());

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            glStencilFunc(GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);

            long start = renderingOptimizer.getGlassLodStart(lod);
            int drawCount = renderingOptimizer.getGlassLodDrawCount(lod);
            renderedGlassModels += drawCount;

            glMultiDrawArraysIndirect(GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    private void renderGlassParticles(Position cameraPosition, Matrix4f projectionViewMatrix) {
        Shader shader = AssetManager.get(Shaders.GLASS_PARTICLE);
        setUpGlassRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ);
        glDisable(GL_STENCIL_TEST);
        long currentTick = Game.getServer().getCurrentGameTick();
        shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());

        renderParticles(shader, currentTick, false);
    }

    private void renderParticles(Shader shader, long currentTick, boolean opaque) {
        for (ParticleEffect particleEffect : player.getParticleCollector().getParticleEffects()) {
            if (particleEffect.isOpaque() != opaque) continue;
            shader.setUniform("lifeTimeTicks", particleEffect.lifeTimeTicks());
            shader.setUniform("aliveTicks", (int) (currentTick - particleEffect.spawnTick()));
            shader.setUniform("startPosition", particleEffect.x(), particleEffect.y(), particleEffect.z());
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, particleEffect.buffer());
            glDrawArraysInstanced(GL_TRIANGLES, 0, 36, particleEffect.count());
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

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_STENCIL_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glDepthMask(false);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, AssetManager.get(TexturePack.get(TextureArrays.MATERIALS)).getID());

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

        glDrawArrays(GL_TRIANGLES, 0, 36);
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
        float scroll = chatTextField == null ? 0.0F : chatTextField.getInput().getScroll();

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

            position.set(0.0F, (lineCount + messageLines - 1) * lineSeparation + chatHeight - scroll);
            shader.drawText(position, prefix, color, true, false);

            lineCount += messageLines;
            for (String line : chatMessage.lines()) {
                lineCount--;
                position.set(prefixSize, lineCount * lineSeparation + chatHeight - scroll);
                if (position.y >= 1.0F) return;
                shader.drawText(position, line, color, true, false);
            }
            lineCount += messageLines;
        }
    }

    private void renderDebugInfo() {
        boolean debugScreenOpen = ToggleSetting.DEBUG_MENU.value();
        int textLine = 0;
        for (DebugScreenLine debugLine : debugLines) if (debugLine.shouldShow(debugScreenOpen)) debugLine.render(++textLine);
    }

    private void renderOccluders(Position cameraPositon, Matrix4f projectionViewMatrix) {
        int lod = (int) FloatSetting.OCCLUDERS_OCCLUDEES_LOD.value();
        if (lod < 0 || lod >= LOD_COUNT) return;

        Shader shader = AssetManager.get(Shaders.VOLUME_INDICATOR);
        shader.bind();
        setUpVolumeRendering(cameraPositon, projectionViewMatrix, shader);
        MeshCollector meshCollector = player.getMeshCollector();

        for (Chunk chunk : Game.getWorld().getLod(lod)) {
            if (chunk == null || meshCollector.isIsolated(chunk.X, chunk.Y, chunk.Z, lod)) continue;
            if ((renderingOptimizer.getVisibilityBits(lod)[chunk.INDEX >> 6] & 1L << chunk.INDEX) == 0) continue;

            AABB occluder = meshCollector.getOccluder(chunk.INDEX, chunk.LOD);
            if (occluder == null) continue;
            renderVolume(shader, chunk, occluder, lod);
        }
    }

    private void renderOccludees(Position cameraPositon, Matrix4f projectionViewMatrix) {
        int lod = (int) FloatSetting.OCCLUDERS_OCCLUDEES_LOD.value();
        if (lod < 0 || lod >= LOD_COUNT) return;

        Shader shader = AssetManager.get(Shaders.VOLUME_INDICATOR);
        shader.bind();
        setUpVolumeRendering(cameraPositon, projectionViewMatrix, shader);
        MeshCollector meshCollector = player.getMeshCollector();

        for (Chunk chunk : Game.getWorld().getLod(lod)) {
            if (chunk == null) continue;
            if ((renderingOptimizer.getVisibilityBits(lod)[chunk.INDEX >> 6] & 1L << chunk.INDEX) == 0) continue;
            OpaqueModel opaqueModel = meshCollector.getOpaqueModel(chunk.INDEX, lod);
            if (opaqueModel == null || opaqueModel.isEmpty()) continue;

            AABB occludee = meshCollector.getOccludee(chunk.INDEX, chunk.LOD);
            if (occludee == null) continue;
            renderVolume(shader, chunk, occludee, lod);
        }
    }

    private static void renderDebugTexture(int texture) {
        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI);
        shader.bind();
        shader.flipNextDrawVertically();
        glDisable(GL_BLEND);
        shader.drawQuad(new Vector2f(0.0F, 0.0F), new Vector2f(0.5F, 0.5F), new Texture(texture));
    }

    private static void renderVolume(Shader shader, Chunk chunk, AABB aabb, int lod) {
        if (aabb.maxX < aabb.minX || aabb.maxY < aabb.minY || aabb.maxZ < aabb.minZ) return;

        int x = chunk.X << CHUNK_SIZE_BITS + lod;
        int y = chunk.Y << CHUNK_SIZE_BITS + lod;
        int z = chunk.Z << CHUNK_SIZE_BITS + lod;

        shader.setUniform("minPosition", x + (aabb.minX << lod), y + (aabb.minY << lod) - (1 << lod) + 1, z + (aabb.minZ << lod));
        shader.setUniform("maxPosition", x + (aabb.maxX << lod), y + (aabb.maxY << lod) - (1 << lod) + 1, z + (aabb.maxZ << lod));

        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    private static void setUpVolumeRendering(Position cameraPositon, Matrix4f projectionViewMatrix, Shader shader) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_STENCIL_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glDepthMask(false);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, AssetManager.get(TexturePack.get(TextureArrays.MATERIALS)).getID());

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
        boolean useShadowMapping = ToggleSetting.USE_SHADOW_MAPPING.value();
        boolean doGlassShadows = ToggleSetting.GLASS_CASTS_SHADOWS.value();
        return (doGlassShadows ? DO_GLASS_SHADOWS_BIT : 0) | (useShadowMapping ? DO_SHADOW_MAPPING_BIT : 0) | (headUnderWater ? HEAD_UNDER_WATER_BIT : 0);
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

    private boolean vSync = true;
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private final ArrayList<Long> frameTimes = new ArrayList<>();
    private final ArrayList<DebugScreenLine> debugLines;
    private final UiElement crosshair;
    private final RenderingOptimizer renderingOptimizer = new RenderingOptimizer();
    private final Player player;

    private int framebuffer, colorTexture, depthTexture, sideTexture;
    private int ssaoFramebuffer, ssaoTexture, noiseTexture;
    private int shadowFramebuffer, shadowTexture, shadowColorTexture;

    private static final int HEAD_UNDER_WATER_BIT = 1;
    private static final int DO_SHADOW_MAPPING_BIT = 2;
    private static final int DO_GLASS_SHADOWS_BIT = 4;
}
