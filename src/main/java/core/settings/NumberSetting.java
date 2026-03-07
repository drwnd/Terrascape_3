package core.settings;

public interface NumberSetting<T extends Number> {

    T valueFromFraction(float fraction);

    float fractionFromValue(T value);

    T valueGeneric();

    T defaultValueGeneric();
}
