package core.assets.identifiers;

import core.assets.ObjectGenerator;
import core.assets.VertexArray;
import core.utils.MainThread;

public interface VertexArrayIdentifier extends AssetIdentifier<VertexArray> {

    ObjectGenerator getGenerator();

    @Override
    @MainThread
    default VertexArray generateAsset() {
        return new VertexArray(getGenerator());
    }
}
