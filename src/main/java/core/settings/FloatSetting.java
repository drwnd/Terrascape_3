package core.settings;

public interface FloatSetting {

    void setValue(float value);

    float value();

    float defaultValue();

    float valueFromFraction(float fraction);

    float fractionFromValue(float value);
}
