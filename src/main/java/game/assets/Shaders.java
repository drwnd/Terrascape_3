package game.assets;

import core.assets.AssetGenerator;
import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.Shader;

public enum Shaders implements ShaderIdentifier {

    OPAQUE_GEOMETRY,
    SKYBOX,
    WATER,
    GLASS,
    SSAO,
    AO_APPLIER,
    OPAQUE_PARTICLE,
    GLASS_PARTICLE,
    VOLUME_INDICATOR,
    AABB,
    OCCLUSION_CULLING,
    CHUNK_SHADOW,
    PARTICLE_SHADOW;

    @Override
    public AssetGenerator<Shader> getAssetGenerator() {
        return () -> ShaderLoader.loadShader(this);
    }
}
