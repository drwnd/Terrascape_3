package core.assets;

public final class SettingsFile extends Asset {

    private final String[][] tokens;

    public SettingsFile(String[][] tokens) {
        this.tokens = tokens;
    }

    public String[][] getTokens() {
        return tokens;
    }

    @Override
    public void delete() {

    }
}
