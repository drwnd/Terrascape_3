package core.assets;

public final class Translation extends Asset {

    private final String[] translations;

    public Translation(String[] translations) {
        this.translations = translations;
    }

    public String[] getTranslations() {
        return translations;
    }

    @Override
    public void delete() {

    }
}
