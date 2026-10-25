package core.assets;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.CoreShaderLoader;
import core.rendering_api.shaders.Shader;
import core.utils.MainThread;

public enum CoreShaders implements ShaderIdentifier {

    GUI, GUI_BACKGROUND, TEXT;

    @Override
    @MainThread
    public Shader generateAsset() {
        return CoreShaderLoader.loadShader(this);
    }
}
