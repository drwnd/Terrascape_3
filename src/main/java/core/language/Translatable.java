package core.language;

import core.utils.StringGetter;

public interface Translatable extends StringGetter {

    default String get() {
        return Language.getTranslation(this);
    }

    String translationFileName();

    int ordinal();

    default String fallbackTranslation() {
        return toTextFormat(((Enum<?>) this).name());
    }

    private static String toTextFormat(String codeName) {
       char[] chars = codeName.toCharArray();
       boolean lastIsSpace = true;

        for (int index = 0; index < chars.length; index++) {
            char character = chars[index];

            if (character == '_') {
                chars[index] = ' ';
                lastIsSpace = true;
                continue;
            }

            if (lastIsSpace) {
                lastIsSpace = false;
                continue;
            }

            chars[index] = Character.toLowerCase(character);
        }

        return new String(chars);
    }
}
