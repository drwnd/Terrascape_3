package core.rendering_api.shaders;

import game.assets.Shaders;

public class GuiBackgroundShader extends GuiShader {
    public GuiBackgroundShader(String vertexShaderFilePath, String fragmentShaderFilePath, Shaders identifier) {
        super(vertexShaderFilePath, fragmentShaderFilePath, identifier);
    }
}
