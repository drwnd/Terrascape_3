package core.assets.identifiers;

import core.assets.AssetLoader;
import core.assets.AssetManager;
import core.assets.Texture;
import core.assets.TextureArray;
import core.rendering_api.CoreObjectLoader;
import core.utils.FileIndexSet;
import core.utils.MainThread;

import java.nio.file.Path;

public interface TextureArrayIdentifier extends AssetIdentifier<TextureArray> {

    String folderName();

    FileIndexSet<?> indexSet();

    @MainThread
    default TextureArray generateAsset() {
        Texture[] textures = getTextures(indexSet());
        return CoreObjectLoader.generateTextureArray(textures);
    }

    @MainThread
    private Texture[] getTextures(FileIndexSet<?> indexSet) {
        Texture[] textures = new Texture[indexSet.getCount()];

        for (int index = 0; index < textures.length; index++) {
            String fileName = indexSet.getFileName(index);
            Path filepath = AssetManager.getAssetFilepath(Path.of("textures", folderName(), fileName));

            textures[index] = AssetLoader.loadTexture2D(filepath);
        }
        return textures;
    }
}
