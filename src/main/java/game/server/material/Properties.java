package game.server.material;

import java.util.List;

public enum Properties {

    NO_COLLISION,
    REPLACEABLE,
    BLAST_RESISTANT,
    HAS_GRAVITY,
    REQUIRES_BOTTOM_SUPPORT,
    TRANSPARENT,
    OCCLUDES_SELF_ONLY(TRANSPARENT);


    Properties(Properties... properties) {
        int value = 1 << ordinal();
        for (Properties property : properties) value |= property.value;
        this.value = value;
    }

    Properties() {
        value = 1 << ordinal();
    }

    public static int getCombinedValue(List<String> properties) {
        if (properties == null) return 0;
        int value = 0;
        for (String property : properties) {
            Properties propertyEnum;
            try {
                propertyEnum = valueOf(property);
            } catch (IllegalArgumentException exception) {
                System.err.printf("Unrecognized property : %s%n", property);
                continue;
            }
            value |= propertyEnum.value;
        }
        return value;
    }

    /**
     * Use {@code Constants.PROPERTY_NAME} instead
     * @return The same value as {@code CONSTANTS.PROPERTY_NAME} just slower
     */
    public int getValue() {
        return value;
    }

    private final int value;
}
