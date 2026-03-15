package core.rendering_api;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.Shader;
import core.assets.CoreShaders;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.TextShader;

public final class CoreShaderLoader {

    private CoreShaderLoader() {
    }


    public static Shader loadShader(ShaderIdentifier identifier) {
        return switch (identifier) {
            case CoreShaders.GUI -> new GuiShader("Gui.vert", "Gui.frag", identifier);
            case CoreShaders.GUI_BACKGROUND -> new GuiShader("Gui.vert", "GuiBackground.frag", identifier);
            case CoreShaders.TEXT -> new TextShader("Text.vert", "Text.frag", identifier);
            default -> throw new IllegalStateException("Unexpected value: " + identifier);
        };
    }
}
