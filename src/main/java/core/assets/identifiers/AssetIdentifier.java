package core.assets.identifiers;

import core.assets.Asset;

public interface AssetIdentifier<ASSET extends Asset> {

    ASSET generateAsset();

}
