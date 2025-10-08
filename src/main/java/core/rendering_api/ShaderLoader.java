package core.rendering_api;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.Shader;
import core.assets.CoreShaders;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.TextShader;

import game.assets.Shaders;

public final class ShaderLoader {

    private ShaderLoader() {
    }


    public static Shader loadShader(ShaderIdentifier identifier) {
        return switch (identifier) {
            case CoreShaders.GUI -> getGuiShader();
            case CoreShaders.GUI_BACKGROUND -> getGuiBackgroundShader();
            case CoreShaders.TEXT -> getTextShader();
            case Shaders.OPAQUE -> new Shader("assets/shaders/Material.vert", "assets/shaders/Opaque.frag", identifier);
            case Shaders.SKYBOX -> new Shader("assets/shaders/Skybox.vert", "assets/shaders/Skybox.frag", identifier);
            case Shaders.WATER -> new Shader("assets/shaders/Material.vert", "assets/shaders/Water.frag", identifier);
            case Shaders.GLASS -> new Shader("assets/shaders/Material.vert", "assets/shaders/Glass.frag", identifier);
            default -> throw new IllegalStateException("Unexpected value: " + identifier);
        };
    }


    private static Shader getGuiShader() {
        return new GuiShader("assets/shaders/Gui.vert", "assets/shaders/Gui.frag", CoreShaders.GUI);
    }

    private static Shader getGuiBackgroundShader() {
        return new GuiShader("assets/shaders/Gui.vert", "assets/shaders/GuiBackground.frag", CoreShaders.GUI_BACKGROUND);
    }

    private static Shader getTextShader() {
        return new TextShader("assets/shaders/Text.vert", "assets/shaders/Text.frag", CoreShaders.TEXT);
    }
}
