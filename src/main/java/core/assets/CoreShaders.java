package core.assets;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.CoreShaderLoader;
import core.rendering_api.shaders.Shader;

public enum CoreShaders implements ShaderIdentifier {

    GUI, GUI_BACKGROUND, TEXT;

    @Override
    public Shader generateAsset() {
        return CoreShaderLoader.loadShader(this);
    }
}
