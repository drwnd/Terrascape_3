package game.assets;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.ComputeShader;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.RenderShader;
import core.rendering_api.shaders.Shader;

public final class ShaderLoader {

    /**
     * Loads a shader based on the provided identifier.
     * Supports both render shaders and compute shaders.
     *
     * @param identifier the shader identifier specifying the source files
     * @return the loaded and compiled Shader instance
     * @throws IllegalStateException if the identifier is not recognized
     */
    public static Shader loadShader(ShaderIdentifier identifier) {
        return switch (identifier) {
            case Shaders.OPAQUE_GEOMETRY -> new RenderShader("Material.vert", "Opaque.frag", identifier);
            case Shaders.SKYBOX -> new RenderShader("Skybox.vert", "Skybox.frag", identifier);
            case Shaders.TRANSPARENT -> new RenderShader("Material.vert", "Transparent.frag", identifier);
            case Shaders.GLASS -> new RenderShader("Material.vert", "Glass.frag", identifier);
            case Shaders.SSAO -> new GuiShader("Gui.vert", "SSAO.frag", identifier);
            case Shaders.OPAQUE_PARTICLE -> new RenderShader("Particle.vert", "Opaque.frag", identifier);
            case Shaders.GLASS_PARTICLE -> new RenderShader("Particle.vert", "Glass.frag", identifier);
            case Shaders.AABB_INDICATOR -> new RenderShader("AABBIndicator.vert", "AABBIndicator.frag", identifier);
            case Shaders.AABB -> new RenderShader("AABB.vert", "Null.frag", identifier);
            case Shaders.OCCLUSION_CULLING -> new RenderShader("AABB.vert", "OcclusionCulling.frag", identifier);
            case Shaders.CHUNK_SHADOW -> new RenderShader("Material.vert", "Shadow.frag", identifier);
            case Shaders.PARTICLE_SHADOW -> new RenderShader("Particle.vert", "Shadow.frag", identifier);
            case Shaders.VOLUME_INDICATOR -> new RenderShader("StructureHologram.vert", "StructureHologram.frag", identifier);
            case Shaders.TRANSPARENCY_APPLIER -> new GuiShader("Gui.vert", "TransparencyApplier.frag", identifier);

            case ComputeShaders.ARC -> new ComputeShader("shapeShaders/Arc.comp", identifier);
            case ComputeShaders.CONE -> new ComputeShader("shapeShaders/Cone.comp", identifier);
            case ComputeShaders.CUBE -> new ComputeShader("shapeShaders/Cube.comp", identifier);
            case ComputeShaders.CYLINDER -> new ComputeShader("shapeShaders/Cylinder.comp", identifier);
            case ComputeShaders.ELLIPSOID -> new ComputeShader("shapeShaders/Ellipsoid.comp", identifier);
            case ComputeShaders.INSIDE_ARC -> new ComputeShader("shapeShaders/InsideArc.comp", identifier);
            case ComputeShaders.INSIDE_STAIR -> new ComputeShader("shapeShaders/InsideStair.comp", identifier);
            case ComputeShaders.OUTSIDE_ARC -> new ComputeShader("shapeShaders/OutsideArc.comp", identifier);
            case ComputeShaders.OUTSIDE_STAIR -> new ComputeShader("shapeShaders/OutsideStair.comp", identifier);
            case ComputeShaders.SLAB -> new ComputeShader("shapeShaders/Slab.comp", identifier);
            case ComputeShaders.SPHERE -> new ComputeShader("shapeShaders/Sphere.comp", identifier);
            case ComputeShaders.STAIR -> new ComputeShader("shapeShaders/Stair.comp", identifier);
            default -> throw new IllegalStateException("Unexpected value: " + identifier);
        };
    }
}
