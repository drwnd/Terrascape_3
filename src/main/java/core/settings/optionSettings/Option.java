package core.settings.optionSettings;

/**
 * Must only be implemented by an Enum.
 * Otherwise, override next(), previous(), value(...), name() and toString().
 */
public interface Option {

    // Side note for my future self : WHY and HOW the actual fuck does this work?
    // Nvm the unnecessary generic type made it way more complicated, this is fine

    default Option next() {
        Enum<?> thisEnum = (Enum<?>) this;
        Option[] enumConstants = getClass().getEnumConstants();

        return enumConstants[(thisEnum.ordinal() + 1) % enumConstants.length];
    }

    default Option previous() {
        Enum<?> thisEnum = (Enum<?>) this;
        Option[] enumConstants = getClass().getEnumConstants();

        return enumConstants[(thisEnum.ordinal() + enumConstants.length - 1) % enumConstants.length];
    }

    default Option value(String name) {
        Enum<?> thisEnum = (Enum<?>) this;

        //noinspection unchecked
        return (Option) Enum.valueOf(thisEnum.getClass(), name.toUpperCase());
    }

    default String name() {
        return ((Enum<?>) this).name();
    }
}
