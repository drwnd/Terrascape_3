package core.assets.identifiers;

import core.assets.Buffer;
import core.assets.ObjectGenerator;
import core.utils.MainThread;

public interface BufferIdentifier extends AssetIdentifier<Buffer> {

    ObjectGenerator getGenerator();

    @Override
    @MainThread
    default Buffer generateAsset() {
        return new Buffer(getGenerator());
    }
}
