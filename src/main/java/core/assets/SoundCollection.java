package core.assets;

import core.assets.identifiers.SoundIdentifier;

public record SoundCollection(SoundIdentifier[] identifiers) implements Asset {

    @Override
    public void delete() {

    }
}
