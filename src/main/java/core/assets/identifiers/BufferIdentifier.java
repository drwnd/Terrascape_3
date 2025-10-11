package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.assets.Buffer;
import core.assets.ObjectGenerator;

public interface BufferIdentifier extends AssetIdentifier<Buffer> {

    ObjectGenerator getGenerator();

    @Override
    default AssetGenerator<Buffer> getAssetGenerator() {
        return () -> new Buffer(getGenerator());
    }
}
