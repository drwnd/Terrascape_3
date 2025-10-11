package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.rendering_api.ShaderLoader;
import core.rendering_api.shaders.Shader;

public interface ShaderIdentifier extends AssetIdentifier<Shader> {

    @Override
    default AssetGenerator<Shader> getAssetGenerator() {
        return () -> ShaderLoader.loadShader(this);
    }
}
