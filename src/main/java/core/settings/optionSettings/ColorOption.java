package core.settings.optionSettings;

import java.awt.*;

public enum ColorOption implements Option {

    WHITE(Color.WHITE),
    LIGHT_GRAY(Color.LIGHT_GRAY),
    GRAY(Color.GRAY),
    DARK_GRAY(Color.DARK_GRAY),
    BLACK(Color.BLACK),
    RED(Color.RED),
    GREEN(Color.GREEN),
    BLUE(Color.BLUE),
    YELLOW(Color.YELLOW),
    MAGENTA(Color.MAGENTA),
    CYAN(Color.CYAN),
    ORANGE(Color.ORANGE),
    PINK(Color.PINK);

    ColorOption(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public static ColorOption fromColor(Color color) {
        for (ColorOption colorOption : values()) if (colorOption.color.equals(color)) return colorOption;
        return WHITE;
    }

    private final Color color;
}
