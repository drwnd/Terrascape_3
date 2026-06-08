package game.assets;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.Shader;

public enum Shaders implements ShaderIdentifier {

    OPAQUE_GEOMETRY,
    SKYBOX,
    TRANSPARENT,
    GLASS,
    SSAO,
    OPAQUE_PARTICLE,
    GLASS_PARTICLE,
    AABB_INDICATOR,
    AABB,
    OCCLUSION_CULLING,
    CHUNK_SHADOW,
    PARTICLE_SHADOW,
    VOLUME_INDICATOR,
    TRANSPARENCY_APPLIER;

    @Override
    public Shader generateAsset() {
        return ShaderLoader.loadShader(this);
    }
}
