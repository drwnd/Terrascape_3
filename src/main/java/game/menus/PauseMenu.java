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

import static org.lwjgl.opengl.GL46.*;

public final class PauseMenu extends Renderable {

    public PauseMenu() {
        super(new Vector2f(1.0F, 1.0F), new Vector2f(0.0F, 0.0F));

        Vector2f sizeToParent = new Vector2f(0.6F, 0.1F);

        UiButton quitButton = new UiButton(sizeToParent, new Vector2f(0.2F, 0.3F), getQuitButtonAction());
        quitButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.QUIT_WORLD));

        UiButton settingsButton = new UiButton(sizeToParent, new Vector2f(0.2F, 0.45F), getSettingsButtonAction());
        settingsButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.SETTINGS));

        UiButton playButton = new UiButton(sizeToParent, new Vector2f(0.2F, 0.6F), getPlayButtonAction());
        playButton.addRenderable(new TextElement(new Vector2f(0.05F, 0.5F), UiMessage.CONTINUE_PLAYING));

        addRenderable(quitButton);
        addRenderable(settingsButton);
        addRenderable(playButton);

        int backGroundWidth = Math.max(1, (int) (Window.getWidth() / (1.0F + FloatSetting.PAUSE_MENU_BACKGROUND_BLUR.value())));
        int backGroundHeight = Math.max(1, (int) (Window.getHeight() / (1.0F + FloatSetting.PAUSE_MENU_BACKGROUND_BLUR.value())));

        int backGround = ObjectLoader.createTexture2D(GL_RGB, backGroundWidth, backGroundHeight, GL_RGB, GL_UNSIGNED_BYTE, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        int frameBuffer = glCreateFramebuffers();

        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, backGround, 0);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Frame buffer not complete. status " + Integer.toHexString(glCheckFramebufferStatus(GL_FRAMEBUFFER)));

        glBlitNamedFramebuffer(0, frameBuffer, 0, 0, Window.getWidth(), Window.getHeight(), 0, 0, backGroundWidth, backGroundHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);
        this.backGround = new Texture(backGround);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(frameBuffer);
    }

    @Override
    public void setOnTop() {
        Window.setInput(new PauseMenuInput(this));
        Game.getServer().pauseTicks();
    }

    @Override
    public void renderSelf(Vector2f position, Vector2f size) {
        GuiShader shader = (GuiShader) AssetManager.get(CoreShaders.GUI);
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
        };
    }

    private final Texture backGround;
}
