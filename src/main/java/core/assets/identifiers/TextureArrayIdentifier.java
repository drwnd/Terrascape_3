package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.assets.ObjectGenerator;
import core.assets.TextureArray;

public interface TextureArrayIdentifier extends AssetIdentifier<TextureArray> {

    ObjectGenerator getGenerator();

    default AssetGenerator<TextureArray> getAssetGenerator() {
        return () -> new TextureArray(getGenerator());
    }
}
