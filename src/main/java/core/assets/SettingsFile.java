package core.assets;

public final class SettingsFile extends Asset {

    private final String[][] settings;

    public SettingsFile(String[][] settings) {
        this.settings = settings;
    }

    public String[][] getSettings() {
        return settings;
    }

    @Override
    public void delete() {

    }
}
