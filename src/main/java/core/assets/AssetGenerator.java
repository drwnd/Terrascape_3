package core.assets;

public interface AssetGenerator<ASSET extends Asset> {

    ASSET generate();

}
