package core.language;

import core.assets.AssetManager;
import core.assets.Translation;
import core.assets.identifiers.AssetIdentifier;
import core.settings.CoreOptionSettings;
import core.settings.optionSettings.Option;
import core.utils.FileManager;

import java.io.File;
import java.util.HashMap;

public final class Language implements Option {

    public static String getTranslation(Translatable translatable) {
        return ((Language) CoreOptionSettings.LANGUAGE.value()).getLanguageTranslation(translatable);
    }

    public Language(String languageName) {
        this(new File("assets/languages/" + languageName));
    }

    private Language(File languageFile) {
        this.languageFile = languageFile;
        translationIdentifierTable = new HashMap<>();
    }

    private String getLanguageTranslation(Translatable translatable) {
        AssetIdentifier<Translation> identifier = translationIdentifierTable.computeIfAbsent(translatable.getClass(),
                _ -> () -> loadTranslation(translatable.translationFileName()));
        return AssetManager.get(identifier).translations()[translatable.ordinal()];
    }


    @Override
    public Option next() {
        File[] languages = FileManager.getSiblings(languageFile);
        int index = (FileManager.indexOf(languageFile, languages) + 1) % languages.length;
        return new Language(languages[index]);
    }

    @Override
    public Option previous() {
        File[] languages = FileManager.getSiblings(languageFile);
        int index = (FileManager.indexOf(languageFile, languages) - 1 + languages.length) % languages.length;
        return new Language(languages[index]);
    }

    @Override
    public Option value(String name) {
        return new Language(name);
    }

    @Override
    public int ordinal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String name() {
        return languageFile.getName();
    }

    @Override
    public String toString() {
        return name();
    }

    private Translation loadTranslation(String translationFileName) {
        String[] translations = FileManager.readAllLines(new File(languageFile.getPath() + "/" + translationFileName));
        return new Translation(translations);
    }

    private final File languageFile;
    private final HashMap<Class<? extends Translatable>, AssetIdentifier<Translation>> translationIdentifierTable;
}
