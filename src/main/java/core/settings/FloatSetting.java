package core.settings;

public interface FloatSetting extends Setting {

    default boolean setIfPresent(String name, String value) {
        if (!name().equalsIgnoreCase(name)) return false;
        setValue(Float.parseFloat(value));
        return true;
    }

    default String toSaveValue() {
        return String.valueOf(value());
    }

    void setValue(float value);

    float value();

    float defaultValue();

    float valueFromFraction(float fraction);

    float fractionFromValue(float value);
}
