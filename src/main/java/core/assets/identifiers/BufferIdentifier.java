package core.assets.identifiers;

import core.assets.Buffer;
import core.assets.ObjectGenerator;

public interface BufferIdentifier extends AssetIdentifier<Buffer> {

    ObjectGenerator getGenerator();

    @Override
    default Buffer generateAsset() {
        return new Buffer(getGenerator());
    }
}
