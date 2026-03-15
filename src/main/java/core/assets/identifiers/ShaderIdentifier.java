package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.rendering_api.CoreShaderLoader;
import core.rendering_api.shaders.Shader;

public interface ShaderIdentifier extends AssetIdentifier<Shader> {

    @Override
    default AssetGenerator<Shader> getAssetGenerator() {
        return () -> CoreShaderLoader.loadShader(this);
    }
}
