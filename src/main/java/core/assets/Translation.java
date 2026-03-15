package core.assets;

public record Translation(String[] translations) implements Asset {

    @Override
    public void delete() {

    }
}
