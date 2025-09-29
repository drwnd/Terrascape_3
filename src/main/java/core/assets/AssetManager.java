package core.assets;

import core.assets.identifiers.*;
import core.rendering_api.shaders.Shader;
import core.rendering_api.ShaderLoader;

import java.util.HashMap;

public final class AssetManager {

    private AssetManager() {
    }


    public static Texture getTexture(ITextureIdentifier identifier) {
        return (Texture) loadAsset(identifier, () -> AssetLoader.loadTexture(identifier));
    }

    public static GuiElement getGuiElement(GuiElementIdentifier identifier) {
        return (GuiElement) loadAsset(identifier, () -> AssetLoader.loadGuiElement(identifier));
    }

    public static Shader getShader(ShaderIdentifier identifier) {
        return (Shader) loadAsset(identifier, () -> ShaderLoader.loadShader(identifier));
    }

    public static Buffer getBuffer(BufferIdentifier identifier) {
        return (Buffer) loadAsset(identifier, () -> new Buffer(identifier.getGenerator()));
    }

    public static VertexArray getVertexArray(VertexArrayIdentifier identifier) {
        return (VertexArray) loadAsset(identifier, () -> new VertexArray(identifier.getGenerator()));
    }


    public static void reload() {
        System.out.println("---Deleting old Assets---");
        synchronized (assets) {
            for (Asset asset : assets.values()) asset.delete();
            assets.clear();
        }
    }


    private static Asset loadAsset(AssetIdentifier identifier, AssetGenerator generator) {
        synchronized (assets) {
            if (assets.containsKey(identifier)) return assets.get(identifier);
            Asset asset = generator.generate();
            assets.put(identifier, asset);
            return asset;
        }
    }

    private static final HashMap<AssetIdentifier, Asset> assets = new HashMap<>();

    private interface AssetGenerator {
        Asset generate();
    }
}
