package core.rendering_api;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.GuiBackgroundShader;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.Shader;
import core.rendering_api.shaders.TextShader;

public final class ShaderLoader {

    private ShaderLoader() {
    }


    public static Shader loadShader(ShaderIdentifier identifier) {
        return switch (identifier) {
            case GUI -> getGuiShader();
            case GUI_BACKGROUND -> getGuiBackgroundShader();
            case TEXT -> getTextShader();
            case OPAQUE -> new Shader("assets/shaders/Material.vert", "assets/shaders/Opaque.frag", identifier);
            case SKYBOX -> new Shader("assets/shaders/Skybox.vert", "assets/shaders/Skybox.frag", identifier);
            case WATER -> new Shader("assets/shaders/Material.vert", "assets/shaders/Water.frag", identifier);
        };
    }


    private static Shader getGuiShader() {
        return new GuiShader("assets/shaders/Gui.vert", "assets/shaders/Gui.frag", ShaderIdentifier.GUI);
    }

    private static Shader getGuiBackgroundShader() {
        return new GuiBackgroundShader("assets/shaders/Gui.vert", "assets/shaders/GuiBackground.frag", ShaderIdentifier.GUI_BACKGROUND);
    }

    private static Shader getTextShader() {
        return new TextShader("assets/shaders/Text.vert", "assets/shaders/Text.frag", ShaderIdentifier.TEXT);
    }
}
