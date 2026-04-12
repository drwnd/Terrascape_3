package game.assets;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.Shader;

public enum ComputeShaders implements ShaderIdentifier {

    ARC,
    CONE,
    CUBE,
    CYLINDER,
    ELLIPSOID,
    INSIDE_ARC,
    INSIDE_STAIR,
    OUTSIDE_ARC,
    OUTSIDE_STAIR,
    SLAB,
    SPHERE,
    STAIR;

    @Override
    public Shader generateAsset() {
        return ShaderLoader.loadShader(this);
    }
}
