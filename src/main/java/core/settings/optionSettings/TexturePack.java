package core.settings.optionSettings;

import core.assets.*;
import core.assets.identifiers.AssetIdentifier;
import core.assets.identifiers.TextureArrayIdentifier;
import core.assets.identifiers.TextureIdentifier;
import core.settings.OptionSetting;
import core.utils.FileIndexSet;
import core.utils.FileManager;
import game.player.rendering.ObjectLoader;

import java.io.File;

public final class TexturePack implements Option {

    public TexturePack(String texturePackName) {
        this(new File("assets/texturePacks/" + texturePackName));
    }

    private TexturePack(File textuePackFile) {
        this.textuePackFile = textuePackFile;
        pathTemplate = textuePackFile.getPath() + '/';
    }


    public static AssetIdentifier<Texture> get(TextureIdentifier identifier) {
        TexturePack texturePack = (TexturePack) OptionSetting.TEXTURE_PACK.value();
        return new TextureAssetIdentifier(identifier.fileName(), texturePack.pathTemplate);
    }

    public static AssetIdentifier<TextureArray> get(TextureArrayIdentifier identifier) {
        TexturePack texturePack = (TexturePack) OptionSetting.TEXTURE_PACK.value();
        return new TextureArrayAssetIdentifier(identifier.folderName(), texturePack.pathTemplate, identifier.indexSet());
    }


    @Override
    public Option next() {
        File[] texturePacks = FileManager.getSiblings(textuePackFile);
        int index = (FileManager.indexOf(textuePackFile, texturePacks) + 1) % texturePacks.length;
        return new TexturePack(texturePacks[index]);
    }

    @Override
    public Option previous() {
        File[] texturePacks = FileManager.getSiblings(textuePackFile);
        int index = (FileManager.indexOf(textuePackFile, texturePacks) - 1 + texturePacks.length) % texturePacks.length;
        return new TexturePack(texturePacks[index]);
    }

    @Override
    public Option value(String name) {
        return new TexturePack(name);
    }

    @Override
    public String name() {
        return textuePackFile.getName();
    }

    @Override
    public String toString() {
        return name();
    }


    private static Texture getTexture(String fileName, String pathTemplate) {
        String filepath = pathTemplate + fileName;
        if (!new File(filepath).exists()) filepath = DEFAULT_PATH_TEMPLATE + fileName;
        return AssetLoader.loadTexture2D(filepath);
    }

    private static Texture[] getTextures(String folderName, String pathTemplate, FileIndexSet indexSet) {
        String basepath = pathTemplate + folderName + '/';
        Texture[] textures = new Texture[indexSet.getCount()];

        for (int index = 0; index < textures.length; index++) {
            String fileName = indexSet.getFileName(index);
            String template = basepath;
            if (!new File(template + fileName).exists()) template = DEFAULT_PATH_TEMPLATE + folderName + '/';

            textures[index] = AssetManager.get(new TextureAssetIdentifier(fileName, template));
        }
        return textures;
    }


    private final File textuePackFile;
    private final String pathTemplate;

    private static final String DEFAULT_PATH_TEMPLATE = "assets/texturePacks/Default/";

    private record TextureAssetIdentifier(String fileName, String pathTemplate) implements AssetIdentifier<Texture> {

        @Override
        public AssetGenerator<Texture> getAssetGenerator() {
            return () -> TexturePack.getTexture(fileName, pathTemplate);
        }
    }

    private record TextureArrayAssetIdentifier(String folderName, String pathTemplate, FileIndexSet indexSet) implements AssetIdentifier<TextureArray> {

        @Override
        public AssetGenerator<TextureArray> getAssetGenerator() {
            return () -> {
                Texture[] textures = TexturePack.getTextures(folderName, pathTemplate, indexSet);
                return ObjectLoader.generateTextureArray(textures);
            };
        }
    }
}
