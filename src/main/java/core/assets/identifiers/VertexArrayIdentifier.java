package core.assets.identifiers;

import core.assets.AssetGenerator;
import core.assets.ObjectGenerator;
import core.assets.VertexArray;

public interface VertexArrayIdentifier extends AssetIdentifier<VertexArray> {

    ObjectGenerator getGenerator();

    @Override
    default AssetGenerator<VertexArray> getAssetGenerator() {
        return () -> new VertexArray(getGenerator());
    }
}
