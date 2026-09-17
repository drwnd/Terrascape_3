package core.assets;

import core.utils.MainThread;

public interface ObjectGenerator {

    @MainThread
    int generateObject();

}
