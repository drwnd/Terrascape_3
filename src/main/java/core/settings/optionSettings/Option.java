package core.settings.optionSettings;

/**
 * Must only be implemented by an Enum.
 * Otherwise, override {@code next()}, {@code previous()}, {@code value(...)}, {@code name()}, {@code ordinal()} and {@code toString()}.
 */
public interface Option {

    // Side note for my future self : WHY and HOW the actual fuck does this work?
    // Nvm the unnecessary generic type made it way more complicated, this is fine

    default Option next() {
        Option[] enumConstants = getClass().getEnumConstants();
        return enumConstants[(ordinal() + 1) % enumConstants.length];
    }

    default Option previous() {
        Option[] enumConstants = getClass().getEnumConstants();
        return enumConstants[(ordinal() + enumConstants.length - 1) % enumConstants.length];
    }

    default Option value(String name) {
        //noinspection unchecked
        return (Option) Enum.valueOf(((Enum<?>) this).getClass(), name.toUpperCase());
    }

    default String name() {
        return ((Enum<?>) this).name();
    }

    default int ordinal() {
        return ((Enum<?>) this).ordinal();
    }
}
