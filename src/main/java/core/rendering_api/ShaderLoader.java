package core.rendering_api;

import core.assets.identifiers.ShaderIdentifier;
import core.rendering_api.shaders.Shader;
import core.assets.CoreShaders;
import core.rendering_api.shaders.GuiShader;
import core.rendering_api.shaders.TextShader;

import game.assets.Shaders;

public final class ShaderLoader {

    private ShaderLoader() {
    }


    public static Shader loadShader(ShaderIdentifier identifier) {
        return switch (identifier) {
            case CoreShaders.GUI -> new GuiShader("Gui.vert", "Gui.frag", identifier);
            case CoreShaders.GUI_BACKGROUND -> new GuiShader("Gui.vert", "GuiBackground.frag", identifier);
            case CoreShaders.TEXT -> new TextShader("Text.vert", "Text.frag", identifier);
            case Shaders.OPAQUE_GEOMETRY -> new Shader("Material.vert", "Opaque.frag", identifier);
            case Shaders.SKYBOX -> new Shader("Skybox.vert", "Skybox.frag", identifier);
            case Shaders.WATER -> new Shader("Material.vert", "Water.frag", identifier);
            case Shaders.GLASS -> new Shader("Material.vert", "Glass.frag", identifier);
            case Shaders.SSAO -> new GuiShader("Gui.vert", "SSAO.frag", identifier);
            case Shaders.AO_APPLIER -> new GuiShader("Gui.vert", "AO_Applier.frag", identifier);
            case Shaders.OPAQUE_PARTICLE -> new Shader("Particle.vert", "Opaque.frag", identifier);
            case Shaders.GLASS_PARTICLE -> new Shader("Particle.vert", "Glass.frag", identifier);
            case Shaders.VOLUME_INDICATOR -> new Shader("VolumeIndicator.vert", "VolumeIndicator.frag", identifier);
            case Shaders.AABB -> new Shader("AABB.vert", "Null.frag", identifier);
            case Shaders.OCCLUSION_CULLING -> new Shader("AABB.vert", "OcclusionCulling.frag", identifier);
            case Shaders.CHUNK_SHADOW -> new Shader("Material.vert", "Shadow.frag", identifier);
            case Shaders.PARTICLE_SHADOW -> new Shader("Particle.vert", "Shadow.frag", identifier);
            default -> throw new IllegalStateException("Unexpected value: " + identifier);
        };
    }
}
