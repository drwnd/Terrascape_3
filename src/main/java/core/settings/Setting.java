package core.settings;

import core.language.Translatable;

public interface Setting extends Translatable {

    default String name() {
        return ((Enum<?>) this).name();
    }

    String toSaveValue();

    boolean setIfPresent(String name, String value);
}
