package game.server.generation;

import core.settings.optionSettings.Option;
import core.utils.StringGetter;

public enum BlockSizes implements Option, StringGetter {
    LOD_0, LOD_1, LOD_2, LOD_3, LOD_4, LOD_5, LOD_6;

    @Override
    public String get() {
        return Integer.toString(1 << ordinal()) + '³';
    }
}
