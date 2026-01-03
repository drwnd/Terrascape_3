package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.assets.TextureArray;
import core.assets.TextureArrayGenerator;

public interface TextureArrayIdentifier extends AssetIdentifier<TextureArray> {

    TextureArrayGenerator getGenerator();

    default AssetGenerator<TextureArray> getAssetGenerator() {
        return () -> getGenerator().generate();
    }
}
