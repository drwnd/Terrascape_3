package core.assets.identifiers;

import core.assets.*;
import core.rendering_api.CoreObjectLoader;
import core.utils.FileIndexSet;
import core.utils.MainThread;

import java.nio.file.Path;

public interface TextureArrayIdentifier extends AssetIdentifier<TextureArray> {

    String folderName();

    FileIndexSet<?> indexSet();

    @MainThread
    default TextureArray generateAsset() {
        Texture2D[] textures = getTextures(indexSet());
        return CoreObjectLoader.generateTextureArray(textures);
    }

    @MainThread
    private Texture2D[] getTextures(FileIndexSet<?> indexSet) {
        Texture2D[] textures = new Texture2D[indexSet.getCount()];

        for (int index = 0; index < textures.length; index++) {
            String fileName = indexSet.getFileName(index);
            Path filepath = AssetManager.getAssetFilepath(Path.of("textures", folderName(), fileName));

            textures[index] = AssetLoader.loadTexture2D(filepath);
        }
        return textures;
    }
}
