package core.settings;

import core.language.Translatable;

public interface NumberSetting<T extends Number> extends Translatable {

    T valueFromFraction(float fraction);

    float fractionFromValue(T value);

    T valueGeneric();

    T defaultValueGeneric();

    default void setValue(Number value) {
        switch (value) {
            case Integer intValue -> ((IntSetting) this).setValue((int) intValue);
            case Float floatValue -> ((FloatSetting) this).setValue((float) floatValue);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        }
    }
}
