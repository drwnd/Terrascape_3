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
            case CoreShaders.GUI -> new GuiShader("assets/shaders/Gui.vert", "assets/shaders/Gui.frag", identifier);
            case CoreShaders.GUI_BACKGROUND -> new GuiShader("assets/shaders/Gui.vert", "assets/shaders/GuiBackground.frag", identifier);
            case CoreShaders.TEXT -> new TextShader("assets/shaders/Text.vert", "assets/shaders/Text.frag", identifier);
            case Shaders.OPAQUE_GEOMETRY -> new Shader("assets/shaders/Material.vert", "assets/shaders/Opaque.frag", identifier);
            case Shaders.SKYBOX -> new Shader("assets/shaders/Skybox.vert", "assets/shaders/Skybox.frag", identifier);
            case Shaders.WATER -> new Shader("assets/shaders/Material.vert", "assets/shaders/Water.frag", identifier);
            case Shaders.GLASS -> new Shader("assets/shaders/Material.vert", "assets/shaders/Glass.frag", identifier);
            case Shaders.SSAO -> new GuiShader("assets/shaders/Gui.vert", "assets/shaders/SSAO.frag", identifier);
            case Shaders.AO_APPLIER -> new GuiShader("assets/shaders/Gui.vert", "assets/shaders/AO_Applier.frag", identifier);
            case Shaders.OPAQUE_PARTICLE -> new Shader("assets/shaders/Particle.vert", "assets/shaders/Opaque.frag", identifier);
            case Shaders.TRANSPARENT_PARTICLE -> new Shader("assets/shaders/Particle.vert", "assets/shaders/Glass.frag", identifier);
            case Shaders.VOLUME_INDICATOR -> new Shader("assets/shaders/VolumeIndicator.vert", "assets/shaders/VolumeIndicator.frag", identifier);
            case Shaders.AABB -> new Shader("assets/shaders/AABB.vert", "assets/shaders/Null.frag", identifier);
            default -> throw new IllegalStateException("Unexpected value: " + identifier);
        };
    }
}
