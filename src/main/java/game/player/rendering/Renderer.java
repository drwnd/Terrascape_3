package game.player.rendering;

import core.assets.AssetManager;
import core.assets.CoreShaders;
import core.rendering_api.Input;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.Shader;
import core.rendering_api.shaders.TextShader;
import core.rendering_api.Window;
import core.settings.FloatSetting;
import core.settings.KeySetting;
import core.settings.OptionSetting;
import core.settings.optionSettings.FontOption;
import core.settings.ToggleSetting;
import core.renderables.Renderable;
import core.renderables.UiElement;

import game.assets.Shaders;
import game.assets.TextureArrays;
import game.assets.Textures;
import game.assets.VertexArrays;
import game.player.ChatTextField;
import game.player.Player;
import game.server.ChatMessage;
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

import java.awt.*;
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

    public float getRenderTime() {
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

        GL46.glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID()); // Just bind something IDK
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

        GL46.glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID()); // Just bind something IDK
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

        GL46.glBindVertexArray(AssetManager.get(VertexArrays.SKYBOX).getID()); // Just bind something IDK
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
        if (!Input.isKeyPressed(KeySetting.SKIP_COMPUTING_VISIBILITY)) renderingOptimizer.computeVisibility(player);

        Matrix4f projectionViewMatrix = Transformation.getProjectionViewMatrix(camera);
        Position cameraPosition = player.getCamera().getPosition();

        setupRenderState();

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer);
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT | GL46.GL_STENCIL_BUFFER_BIT);

        renderSkybox(camera);
        renderOpaqueGeometry(cameraPosition, projectionViewMatrix);

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, ssaoFramebuffer);
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT);
        GL46.glDisable(GL46.GL_STENCIL_TEST);

        computeAmbientOcclusion();

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, framebuffer);

        applyAmbientOcclusion();

        renderWater(cameraPosition, projectionViewMatrix);
        renderGlass(cameraPosition, projectionViewMatrix);

        GL46.glDisable(GL46.GL_STENCIL_TEST);
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0);
        GL46.glBlitNamedFramebuffer(framebuffer, 0,
                0, 0, Window.getWidth(), Window.getHeight(),
                0, 0, Window.getWidth(), Window.getHeight(),
                GL46.GL_COLOR_BUFFER_BIT, GL46.GL_NEAREST);
        GL46.glPolygonMode(GL46.GL_FRONT_AND_BACK, GL46.GL_FILL);

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

        float[] noise = new float[4 * 4 * 3];
        for (int i = 0; i < noise.length; i += 3) {
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

    private void renderOpaqueGeometry(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedOpaqueModels = 0;

        int cameraChunkX = cameraPosition.intX >> CHUNK_SIZE_BITS;
        int cameraChunkY = cameraPosition.intY >> CHUNK_SIZE_BITS;
        int cameraChunkZ = cameraPosition.intZ >> CHUNK_SIZE_BITS;

        Shader shader = AssetManager.get(Shaders.OPAQUE);
        setupOpaqueRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ, getRenderTime());
        shader.setUniform("cameraPosition", cameraPosition.getInChunkPosition());
        shader.setUniform("flags", getFlags(cameraPosition));

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            GL46.glStencilFunc(GL46.GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            long[] lodVisibilityBits = renderingOptimizer.getVisibilityBits()[lod];

            for (OpaqueModel model : player.getMeshCollector().getOpaqueModels(lod)) {
                if (model == null || model.isEmpty() || isInvisible(model.chunkX(), model.chunkY(), model.chunkZ(), lod, lodVisibilityBits)) continue;
                int[] toRenderVertexCounts = model.getVertexCounts(cameraChunkX, cameraChunkY, cameraChunkZ);

                GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
                shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());

                GL46.glMultiDrawArrays(GL46.GL_TRIANGLES, model.getIndices(), toRenderVertexCounts);
                renderedOpaqueModels++;
            }
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

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            GL46.glStencilFunc(GL46.GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            long[] lodVisibilityBits = renderingOptimizer.getVisibilityBits()[lod];

            for (TransparentModel model : player.getMeshCollector().getTransparentModels(lod)) {
                if (model == null || model.isWaterEmpty() || isInvisible(model.chunkX(), model.chunkY(), model.chunkZ(), lod, lodVisibilityBits)) continue;

                GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
                shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());

                GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.waterVertexCount() * (6 / 2));
                renderedWaterModels++;
            }
        }
    }

    private void renderGlass(Position cameraPosition, Matrix4f projectionViewMatrix) {
        renderedGlassModels = 0;

        Shader shader = AssetManager.get(Shaders.GLASS);
        setUpGlassRendering(shader, projectionViewMatrix, cameraPosition.intX, cameraPosition.intY, cameraPosition.intZ);

        for (int lod = 0; lod < LOD_COUNT; lod++) {
            GL46.glStencilFunc(GL46.GL_GEQUAL, LOD_COUNT - lod, 0xFF);
            long[] lodVisibilityBits = renderingOptimizer.getVisibilityBits()[lod];

            for (TransparentModel model : player.getMeshCollector().getTransparentModels(lod)) {
                if (model == null || model.isGlassEmpty() || isInvisible(model.chunkX(), model.chunkY(), model.chunkZ(), lod, lodVisibilityBits)) continue;

                GL46.glBindBufferBase(GL46.GL_SHADER_STORAGE_BUFFER, 0, model.verticesBuffer());
                shader.setUniform("worldPos", model.totalX(), model.totalY(), model.totalZ(), 1 << model.LOD());
                shader.setUniform("indexOffset", model.waterVertexCount() >> 1);

                GL46.glDrawArrays(GL46.GL_TRIANGLES, 0, model.glassVertexCount() * (6 / 2));
                renderedGlassModels++;
            }
        }
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

    private static boolean isInvisible(int chunkX, int chunkY, int chunkZ, int lod, long[] visibilityBits) {
        int index = Utils.getChunkIndex(chunkX, chunkY, chunkZ, lod);
        return (visibilityBits[index >> 6] & 1L << index) == 0;
    }

    private int getFlags(Position cameraPosition) {
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

    private boolean debugScreenOpen = false;
    private ArrayList<ChatMessage> messages = new ArrayList<>();
    private final ArrayList<Long> frameTimes = new ArrayList<>();
    private final ArrayList<DebugScreenLine> debugLines;
    private final UiElement crosshair;
    private final RenderingOptimizer renderingOptimizer = new RenderingOptimizer();
    private final Player player;

    private int framebuffer, ssaoFramebuffer;
    private int colorTexture, depthTexture, ssaoTexture, noiseTexture, sideTexture;
}
