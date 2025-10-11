package core.assets.identifiers;

import core.assets.Asset;
import core.assets.AssetGenerator;

public interface AssetIdentifier<ASSET extends Asset> {

    AssetGenerator<ASSET> getAssetGenerator();

}
