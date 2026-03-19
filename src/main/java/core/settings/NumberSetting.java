package core.settings;

public interface NumberSetting<T extends Number> {

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
