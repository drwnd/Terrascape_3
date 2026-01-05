package core.settings.optionSettings;

import core.assets.AssetGenerator;
import core.assets.AssetLoader;
import core.assets.Texture;
import core.assets.identifiers.AssetIdentifier;
import core.utils.FileManager;

import org.joml.Vector2f;

import java.io.File;
import java.util.Arrays;

public final class FontOption implements Option, AssetIdentifier<Texture> {

    public FontOption(String fontName) {
        this(new File("assets/fonts/" + fontName));
    }

    private FontOption(File fontFile) {
        this.fontFile = fontFile;
        load();
    }


    public void load() {
        String[] lines = FileManager.readAllLines(new File(fontFile.getPath() + "/settings"));
        for (String line : lines) {
            if (line.startsWith("default:")) {
                Arrays.fill(charSizes, Byte.parseByte(line.substring(8)));
                continue;
            }
            if (line.startsWith("pixelSize:")) {
                int separatorIndex = line.indexOf('|');
                int x = Integer.parseInt(line.substring(10, separatorIndex));
                int y = Integer.parseInt(line.substring(separatorIndex + 1));
                defaultTextSize.set(x, y).mul(5.2083336E-4f, 9.259259E-4f);
                continue;
            }
            int colonIndex = line.indexOf(':');
            byte size = Byte.parseByte(line.substring(0, colonIndex));
            char[] charsWithSize = line.substring(colonIndex + 1).toCharArray();
            for (char character : charsWithSize) charSizes[character & 0xFF] = size;
        }
    }


    public byte[] getCharSizes() {
        return charSizes;
    }

    public Vector2f getDefaultTextSize() {
        return defaultTextSize;
    }


    @Override
    public Option next() {
        File[] fonts = FileManager.getSiblings(fontFile);
        int index = (FileManager.indexOf(fontFile, fonts) + 1) % fonts.length;
        return new FontOption(fonts[index]);
    }

    @Override
    public Option previous() {
        File[] fonts = FileManager.getSiblings(fontFile);
        int index = (FileManager.indexOf(fontFile, fonts) - 1 + fonts.length) % fonts.length;
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
    public String toString() {
        return name();
    }

    @Override
    public AssetGenerator<Texture> getAssetGenerator() {
        return () -> AssetLoader.loadTexture2D(filepath());
    }

    private String filepath() {
        return fontFile.getPath() + "/Atlas.png";
    }

    private final byte[] charSizes = new byte[256];
    private final Vector2f defaultTextSize = new Vector2f();
    private final File fontFile;
}
