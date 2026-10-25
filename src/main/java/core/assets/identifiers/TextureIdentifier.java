package core.assets.identifiers;

import core.assets.AssetLoader;
import core.assets.AssetManager;
import core.assets.Texture;
import core.utils.MainThread;

import java.nio.file.Path;

public interface TextureIdentifier extends AssetIdentifier<Texture> {

    String fileName();

    @MainThread
    default Texture generateAsset() {
        Path filepath = AssetManager.getAssetFilepath(Path.of("textures", fileName()));
        return AssetLoader.loadTexture2D(filepath);
    }
}
