package core.settings;

public interface IntSetting extends Setting, NumberSetting<Integer> {

    default boolean setIfPresent(String name, String value) {
        if (!name().equalsIgnoreCase(name)) return false;
        setValue((int) Double.parseDouble(value));
        return true;
    }

    default String toSaveValue() {
        return String.valueOf(value());
    }

    void setValue(int value);

    int value();

    int defaultValue();

    @Override
    default Integer valueGeneric() {
        return value();
    }

    @Override
    default Integer defaultValueGeneric() {
        return defaultValue();
    }
}
