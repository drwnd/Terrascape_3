package core.settings;

public interface Setting {

    default String name() {
        return ((Enum<?>) this).name();
    }

    String toSaveValue();

    boolean setIfPresent(String name, String value);
}
