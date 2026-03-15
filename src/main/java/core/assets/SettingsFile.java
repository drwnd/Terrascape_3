package core.assets;

public record SettingsFile(String[][] tokens) implements Asset {

    @Override
    public void delete() {

    }
}
