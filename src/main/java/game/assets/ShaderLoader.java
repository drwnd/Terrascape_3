package game.assets;

import core.assets.Kernel;
import core.assets.identifiers.KernelIdentifier;
import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.RenderShader;
import core.rendering_api.shaders.Shader;

public final class ShaderLoader {

    public static Shader loadShader(ShaderIdentifier identifier) {
        return switch (identifier) {
            case Shaders.OPAQUE_GEOMETRY -> new RenderShader("Material.vert", "Opaque.frag", identifier);
            case Shaders.SKYBOX -> new RenderShader("Skybox.vert", "Skybox.frag", identifier);
            case Shaders.WATER -> new RenderShader("Material.vert", "Water.frag", identifier);
            case Shaders.GLASS -> new RenderShader("Material.vert", "Glass.frag", identifier);
            case Shaders.SSAO -> new GuiShader("Gui.vert", "SSAO.frag", identifier);
            case Shaders.AO_APPLIER -> new GuiShader("Gui.vert", "AO_Applier.frag", identifier);
            case Shaders.OPAQUE_PARTICLE -> new RenderShader("Particle.vert", "Opaque.frag", identifier);
            case Shaders.GLASS_PARTICLE -> new RenderShader("Particle.vert", "Glass.frag", identifier);
            case Shaders.AABB_INDICATOR -> new RenderShader("AABBIndicator.vert", "AABBIndicator.frag", identifier);
            case Shaders.AABB -> new RenderShader("AABB.vert", "Null.frag", identifier);
            case Shaders.OCCLUSION_CULLING -> new RenderShader("AABB.vert", "OcclusionCulling.frag", identifier);
            case Shaders.CHUNK_SHADOW -> new RenderShader("Material.vert", "Shadow.frag", identifier);
            case Shaders.PARTICLE_SHADOW -> new RenderShader("Particle.vert", "Shadow.frag", identifier);
            case Shaders.VOLUME_INDICATOR -> new RenderShader("StructureHologram.vert", "StructureHologram.frag", identifier);
            default -> throw new IllegalStateException("Unexpected value: " + identifier);
        };
    }

    public static Kernel loadKernel(KernelIdentifier identifier) {
        return switch (identifier) {
            case Kernels.ARC -> new Kernel("shapeKernels/Arc.comp");
            case Kernels.CONE -> new Kernel("shapeKernels/Cone.comp");
            case Kernels.CUBE -> new Kernel("shapeKernels/Cube.comp");
            case Kernels.CYLINDER -> new Kernel("shapeKernels/Cylinder.comp");
            case Kernels.ELLIPSOID -> new Kernel("shapeKernels/Ellipsoid.comp");
            case Kernels.INSIDE_ARC -> new Kernel("shapeKernels/InsideArc.comp");
            case Kernels.INSIDE_STAIR -> new Kernel("shapeKernels/InsideStair.comp");
            case Kernels.OUTSIDE_ARC -> new Kernel("shapeKernels/OutsideArc.comp");
            case Kernels.OUTSIDE_STAIR -> new Kernel("shapeKernels/OutsideStair.comp");
            case Kernels.SLAB -> new Kernel("shapeKernels/Slab.comp");
            case Kernels.SPHERE -> new Kernel("shapeKernels/Sphere.comp");
            case Kernels.STAIR -> new Kernel("shapeKernels/Stair.comp");
            default -> throw new IllegalStateException("Unexpected value: " + identifier);
        };
    }
}
