package core.language;

import core.utils.StringGetter;

public interface Translatable extends StringGetter {

    default String get() {
        return Language.getTranslation(this);
    }

    String translationFileName();

    int ordinal();

    default String fallbackTranslation() {
        return ((Enum<?>) this).name();
    }
}
