package core.rendering_api.shaders;

import core.assets.identifiers.ShaderIdentifier;

public class GuiBackgroundShader extends GuiShader {
    public GuiBackgroundShader(String vertexShaderFilePath, String fragmentShaderFilePath, ShaderIdentifier identifier) {
        super(vertexShaderFilePath, fragmentShaderFilePath, identifier);
    }
}
