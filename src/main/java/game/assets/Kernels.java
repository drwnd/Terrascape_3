package game.assets;

import core.assets.Kernel;
import core.assets.identifiers.KernelIdentifier;

public enum Kernels implements KernelIdentifier {

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
    public Kernel generateAsset() {
        return ShaderLoader.loadKernel(this);
    }
}
