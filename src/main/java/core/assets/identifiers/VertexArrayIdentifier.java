package core.assets.identifiers;

import core.assets.ObjectGenerator;
import core.assets.VertexArray;

public interface VertexArrayIdentifier extends AssetIdentifier<VertexArray> {

    ObjectGenerator getGenerator();

    @Override
    default VertexArray generateAsset() {
        return new VertexArray(getGenerator());
    }
}
