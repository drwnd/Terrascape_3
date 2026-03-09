package core.language;

import core.settings.CoreOptionSettings;
import core.settings.optionSettings.Option;
import core.utils.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public final class Language implements Option {

    static {
        translatables = new ArrayList<>();
        indexTable = new HashMap<>();
        registerTranslationEnums(CoreUiMessages.class);
    }

    @SafeVarargs
    public static void registerTranslationEnums(Class<? extends Translatable>... translatables) {
        for (Class<? extends Translatable> translatable : translatables) registerTranslationEnum(translatable);
    }

    public static String getTranslation(Translatable translatable) {
        return ((Language) CoreOptionSettings.LANGUAGE.value()).getLanguageTranslation(translatable);
    }

    public Language(String languageName) {
        this(new File("assets/languages/" + languageName));
    }

    private Language(File languageFile) {
        this.languageFile = languageFile;
        translations = new String[translatables.size()][0];
        load();
    }

    public void load() {
        for (int index = 0; index < translations.length; index++) {
            Translatable[] translatables = Language.translatables.get(index);
            String translationFilePath = languageFile.getPath() + '/' + translatables[0].translationFileName();
            String[] translations = FileManager.readAllLines(new File(translationFilePath));
            this.translations[index] = getTranslations(translatables, translations);
        }
    }

    private String getLanguageTranslation(Translatable translatable) {
        return translations[indexTable.get(translatable.getClass())][translatable.ordinal()];
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
    public String name() {
        return languageFile.getName();
    }

    @Override
    public String toString() {
        return name();
    }


    private static void registerTranslationEnum(Class<? extends Translatable> translatables) {
        if (!translatables.isEnum()) throw new IllegalArgumentException("Argument must be an Enum");
        Language.translatables.add(translatables.getEnumConstants());
        indexTable.put(translatables, indexTable.size());
    }

    private static String[] getTranslations(Translatable[] translatables, String[] storedTranslations) {
        String[] translations = new String[translatables.length];
        for (int index = 0; index < translations.length; index++)
            translations[index] = storedTranslations.length > index ? storedTranslations[index] : translatables[index].fallbackTranslation();
        return translations;
    }

    private final File languageFile;
    private final String[][] translations;

    private static final ArrayList<Translatable[]> translatables;
    private static final HashMap<Class<? extends Translatable>, Integer> indexTable;
}
