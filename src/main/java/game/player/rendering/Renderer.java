package game.player.rendering;

import core.assets.*;
import core.assets.identifiers.TextureIdentifier;
import core.renderables.Renderable;
import core.renderables.UiElement;
import core.rendering_api.CoreObjectLoader;
import core.rendering_api.Input;
import core.rendering_api.Window;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.Shader;
import core.rendering_api.shaders.TextShader;
import core.settings.CoreFloatSettings;
import core.settings.CoreOptionSettings;
import core.settings.CoreToggleSettings;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.FontOption;
import core.utils.MainThread;
import core.utils.Vector3l;

import game.assets.*;
import game.player.ChatTextField;
import game.player.Player;
import game.player.interaction.*;
import game.player.interaction.placeable_shapes.CapsulePlaceable;
import game.player.movement.MovementState;
import game.player.particles.ParticleEffect;
import game.server.*;
import game.server.generation.Structure;
import game.settings.*;
import game.utils.*;

import org.joml.*;

import java.awt.*;
import java.lang.Math;
import java.util.ArrayList;

import static game.utils.Constants.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.glfw.GLFW.*;

public final class Renderer extends Renderable {

    public int renderedOpaqueModels, renderedTransparentModels, renderedGlassModels;

    @MainThread
    public Renderer(Player player, MeshCollector meshCollector) {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));
        this.player = player;
        setDoAutoFocusScaling(false);
        debugLines = DebugScreenLine.getDebugLines();

        renderingOptimizer = new RenderingOptimizer(meshCollector);
        crosshair = new UiElement(new Vector2f(), new Vector2f(), Textures.CROSSHAIR);
        crosshair.setScaleWithGuiSize(false);
        crosshair.setDoAutoFocusScaling(false);

        addHUDRenderable(crosshair);
        addHUDRenderable(new BreakPlaceOptionsDisplay());

        createTextures(Window.getWidth(), Window.getHeight());
        createFrameBuffers();
    }


    @MainThread
    public void addHUDRenderable(Renderable renderable) {
        hudElements.add(renderable);
        addRenderable(renderable);
    }

    @MainThread
    public ArrayList<Long> getFrameTimes() {
        return frameTimes;
    }

    @MainThread
    public static float getRenderTime() {
        Server server = Game.getServer();
        float renderTime = server.getDayTime() + FloatSettings.TIME_SPEED.value() * server.getCurrentGameTickFraction();
        if (renderTime > 1.0F) renderTime -= 2.0F;
        return renderTime;
    }

    @ServerThread
    public void updateGameTick() {
        messages = Game.getServer().getMessages();
    }

    @MainThread
    public void invalidateHologram() {
        hologramModelsValid = false;
    }

    @MainThread
    public void reloadRenderingOptimizer() {
        renderingOptimizer.cleanUp();
        renderingOptimizer = new RenderingOptimizer(player.getMeshCollector());
    }

    @MainThread
    public void reloadShadowMaps(int ignore) {
        glDeleteTextures(shadowTexture);
        glDeleteTextures(shadowColorTexture);
        glDeleteFramebuffers(shadowFramebuffers);
        createShadowTextures();
        createShadowFrameBuffers();
    }


    @MainThread
    public static void takeScreenshot() {
        String screenshotName = Window.takeScreenShot();
        if (screenshotName == null) Game.getServer().sendServerMessage("Could not save Screenshot", ColorOption.RED);
        else Game.getServer().sendServerMessage("Saved Screenshot as " + screenshotName, ColorOption.WHITE);
    }

    @MainThread
    public static void setupOpaqueRendering(Shader shader, Matrix4f matrix, long x, long y, long z, float time) {
        TextureArray materialsTexture = AssetManager.get(TextureArrays.MATERIALS);
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("propertiesTextures", 1);
        shader.setUniform("nightBrightness", FloatSettings.NIGHT_BRIGHTNESS.value());
        shader.setUniform("time", time);
        shader.setUniform("sunDirection", getSunDirection(time));
        shader.setUniform("textureSizes", materialsTexture.textureSizes());
        shader.setUniform("maxTextureSize", materialsTexture.maxTextureSize());

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glEnable(GL_STENCIL_TEST);
        glDisable(GL_BLEND);
        glDepthMask(true);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, materialsTexture.id());
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.PROPERTIES).id());
    }

    @MainThread
    public static void setUpTransparentRendering(Shader shader, Matrix4f matrix, long x, long y, long z, float time) {
        TextureArray materialsTexture = AssetManager.get(TextureArrays.MATERIALS);
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("nightBrightness", FloatSettings.NIGHT_BRIGHTNESS.value());
        shader.setUniform("time", time);
        shader.setUniform("sunDirection", getSunDirection(time));
        shader.setUniform("textureSizes", materialsTexture.textureSizes());
        shader.setUniform("maxTextureSize", materialsTexture.maxTextureSize());

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glDisable(GL_CULL_FACE);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, materialsTexture.id());
    }

    @MainThread
    public static void setUpGlassRendering(Shader shader, Matrix4f matrix, long x, long y, long z) {
        TextureArray materialsTexture = AssetManager.get(TextureArrays.MATERIALS);
        shader.bind();
        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("iCameraPosition", x & ~CHUNK_SIZE_MASK, y & ~CHUNK_SIZE_MASK, z & ~CHUNK_SIZE_MASK);

        shader.setUniform("textures", 0);
        shader.setUniform("textureSizes", materialsTexture.textureSizes());
        shader.setUniform("maxTextureSize", materialsTexture.maxTextureSize());

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glDisable(GL_STENCIL_TEST);
        glEnable(GL_CULL_FACE);
        glBlendFunc(GL_ZERO, GL_SRC_COLOR);
        glDepthMask(false);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, materialsTexture.id());
    }

    @MainThread
    private void setUpShadowMappedRendering(Shader shader) {
        shader.setUniform("shadowMap", 2);
        shader.setUniform("shadowColor", 3);
        shader.setUniform("sunMatrices", sunMatrices);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowTexture);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowColorTexture);
    }


    @Override
    @MainThread
    protected void renderSelf(Vector2f position, Vector2f size) {
        glDepthFunc(GL_GREATER);

        Camera camera = player.getCamera();
        Position toRenderPlayerPosition = player.updateFrame();
        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera);
        Position cameraPosition = player.getCamera().getPosition();

        Model playerCharacter = AssetManager.get(Models.PLAYER_MODEL);
        float frameTime = frameTimes.size() >= 2 ? (frameTimes.getLast() - frameTimes.get(frameTimes.size() - 2)) / 1_000_000F : 0;
        animationTimer = player.getMovement().getState().applyAnimation(playerCharacter, player.getCamera(), animationTimer, frameTime);
        player.applyAnimation(playerCharacter);

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        if (ToggleSettings.CULLING_COMPUTATION.value()) {
            if (OptionSettings.OCCLUSION_CULLING.value() == RenderingOptimizer.OcclusionCullingOptions.AGGRESSIVE)
                renderingOptimizer.computeVisibility(player, lastCameraPosition, lastProjectionViewMatrix);
            else renderingOptimizer.computeVisibility(player, cameraPosition, projectionViewMatrix);
        }
        if (ToggleSettings.USE_SHADOW_MAPPING.value()) computeShadowMap(cameraPosition, toRenderPlayerPosition);

        lastProjectionViewMatrix = projectionViewMatrix;
        lastCameraPosition = cameraPosition;
        setupRenderState();

        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        renderSkybox(camera);
        renderOpaqueGeometry(cameraPosition, projectionViewMatrix);
        renderOpaqueParticles(cameraPosition, projectionViewMatrix);
        renderPlayerCharacter(cameraPosition, projectionViewMatrix, toRenderPlayerPosition);

        glDrawBuffers(GL_COLOR_ATTACHMENT0);
        if (ToggleSettings.USE_AMBIENT_OCCLUSION.value() && IntSettings.AMBIENT_OCCLUSION_SAMPLES.value() > 0)
            applyAmbientOcclusion(cameraPosition, projectionViewMatrix);

        startTransparentRendering();
        renderTransparentGeometry(cameraPosition, projectionViewMatrix);
        finishTransparentRendering();

        renderGlass(cameraPosition, projectionViewMatrix);
        renderGlassParticles(cameraPosition, projectionViewMatrix);
        renderPlaceableHologram(cameraPosition, projectionViewMatrix);

        glDisable(GL_STENCIL_TEST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBlitNamedFramebuffer(framebuffer, 0,
                0, 0, Window.getWidth(), Window.getHeight(),
                0, 0, Window.getWidth(), Window.getHeight(),
                GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDrawBuffers(GL_COLOR_ATTACHMENT0);

        if (ToggleSettings.RENDER_OCCLUDERS.value()) renderOccluders(cameraPosition, projectionViewMatrix);
        if (ToggleSettings.RENDER_OCCLUDEES.value()) renderOccludees(cameraPosition, projectionViewMatrix);
        if (ToggleSettings.RENDER_OCCLUDER_DEPTH_MAP.value()) renderDebugTexture2D(depthTexture);
        if (ToggleSettings.RENDER_SHADOW_MAP.value()) renderDebugTexture3D(shadowTexture, IntSettings.DEBUG_VISUALIZATION_LOD.value());
        if (ToggleSettings.RENDER_SHADOW_COLORS.value()) renderDebugTexture3D(shadowColorTexture, IntSettings.DEBUG_VISUALIZATION_LOD.value());
        if (ToggleSettings.RENDER_ACCUMULATION_TEXTURE.value()) renderDebugTexture2D(accumulationTexture);
        if (ToggleSettings.RENDER_REVEAL_TEXTURE.value()) renderDebugTexture2D(revealTexture);

        renderChat();
        renderDebugInfo();

        glDepthMask(true);
    }

    @Override
    @MainThread
    public void setOnTop() {
        player.setInput();
    }

    @Override
    @MainThread
    public void hoverOver(Vector2i pixelCoordinate) {
        if (player.getInventory().isVisible()) player.getInventory().hoverOver(pixelCoordinate);
    }

    @Override
    @MainThread
    protected void resizeSelfTo(int width, int height) {
        if (width == 0 || height == 0) return;

        reloadRenderingOptimizer();

        deleteFrameBuffers();
        deleteTextures();

        createTextures(width, height);
        createFrameBuffers();
    }

    @Override
    @MainThread
    public void deleteSelf() {
        deleteFrameBuffers();
        deleteTextures();
        renderingOptimizer.cleanUp();
    }


    @MainThread
    private void setupRenderState() {
        for (Renderable renderable : hudElements) renderable.setVisible(ToggleSettings.RENDER_HUD.value());

        long currentTime = System.nanoTime();
        frameTimes.removeIf(frameTime -> currentTime - frameTime > 1_000_000_000L);
        frameTimes.add(currentTime);

        Game.getPlayer().getCamera().updateProjectionMatrix();

        glStencilFunc(GL_ALWAYS, 0, 0xFF);
        glDepthFunc(GL_GREATER);
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        glDisable(GL_STENCIL_TEST);
        glPolygonMode(GL_FRONT_AND_BACK, ToggleSettings.TOGGLE_X_RAY.value() ? GL_LINE : GL_FILL);
        if (vSync != CoreToggleSettings.V_SYNC.value()) {
            vSync = CoreToggleSettings.V_SYNC.value();
            glfwSwapInterval(vSync ? 1 : 0);
        }

        float crosshairSize = FloatSettings.CROSSHAIR_SIZE.value();
        crosshair.setOffsetToParent(0.5F - crosshairSize * 0.5F, 0.5F - crosshairSize * 0.5F * Window.getAspectRatio());
        crosshair.setSizeToParent(crosshairSize, crosshairSize * Window.getAspectRatio());
    }

    @MainThread
    private static void renderSkybox(Camera camera) {
        Shader shader = AssetManager.get(Shaders.SKYBOX);

        shader.bind();
        shader.setUniform("dayTexture", 0);
        shader.setUniform("nightTexture", 1);
        shader.setUniform("time", getRenderTime());
        shader.setUniform("projectionViewMatrix", Transformation.createProjectionRotationMatrix(camera));

        glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).id());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glActiveTexture(GL_TEXTURE0);
        AssetManager.get(Textures.DAY_SKY).bind();
        glActiveTexture(GL_TEXTURE1);
        AssetManager.get(Textures.NIGHT_SKY).bind();

        glDepthMask(false);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        glDrawElements(GL_TRIANGLES, SKY_BOX_INDICES.length, GL_UNSIGNED_INT, 0);

        glDepthMask(true);
    }

    @MainThread
    private void computeShadowMap(Position cameraPosition, Position playerPosition) {
        currentShadowIndex = (currentShadowIndex + 1) % shadowFramebuffers.length;
        float renderTime = getRenderTime();
        Vector3f sunDirection = Transformation.getSunDirection(renderTime).mul(-4096);
        int shadowLod = SHADOW_LOD + currentShadowIndex;
        if (shadowLod >= IntSettings.LOD_COUNT.value()) return;

        shadowSnapshotPositions[currentShadowIndex] = cameraPosition;
        for (int cascade = 0; cascade < sunMatrices.length; cascade++)
            Transformation.updateSunMatrix(sunMatrices[cascade], shadowSnapshotPositions[cascade], cameraPosition, renderTime, cascade);

        glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebuffers[currentShadowIndex]);
        glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glColorMask(false, false, false, false);
        glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        glDepthMask(true);

        if (ToggleSettings.CHUNKS_CAST_SHADOWS.value()) {
            Shader shader = AssetManager.get(Shaders.CHUNK_SHADOW);
            shader.bind();
            shader.setUniform("lodSize", 1 << shadowLod);
            shader.setUniform("projectionViewMatrix", sunMatrices[currentShadowIndex]);
            shader.setUniform("iCameraPosition",
                    cameraPosition.longX & ~CHUNK_SIZE_MASK,
                    cameraPosition.longY & ~CHUNK_SIZE_MASK,
                    cameraPosition.longZ & ~CHUNK_SIZE_MASK);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);

            renderingOptimizer.populateOpaqueShadowIndirectBuffer(shadowLod, renderTime);

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getShadowIndirectBuffer());

            int drawCount = renderingOptimizer.getShadowDrawCount();
            glMultiDrawArraysIndirect(GL_TRIANGLES, 0, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }

        if (ToggleSettings.PARTICLES_CAST_SHADOWS.value() && currentShadowIndex == 0) {
            long currentTick = Game.getServer().getCurrentGameTick();
            Shader shader = AssetManager.get(Shaders.PARTICLE_SHADOW);
            shader.bind();
            shader.setUniform("projectionViewMatrix", sunMatrices[currentShadowIndex]);
            shader.setUniform("iCameraPosition",
                    cameraPosition.longX & ~CHUNK_SIZE_MASK,
                    cameraPosition.longY & ~CHUNK_SIZE_MASK,
                    cameraPosition.longZ & ~CHUNK_SIZE_MASK);
            shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());
            shader.setUniform("viewPosition", sunDirection.x, sunDirection.y, sunDirection.z);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);
            glDisable(GL_STENCIL_TEST);

            renderParticles(shader, currentTick, true);
        }

        if (ToggleSettings.PLAYER_CASTS_SHADOWS.value() && currentShadowIndex == 0) {
            Shader shader = AssetManager.get(Shaders.MODEL_SHADOW);
            shader.bind();
            renderPlayerCharacter(shader, cameraPosition, playerPosition, sunMatrices[currentShadowIndex]);
        }

        glColorMask(true, true, true, true);
        glDepthMask(false);

        if (ToggleSettings.GLASS_CASTS_SHADOWS.value() && ToggleSettings.CHUNKS_CAST_SHADOWS.value()) {
            Shader shader = AssetManager.get(Shaders.GLASS);
            shader.bind();
            shader.setUniform("lodSize", 1 << shadowLod);
            shader.setUniform("projectionViewMatrix", sunMatrices[currentShadowIndex]);
            shader.setUniform("iCameraPosition",
                    cameraPosition.longX & ~CHUNK_SIZE_MASK,
                    cameraPosition.longY & ~CHUNK_SIZE_MASK,
                    cameraPosition.longZ & ~CHUNK_SIZE_MASK);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_ZERO, GL_SRC_COLOR);

            renderingOptimizer.populateGlassShadowIndirectBuffer(shadowLod);

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getShadowIndirectBuffer());

            int drawCount = renderingOptimizer.getShadowDrawCount();
            glMultiDrawArraysIndirect(GL_TRIANGLES, 0, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }

        if (ToggleSettings.GLASS_CASTS_SHADOWS.value() && ToggleSettings.PARTICLES_CAST_SHADOWS.value() && currentShadowIndex == 0) {
            long currentTick = Game.getServer().getCurrentGameTick();
            Shader shader = AssetManager.get(Shaders.GLASS_PARTICLE);
            shader.bind();
            shader.setUniform("projectionViewMatrix", sunMatrices[currentShadowIndex]);
            shader.setUniform("iCameraPosition",
                    cameraPosition.longX & ~CHUNK_SIZE_MASK,
                    cameraPosition.longY & ~CHUNK_SIZE_MASK,
                    cameraPosition.longZ & ~CHUNK_SIZE_MASK);
            shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());
            shader.setUniform("viewPosition", sunDirection.x, sunDirection.y, sunDirection.z);

            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glEnable(GL_BLEND);
            glBlendFunc(GL_ZERO, GL_SRC_COLOR);

            renderParticles(shader, currentTick, false);
        }

        glDepthMask(true);
        glViewport(0, 0, Window.getWidth(), Window.getHeight());
    }

    @MainThread
    private void renderOpaqueGeometry(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedOpaqueModels = 0;

        Shader shader = AssetManager.get(Shaders.OPAQUE_GEOMETRY);
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.longX, cameraPosition.longY, cameraPosition.longZ, getRenderTime());
        setUpShadowMappedRendering(shader);
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getOpaqueIndirectBuffer());

        int flags = getFlags(cameraPosition);
        int maxShadowLod = SHADOW_LOD + IntSettings.SHADOW_CASCADES_COUNT.value();

        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            glStencilFunc(GL_GEQUAL, lodCount - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);
            shader.setUniform("flags", flags & (lod > maxShadowLod ? ~DO_SHADOW_MAPPING_BIT : -1));
            shader.setUniform("shadowStartIndex", Math.max(0, lod - 1));

            long start = renderingOptimizer.getOpaqueLodStart(lod);
            int drawCount = renderingOptimizer.getOpaqueLodDrawCount(lod);
            renderedOpaqueModels += drawCount / 6;

            glMultiDrawArraysIndirect(GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    @MainThread
    private void renderOpaqueParticles(Position cameraPosition, Matrix4f projectionViewMatrix) {
        Shader shader = AssetManager.get(Shaders.OPAQUE_PARTICLE);
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.longX, cameraPosition.longY, cameraPosition.longZ, getRenderTime());
        setUpShadowMappedRendering(shader);
        glDisable(GL_STENCIL_TEST);
        long currentTick = Game.getServer().getCurrentGameTick();
        shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());
        shader.setUniform("flags", getFlags(cameraPosition));
        shader.setUniform("viewPosition", cameraPosition.getInChunkPosition());
        shader.setUniform("shadowStartIndex", 0);

        renderParticles(shader, currentTick, true);
    }

    @MainThread
    private void renderPlayerCharacter(Position cameraPosition, Matrix4f projectionViewMatrix, Position playerPosition) {
        if (ToggleSettings.HIDE_BODY_IN_FIRST_PERSON.value() && OptionSettings.PERSPECTIVE.value() == Camera.Perspective.FIRST_PERSON) return;
        Shader shader = AssetManager.get(Shaders.MODEL);
        shader.bind();
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.longX, cameraPosition.longY, cameraPosition.longZ, getRenderTime());
        setUpShadowMappedRendering(shader);
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        shader.setUniform("image", 0);
        shader.setUniform("flags", getFlags(cameraPosition));
        shader.setUniform("shadowStartIndex", 0);

        glDisable(GL_STENCIL_TEST);
        glDisable(GL_CULL_FACE);
        glActiveTexture(GL_TEXTURE0);
        AssetManager.get((TextureIdentifier) OptionSettings.SKIN.value()).bind();

        if (OptionSettings.PERSPECTIVE.value() == Camera.Perspective.FIRST_PERSON) {
            Model playerCharacter = AssetManager.get(Models.PLAYER_MODEL);
            MovementState state = player.getMovement().getState();
            if (state.hideHeadInFirstPerson()) playerCharacter.transforms()[Models.HEAD].zero();
            if (state.hideBodyInFirstPerson()) playerCharacter.transforms()[Models.BODY].zero();
        }

        renderPlayerCharacter(shader, cameraPosition, playerPosition, projectionViewMatrix);
        glEnable(GL_CULL_FACE);
    }

    @MainThread
    private static void renderPlayerCharacter(Shader shader, Position cameraPosition, Position playerPosition, Matrix4f matrix) {
        Model playerCharacter = AssetManager.get(Models.PLAYER_MODEL);
        Vector3l cameraChunkPosition = new Vector3l(
                cameraPosition.longX & ~CHUNK_SIZE_MASK,
                cameraPosition.longY & ~CHUNK_SIZE_MASK,
                cameraPosition.longZ & ~CHUNK_SIZE_MASK);

        shader.setUniform("projectionViewMatrix", matrix);
        shader.setUniform("position",
                (playerPosition.longX - cameraChunkPosition.x) + playerPosition.fractionX,
                (playerPosition.longY - cameraChunkPosition.y) + playerPosition.fractionY,
                (playerPosition.longZ - cameraChunkPosition.z) + playerPosition.fractionZ
        );
        shader.setUniform("transformations", playerCharacter.transforms());

        glBindVertexArray(playerCharacter.guiElement().vao());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glDrawArrays(GL_TRIANGLES, 0, playerCharacter.guiElement().vertexCount());
    }

    @MainThread
    private void applyAmbientOcclusion(Position cameraPosition, Matrix4f projectionViewMatrix) {
        GuiShader shader = (GuiShader) AssetManager.get(Shaders.SSAO);
        shader.bind();
        shader.setUniform("colorTexture", 0);
        shader.setUniform("intPosTexture", 1);
        shader.setUniform("screenSize", Window.getWidth(), Window.getHeight());

        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("inChunkPosition", (float) (cameraPosition.longX & CHUNK_SIZE_MASK), cameraPosition.longY & CHUNK_SIZE_MASK, cameraPosition.longZ & CHUNK_SIZE_MASK);
        shader.setUniform("samples", IntSettings.AMBIENT_OCCLUSION_SAMPLES.value());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, intPosTexture);
        glDisable(GL_BLEND);
        glDisable(GL_STENCIL_TEST);

        shader.flipNextDrawVertically();
        shader.drawFullScreenQuad();
    }

    @MainThread
    private void startTransparentRendering() {
        glDepthMask(false);
        glEnable(GL_BLEND);
        glBlendFunci(0, GL_ONE, GL_ONE);
        glBlendFunci(1, GL_ZERO, GL_ONE_MINUS_SRC_COLOR);
        glBlendEquation(GL_FUNC_ADD);
        glBindFramebuffer(GL_FRAMEBUFFER, transparencyFramebuffer);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
        glClearBufferfv(GL_COLOR, 0, new float[]{0, 0, 0, 0});
        glClearBufferfv(GL_COLOR, 1, new float[]{1, 1, 1, 1});
    }

    @MainThread
    private void renderTransparentGeometry(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedTransparentModels = 0;

        Shader shader = AssetManager.get(Shaders.TRANSPARENT);
        setUpTransparentRendering(shader, projectionViewMatrix, cameraPosition.longX, cameraPosition.longY, cameraPosition.longZ, getRenderTime());
        setUpShadowMappedRendering(shader);
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getTransparentIndirectBuffer());

        int flags = getFlags(cameraPosition);
        int maxShadowLod = SHADOW_LOD + IntSettings.SHADOW_CASCADES_COUNT.value();

        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            shader.setUniform("lodSize", 1 << lod);
            shader.setUniform("flags", flags & (lod > maxShadowLod ? ~DO_SHADOW_MAPPING_BIT : -1));
            shader.setUniform("shadowStartIndex", Math.max(0, lod - 1));

            long start = renderingOptimizer.getTransparentLodStart(lod);
            int drawCount = renderingOptimizer.getTransparentLodDrawCount(lod);
            renderedTransparentModels += drawCount;

            glMultiDrawArraysIndirect(GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    @MainThread
    private void finishTransparentRendering() {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_STENCIL_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0});

        GuiShader shader = (GuiShader) AssetManager.get(Shaders.TRANSPARENCY_APPLIER);
        shader.bind();
        shader.setUniform("accumulationTexture", 0);
        shader.setUniform("revealTexture", 1);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, accumulationTexture);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, revealTexture);

        shader.flipNextDrawVertically();
        shader.drawFullScreenQuad();
    }

    @MainThread
    private void renderGlass(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedGlassModels = 0;

        Shader shader = AssetManager.get(Shaders.GLASS);
        setUpGlassRendering(shader, projectionViewMatrix, cameraPosition.longX, cameraPosition.longY, cameraPosition.longZ);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, player.getMeshCollector().getBuffer());
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, renderingOptimizer.getGlassIndirectBuffer());

        for (int lod = 0, lodCount = Game.getWorld().LOD_COUNT; lod < lodCount; lod++) {
            glStencilFunc(GL_GEQUAL, lodCount - lod, 0xFF);
            shader.setUniform("lodSize", 1 << lod);

            long start = renderingOptimizer.getGlassLodStart(lod);
            int drawCount = renderingOptimizer.getGlassLodDrawCount(lod);
            renderedGlassModels += drawCount;

            glMultiDrawArraysIndirect(GL_TRIANGLES, start, drawCount, RenderingOptimizer.INDIRECT_COMMAND_SIZE);
        }
    }

    @MainThread
    private void renderGlassParticles(Position cameraPosition, Matrix4f projectionViewMatrix) {
        Shader shader = AssetManager.get(Shaders.GLASS_PARTICLE);
        setUpGlassRendering(shader, projectionViewMatrix, cameraPosition.longX, cameraPosition.longY, cameraPosition.longZ);
        glDisable(GL_STENCIL_TEST);
        long currentTick = Game.getServer().getCurrentGameTick();
        shader.setUniform("gameTickFraction", Game.getServer().getCurrentGameTickFraction());
        shader.setUniform("viewPosition", cameraPosition.getInChunkPosition());

        renderParticles(shader, currentTick, false);
    }

    @MainThread
    private void renderParticles(Shader shader, long currentTick, boolean opaque) {
        for (ParticleEffect particleEffect : player.getParticleCollector().getParticleEffects()) {
            if (particleEffect.isOpaque() != opaque) continue;
            shader.setUniform("lifeTimeTicks", particleEffect.lifeTimeTicks());
            shader.setUniform("aliveTicks", (int) (currentTick - particleEffect.spawnTick()));
            shader.setUniform("startPosition", particleEffect.x(), particleEffect.y(), particleEffect.z());
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, particleEffect.buffer());
            glDrawArraysInstanced(GL_TRIANGLES, 0, 9, particleEffect.count());
        }
    }

    @MainThread
    private void renderPlaceableHologram(Position cameraPosition, Matrix4f projectionViewMatrix) {
        Target currentTarget = Target.getPlayerTarget();
        Target lockedTarget = player.getInteractionHandler().getLockedTarget();
        Target startTarget = player.getInteractionHandler().getStartTarget();
        PlacingState state = player.getInteractionHandler().getState(currentTarget);

        if (!state.shouldRender()) return;
        switch (state) {
            case REPEAT -> renderRepeatVolumeIndicator(cameraPosition, projectionViewMatrix, startTarget, currentTarget, player.getHeldPlaceable());
            case REPEAT_LOCKED -> renderRepeatVolumeIndicator(cameraPosition, projectionViewMatrix, startTarget, lockedTarget, player.getHeldPlaceable());

            case STRUCTURE_SELECT, SHAPE ->
                    renderRepeatVolumeIndicator(cameraPosition, projectionViewMatrix, startTarget == null ? currentTarget : startTarget, currentTarget, player.getHeldPlaceable());
            case STRUCTURE_SELECT_LOCKED ->
                    renderRepeatVolumeIndicator(cameraPosition, projectionViewMatrix, startTarget == null ? lockedTarget : startTarget, lockedTarget, player.getHeldPlaceable());

            case STRUCTURE_PLACE -> renderStructureVolumeIndicator(cameraPosition, projectionViewMatrix, currentTarget);
            case STRUCTURE_PLACE_LOCKED -> renderStructureVolumeIndicator(cameraPosition, projectionViewMatrix, lockedTarget);

            case CAPSULE -> renderCapsuleVolumeIndicator(cameraPosition, projectionViewMatrix, startTarget, currentTarget);
            case CAPSULE_LOCKED -> renderCapsuleVolumeIndicator(cameraPosition, projectionViewMatrix, startTarget, lockedTarget);
        }
    }

    @MainThread
    private void renderCapsuleVolumeIndicator(Position cameraPosition, Matrix4f projectionViewMatrix, Target startTarget, Target endTarget) {
        if (startTarget == null || endTarget == null) return;

        CapsulePlaceable placeable = (CapsulePlaceable) player.getHeldPlaceable();
        byte material = !Input.isKeyPressed(KeySettings.SPRINT)
                && OptionSettings.PLACE_MODE.value() != PlaceMode.BREAK_HELD_ONLY ? placeable.getMaterial() : AIR;

        setupHologramRendering();

        Vector3l startPosition = startTarget.offsetPosition(), endPosition = endTarget.offsetPosition();
        CapsulePlaceable.offsetPositions(startPosition, endPosition);
        Vector3l minPosition = Utils.min(startPosition, endPosition);

        placeable.setStartEndPositions(startPosition, endPosition);
        synchronizeHologramModel(placeable);

        renderHologram(cameraPosition, projectionViewMatrix, minPosition.sub(placeable.getRadius(), placeable.getRadius(), placeable.getRadius()),
                new Matrix4f(), new int[]{NORTH, TOP, WEST, SOUTH, BOTTOM, EAST}, material,
                placeable.getLengthX(), placeable.getLengthY(), placeable.getLengthZ(), 1, 1, 1);
    }

    @MainThread
    private void renderStructureVolumeIndicator(Position cameraPosition, Matrix4f projectionViewMatrix, Target target) {
        StructurePlaceable placeable = (StructurePlaceable) player.getHeldPlaceable();
        Vector3l position = target.offsetPosition();
        placeable.offsetPosition(position, target.side());
        synchronizeHologramModel(placeable);

        setupHologramRendering();
        glDisable(GL_CULL_FACE);

        renderHologram(cameraPosition, projectionViewMatrix, position, placeable.getModelMatrix(), placeable.getSideTransform(), OUT_OF_WORLD,
                hologramSize, hologramSize, hologramSize, 1, 1, 1);
    }

    @MainThread
    private void renderRepeatVolumeIndicator(Position cameraPosition, Matrix4f projectionViewMatrix, Target startTarget, Target currentTarget, Placeable placeable) {
        byte material = placeable instanceof ShapePlaceable shapePlaceable
                && !Input.isKeyPressed(KeySettings.SPRINT)
                && OptionSettings.PLACE_MODE.value() != PlaceMode.BREAK_HELD_ONLY ? shapePlaceable.getMaterial() : AIR;

        Vector3l startPosition = material == AIR ? startTarget.position() : startTarget.offsetPosition();
        Vector3l endPosition = material == AIR ? currentTarget.position() : currentTarget.offsetPosition();

        RepeatPlaceable.offsetPositions(startPosition, endPosition, startTarget.side(), placeable);
        Vector3l minPosition = Utils.min(startPosition, endPosition);
        Vector3l maxPosition = Utils.max(startPosition, endPosition);
        maxPosition.add(1, 1, 1);

        setupHologramRendering();

        if (placeable instanceof ShapePlaceable shapePlaceable) {
            synchronizeHologramModel(shapePlaceable.updateBitMap(false));

            int countX = (int) (maxPosition.x - minPosition.x) / placeable.getLengthX();
            int countY = (int) (maxPosition.y - minPosition.y) / placeable.getLengthY();
            int countZ = (int) (maxPosition.z - minPosition.z) / placeable.getLengthZ();

            renderHologram(cameraPosition, projectionViewMatrix, minPosition, new Matrix4f(), new int[]{NORTH, TOP, WEST, SOUTH, BOTTOM, EAST}, material,
                    placeable.getLengthX(), placeable.getLengthY(), placeable.getLengthZ(), countX, countY, countZ);
        } else {
            Shader shader = AssetManager.get(Shaders.AABB_INDICATOR);
            shader.bind();
            shader.setUniform("iCameraPosition",
                    cameraPosition.longX & ~CHUNK_SIZE_MASK,
                    cameraPosition.longY & ~CHUNK_SIZE_MASK,
                    cameraPosition.longZ & ~CHUNK_SIZE_MASK);
            shader.setUniform("projectionViewMatrix", projectionViewMatrix);
            shader.setUniform("minPosition", minPosition);
            shader.setUniform("maxPosition", maxPosition);

            glDrawArrays(GL_TRIANGLES, 0, 36);
        }
    }

    @MainThread
    private static void setupHologramRendering() {
        TextureArray materialsTexture = AssetManager.get(TextureArrays.MATERIALS);
        glEnable(GL_DEPTH_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glDisable(GL_STENCIL_TEST);
        glDepthMask(true);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, materialsTexture.id());
    }

    @MainThread
    private void renderHologram(Position cameraPosition, Matrix4f projectionViewMatrix, Vector3l startPosition, Matrix4f modelMatrix,
                                int[] sideTransform, byte material,
                                int lengthX, int lengthY, int lengthZ,
                                int countX, int countY, int countZ) {
        TextureArray materialsTexture = AssetManager.get(TextureArrays.MATERIALS);
        Shader shader = AssetManager.get(Shaders.VOLUME_INDICATOR);
        shader.bind();
        shader.setUniform("iCameraPosition",
                cameraPosition.longX & ~CHUNK_SIZE_MASK,
                cameraPosition.longY & ~CHUNK_SIZE_MASK,
                cameraPosition.longZ & ~CHUNK_SIZE_MASK);
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
        shader.setUniform("modelMatrix", modelMatrix);
        shader.setUniform("sideTransform", sideTransform);
        shader.setUniform("instanceCount", countX, countY, countZ);
        shader.setUniform("instanceSize", lengthX, lengthY, lengthZ);
        shader.setUniform("startPosition", startPosition);

        shader.setUniform("textures", 0);
        shader.setUniform("textureSizes", materialsTexture.textureSizes());
        shader.setUniform("maxTextureSize", materialsTexture.maxTextureSize());
        shader.setUniform("material", material);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, opaqueHologram.bufferOrStart());
        glDrawArraysInstanced(GL_TRIANGLES, 0, opaqueHologram.vertexCountSum(), countX * countY * countZ);
    }

    @MainThread
    private void renderChat() {
        long currentTime = System.nanoTime();

        ChatTextField chatTextField = firstChildOf(ChatTextField.class);
        Vector2f defaultTextSize = ((FontOption) CoreOptionSettings.FONT.value()).getDefaultTextSize();
        Vector2f position = new Vector2f();
        TextShader shader = (TextShader) AssetManager.get(CoreShaders.TEXT);
        shader.bind();
        float lineSeparation = defaultTextSize.y * CoreFloatSettings.TEXT_SIZE.value();
        float chatMessageDuration = FloatSettings.CHAT_MESSAGE_DURATION.value();
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

    @MainThread
    private void renderDebugInfo() {
        boolean debugScreenOpen = ToggleSettings.OPEN_DEBUG_MENU.value();
        int textLine = 0;
        for (DebugScreenLine debugLine : debugLines) if (debugLine.shouldShow(debugScreenOpen)) debugLine.render(++textLine);
    }

    @MainThread
    private void renderOccluders(Position cameraPositon, Matrix4f projectionViewMatrix) {
        int lod = IntSettings.DEBUG_VISUALIZATION_LOD.value();
        if (lod < 0 || lod >= Game.getWorld().LOD_COUNT) return;

        Shader shader = AssetManager.get(Shaders.AABB_INDICATOR);
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

    @MainThread
    private void renderOccludees(Position cameraPositon, Matrix4f projectionViewMatrix) {
        int lod = IntSettings.DEBUG_VISUALIZATION_LOD.value();
        if (lod < 0 || lod >= Game.getWorld().LOD_COUNT) return;

        Shader shader = AssetManager.get(Shaders.AABB_INDICATOR);
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

    @MainThread
    private static void renderDebugTexture2D(int texture) {
        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI);
        shader.bind();
        shader.flipNextDrawVertically();
        glDisable(GL_BLEND);
        shader.drawQuad(new Vector2f(0.0F, 0.0F), new Vector2f(0.5F, 0.5F), new Texture2D(texture));
    }

    @MainThread
    private static void renderDebugTexture3D(int texture, int layer) {
        GuiShader shader = (GuiShader) AssetManager.get(Shaders.TEXTURE_LAYER);
        shader.bind();
        shader.setUniform("layer", layer);
        shader.flipNextDrawVertically();
        glDisable(GL_BLEND);
        shader.drawQuad(new Vector2f(0.0F, 0.0F), new Vector2f(0.5F, 0.5F), new Texture3D(texture, GL_TEXTURE_2D_ARRAY));
    }

    @MainThread
    private static void renderVolume(Shader shader, Chunk chunk, AABB aabb, int lod) {
        if (aabb.maxX < aabb.minX || aabb.maxY < aabb.minY || aabb.maxZ < aabb.minZ) return;

        long x = chunk.X << CHUNK_SIZE_BITS + lod;
        long y = chunk.Y << CHUNK_SIZE_BITS + lod;
        long z = chunk.Z << CHUNK_SIZE_BITS + lod;

        shader.setUniform("minPosition", x + ((long) aabb.minX << lod), y + ((long) aabb.minY << lod), z + ((long) aabb.minZ << lod));
        shader.setUniform("maxPosition", x + ((long) aabb.maxX << lod), y + ((long) aabb.maxY << lod), z + ((long) aabb.maxZ << lod));

        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    @MainThread
    private static void setUpVolumeRendering(Position cameraPositon, Matrix4f projectionViewMatrix, Shader shader) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_STENCIL_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_BLEND);
        glDepthMask(false);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, AssetManager.get(TextureArrays.MATERIALS).id());

        shader.setUniform("iCameraPosition",
                cameraPositon.longX & ~CHUNK_SIZE_MASK,
                cameraPositon.longY & ~CHUNK_SIZE_MASK,
                cameraPositon.longZ & ~CHUNK_SIZE_MASK);
        shader.setUniform("projectionViewMatrix", projectionViewMatrix);
    }


    @MainThread
    private void createTextures(int width, int height) {
        colorTexture = CoreObjectLoader.createTexture2D(GL_RGBA8, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST);
        intPosTexture = CoreObjectLoader.createTexture2D(GL_RGBA16I, width, height, GL_RGBA_INTEGER, GL_SHORT, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new int[]{1000, 1000, 1000, 6});

        depthTexture = CoreObjectLoader.createTexture2D(GL_DEPTH32F_STENCIL8, width, height, GL_DEPTH_STENCIL, GL_FLOAT_32_UNSIGNED_INT_24_8_REV, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{0, 0, 0, 0});

        accumulationTexture = CoreObjectLoader.createTexture2D(GL_RGBA16F, width, height, GL_RGBA, GL_HALF_FLOAT, GL_NEAREST);
        revealTexture = CoreObjectLoader.createTexture2D(GL_R8, width, height, GL_RED, GL_FLOAT, GL_NEAREST);
        createShadowTextures();
    }

    @MainThread
    private void createFrameBuffers() {
        framebuffer = glCreateFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, intPosTexture, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));

        transparencyFramebuffer = glCreateFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, transparencyFramebuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, accumulationTexture, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, revealTexture, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0);
        glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1});
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Transparency buffer not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));

        createShadowFrameBuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @MainThread
    private void createShadowTextures() {
        int shadowCascades = IntSettings.SHADOW_CASCADES_COUNT.value();

        shadowColorTexture = CoreObjectLoader.createTexture2DArray(GL_RGB8, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, shadowCascades, GL_RGB, GL_UNSIGNED_BYTE, GL_NEAREST);
        shadowTexture = CoreObjectLoader.createTexture2DArray(GL_DEPTH_COMPONENT32F, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, shadowCascades, GL_DEPTH_COMPONENT, GL_FLOAT, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_BORDER);
        glTexParameterfv(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BORDER_COLOR, new float[]{0, 0, 0, 0});
    }

    @MainThread
    private void createShadowFrameBuffers() {
        int shadowCascades = IntSettings.SHADOW_CASCADES_COUNT.value();
        shadowFramebuffers = new int[shadowCascades];
        sunMatrices = new Matrix4f[shadowCascades];
        shadowSnapshotPositions = new Position[shadowCascades];

        for (int index = 0; index < shadowCascades; index++) {
            sunMatrices[index] = new Matrix4f();
            shadowFramebuffers[index] = glCreateFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebuffers[index]);
            glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTexture, 0, index);
            glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, shadowColorTexture, 0, index);
            glDrawBuffers(new int[]{GL_COLOR_ATTACHMENT0});
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Shadow Frame buffer " + index + " not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));
        }
    }

    @MainThread
    private void deleteTextures() {
        glDeleteTextures(colorTexture);
        glDeleteTextures(depthTexture);
        glDeleteTextures(intPosTexture);
        glDeleteTextures(shadowTexture);
        glDeleteTextures(shadowColorTexture);
        glDeleteTextures(accumulationTexture);
        glDeleteTextures(revealTexture);
    }

    @MainThread
    private void deleteFrameBuffers() {
        glDeleteFramebuffers(framebuffer);
        glDeleteFramebuffers(shadowFramebuffers);
        glDeleteFramebuffers(transparencyFramebuffer);
    }

    @MainThread
    private void synchronizeHologramModel(Placeable placeable) {
        int preferredSize = placeable.getPreferredSizePowOf2();
        int hologramHash = placeable.hashCode();
        if (!hologramModelsValid || hologramSize != preferredSize || this.hologramHash != hologramHash) {
            if (opaqueHologram != null) opaqueHologram.delete();

            Structure structure = placeable.getStructure();
            Mesh mesh = new MeshGenerator().generateMesh(structure);
            opaqueHologram = ObjectLoader.loadCombinedModel(mesh);
            hologramSize = preferredSize;
            this.hologramHash = hologramHash;

            hologramModelsValid = true;
        }
    }

    @MainThread
    private static int getFlags(Position cameraPosition) {
        boolean headUnderWater = Game.getWorld().getMaterial(cameraPosition.longX, cameraPosition.longY, cameraPosition.longZ) == WATER;
        boolean useShadowMapping = ToggleSettings.USE_SHADOW_MAPPING.value();
        boolean doGlassShadows = ToggleSettings.GLASS_CASTS_SHADOWS.value();
        return (doGlassShadows ? DO_GLASS_SHADOWS_BIT : 0) | (useShadowMapping ? DO_SHADOW_MAPPING_BIT : 0) | (headUnderWater ? HEAD_UNDER_WATER_BIT : 0);
    }

    private static Vector3f getSunDirection(float renderTime) {
        final float downwardsSunPart = FloatSettings.DOWNWARD_SUN_DIRECTION.value();
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
    private RenderingOptimizer renderingOptimizer;
    private final ArrayList<Long> frameTimes = new ArrayList<>();
    private final ArrayList<DebugScreenLine> debugLines;
    private final ArrayList<Renderable> hudElements = new ArrayList<>();
    private final UiElement crosshair;
    private final Player player;
    private double animationTimer = 0;

    private Position lastCameraPosition;
    private Matrix4f lastProjectionViewMatrix;
    private OpaqueModel opaqueHologram;
    private boolean hologramModelsValid = false;
    private int hologramSize, hologramHash;

    private int framebuffer, colorTexture, depthTexture, intPosTexture;
    private int shadowTexture, shadowColorTexture, currentShadowIndex = 0;
    private int[] shadowFramebuffers;
    private Matrix4f[] sunMatrices;
    private Position[] shadowSnapshotPositions;
    private int transparencyFramebuffer, accumulationTexture, revealTexture;

    private static final int HEAD_UNDER_WATER_BIT = 1;
    private static final int DO_SHADOW_MAPPING_BIT = 2;
    private static final int DO_GLASS_SHADOWS_BIT = 4;
}
