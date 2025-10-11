package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.assets.AssetLoader;
import core.assets.Texture;

public interface TextureIdentifier extends AssetIdentifier<Texture> {

    String filepath();

    default AssetGenerator<Texture> getAssetGenerator() {
        return () -> AssetLoader.loadTexture2D(this);
    }
}
