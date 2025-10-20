package game.server.material;

import java.util.List;

public enum Properties {

    NO_COLLISION,
    TRANSPARENT,
    OCCLUDES_SELF_ONLY(TRANSPARENT);


    Properties(Properties... properties) {
        byte value = (byte) (1 << ordinal());
        for (Properties property : properties) value |= property.value;
        this.value = value;
    }

    Properties() {
        value = (byte) (1 << ordinal());
    }

    public static byte getCombinedValue(List<String> properties) {
        if (properties == null) return 0;
        byte value = 0;
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

    public static boolean hasProperties(byte material, int properties) {
        return (Material.getMaterialProperties(material) & properties) == properties;
    }

    public static boolean doesntHaveProperties(byte material, byte properties) {
        return (Material.getMaterialProperties(material) & properties) == 0;
    }

    /**
     * Use {@code Constants.PROPERTY_NAME} instead
     *
     * @return The same value as {@code CONSTANTS.PROPERTY_NAME} just slower
     */
    public byte getValue() {
        return value;
    }

    private final byte value;
}
