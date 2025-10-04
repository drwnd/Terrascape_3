package game.menus;

import core.assets.AssetManager;
import core.assets.Texture;
import core.languages.UiMessage;
import core.renderables.Renderable;
import core.renderables.TextElement;
import core.renderables.UiButton;
import core.rendering_api.Window;
import core.rendering_api.shaders.GuiShader;
import core.settings.FloatSetting;
import core.assets.CoreShaders;

import game.server.Game;
import game.player.rendering.ObjectLoader;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL46;

public final class PauseMenu extends Renderable {
    public PauseMenu() {
        super(new Vector2f(1.0f, 1.0f), new Vector2f(0.0f, 0.0f));

        Vector2f sizeToParent = new Vector2f(0.6f, 0.1f);

        UiButton quitButton = new UiButton(sizeToParent, new Vector2f(0.2f, 0.3f), getQuitButtonAction());
        quitButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), UiMessage.QUIT_WORLD));

        UiButton settingsButton = new UiButton(sizeToParent, new Vector2f(0.2f, 0.45f), getSettingsButtonAction());
        settingsButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), UiMessage.SETTINGS));

        UiButton playButton = new UiButton(sizeToParent, new Vector2f(0.2f, 0.6f), getPlayButtonAction());
        playButton.addRenderable(new TextElement(new Vector2f(0.05f, 0.5f), UiMessage.CONTINUE_PLAYING));

        addRenderable(quitButton);
        addRenderable(settingsButton);
        addRenderable(playButton);

        int backGroundWidth = Math.max(1, (int) (Window.getWidth() / (1.0f + FloatSetting.PAUSE_MENU_BACKGROUND_BLUR.value())));
        int backGroundHeight = Math.max(1, (int) (Window.getHeight() / (1.0f + FloatSetting.PAUSE_MENU_BACKGROUND_BLUR.value())));

        int backGround = ObjectLoader.createTexture2D(GL46.GL_RGB, backGroundWidth, backGroundHeight, GL46.GL_RGB, GL46.GL_UNSIGNED_BYTE, GL46.GL_LINEAR);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_R, GL46.GL_CLAMP_TO_EDGE);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_S, GL46.GL_CLAMP_TO_EDGE);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_WRAP_T, GL46.GL_CLAMP_TO_EDGE);
        int frameBuffer = GL46.glCreateFramebuffers();

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, frameBuffer);
        GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, backGround, 0);
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER)));

        GL46.glBlitNamedFramebuffer(0, frameBuffer, 0, 0, Window.getWidth(), Window.getHeight(), 0, 0, backGroundWidth, backGroundHeight, GL46.GL_COLOR_BUFFER_BIT, GL46.GL_LINEAR);
        this.backGround = new Texture(backGround);

        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0);
        GL46.glDeleteFramebuffers(frameBuffer);
    }

    @Override
    public void setOnTop() {
        Window.setInput(new PauseMenuInput(this));
        Game.getServer().pauseTicks();
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        GuiShader shader = (GuiShader) AssetManager.getShader(CoreShaders.GUI);
        shader.bind();

        shader.flipNextDrawVertically();
        shader.drawQuadNoGuiScale(position, size, backGround);
    }

    @Override
    public void deleteSelf() {
        backGround.delete();
    }


    private static Runnable getQuitButtonAction() {
        return Game::quit;
    }

    private static Runnable getSettingsButtonAction() {
        return () -> Window.pushRenderable(new SettingsMenu());
    }

    private static Runnable getPlayButtonAction() {
        return () -> {
            Window.popRenderable();
            Game.getServer().startTicks();
            Game.getPlayer().setInput();
        };
    }

    private final Texture backGround;
}
