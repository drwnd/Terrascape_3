package game.server.material;

import core.rendering_api.Debug;

import java.util.List;

import static game.utils.Constants.*;

public enum Properties {

    NO_COLLISION,
    TRANSPARENT,
    OCCLUDES_SELF_ONLY(TRANSPARENT),
    STRUCTURE_REPLACEABLE,
    SHADOW_TRANSPARENT,

    USE_OPAQUE_RENDERING(OPAQUE_RENDERING),
    USE_TRANSPARENT_RENDERING(TRANSPARENT_RENDERING, TRANSPARENT, OCCLUDES_SELF_ONLY),
    USE_GLASS_RENDERING(GLASS_RENDERING, TRANSPARENT, OCCLUDES_SELF_ONLY);

/**
 * Creates a new Properties instance.
 *
 * @param properties parameter
 */
    Properties(Properties... properties) {
        int value = 1 << ordinal();
        for (Properties property : properties) value |= property.value;
        this.value = value;
    }

    Properties() {
        value = 1 << ordinal();
    }

/**
 * Creates a new Properties instance.
 *
 * @param value parameter
 * @param properties parameter
 */
    Properties(int value, Properties... properties) {
        for (Properties property : properties) value |= property.value;
        this.value = value;
    }

/**
 * Returns the combined value.
 *
 * @param properties parameter
 * @return result
 */
    public static int getCombinedValue(List<String> properties) {
        if (properties == null) return 0;
        int value = 0;
        for (String property : properties) {
            Properties propertyEnum;
            try {
                propertyEnum = valueOf(property);
            } catch (IllegalArgumentException exception) {
                Debug.err("Unrecognized property : %s%n", property);
                continue;
            }
            value |= propertyEnum.value;
        }
        return value;
    }

    public static boolean hasProperties(byte material, int properties) {
        return (Material.getProperties(material) & properties) == properties;
    }

    public static boolean doesntHaveProperties(byte material, int properties) {
        return (Material.getProperties(material) & properties) == 0;
    }

    /**
     * Use {@code Constants.PROPERTY_NAME} instead
     *
     * @return The same value as {@code CONSTANTS.PROPERTY_NAME} just slower
     */
    public int getValue() {
        return value;
    }

    private final int value;
}
