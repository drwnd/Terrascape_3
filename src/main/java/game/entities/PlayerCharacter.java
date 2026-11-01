package game.entities;

import core.assets.Texture2D;

import core.assets.identifiers.AssetIdentifier;
import game.assets.EntityType;
import game.settings.OptionSettings;

public final class PlayerCharacter extends RenderEntity {
    public PlayerCharacter() {
        super(EntityType.PLAYER);
    }

    @Override
    public AssetIdentifier<Texture2D> getTextureIdentifier() {
        //noinspection unchecked
        return (AssetIdentifier<Texture2D>) OptionSettings.SKIN.value();
    }
}
