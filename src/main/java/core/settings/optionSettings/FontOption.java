package core.settings.optionSettings;

import core.assets.identifiers.ITextureIdentifier;
import core.utils.FileManager;

import java.io.File;
import java.util.Arrays;

public final class FontOption implements Option, ITextureIdentifier {

    public FontOption(String fontName) {
        this(new File("assets/fonts/" + fontName));
    }

    private FontOption(File fontFile) {
        this.fontFile = fontFile;

        String[] lines = FileManager.readAllLines(new File(fontFile.getPath() + "/settings"));
        if (lines.length == 0) return;

        String defaultLine = lines[0];
        if (defaultLine.startsWith("default:")) Arrays.fill(charSizes, Byte.parseByte(defaultLine.substring(8)));

        for (String line : lines) {
            if (line.startsWith("default:")) continue;

            int colonIndex = line.indexOf(':');
            byte size = Byte.parseByte(line.substring(0, colonIndex));
            char[] charsWithSize = line.substring(colonIndex + 1).toCharArray();
            for (char character : charsWithSize) charSizes[character & 0xFF] = size;
        }
    }


    public byte[] getCharSizes() {
        return charSizes;
    }


    @Override
    public Option next() {
        File[] fonts = getAvailableFonts();
        int index = (getOwnIndex(fonts) + 1) % fonts.length;
        return new FontOption(fonts[index]);
    }

    @Override
    public Option previous() {
        File[] fonts = getAvailableFonts();
        int index = (getOwnIndex(fonts) - 1 + fonts.length) % fonts.length;
        return new FontOption(fonts[index]);
    }

    @Override
    public Option value(String name) {
        return new FontOption(name);
    }

    @Override
    public String name() {
        return fontFile.getName();
    }

    @Override
    public String filepath() {
        return fontFile.getPath() + "/Atlas.png";
    }

    @Override
    public String toString() {
        return name();
    }


    private File[] getAvailableFonts() {
        File parent = fontFile.getParentFile();
        return parent.listFiles();
    }

    private int getOwnIndex(File[] fonts) {
        for (int index = 0; index < fonts.length; index++) if (fontFile.equals(fonts[index])) return index;
        return 0;
    }

    private final byte[] charSizes = new byte[256];
    private final File fontFile;
}
