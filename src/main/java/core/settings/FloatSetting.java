package core.settings;

public interface FloatSetting extends Setting, NumberSetting<Float> {

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

    float min();

    float max();

    @Override
    default Float valueGeneric() {
        return value();
    }

    @Override
    default Float defaultValueGeneric() {
        return defaultValue();
    }
}
