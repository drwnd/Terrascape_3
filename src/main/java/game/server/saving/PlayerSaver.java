package game.server.saving;

import core.utils.Saver;
import core.utils.Vector3l;

import game.player.Hotbar;
import game.player.Player;
import game.player.interaction.Placeable;
import game.utils.Position;

import org.joml.Vector3f;

public final class PlayerSaver extends Saver<Player> {

    public static String getSaveFileLocation(String worldName) {
        return "saves/%s/playerData".formatted(worldName);
    }

    public PlayerSaver() {
        super(64);
    }

    @Override
    protected void save(Player player) {
        saveGeneric(player.getPosition(), Position::save);
        saveVector3f(player.getCamera().getRotation());
        saveVector3f(player.getMovement().getVelocity());
        saveInt(player.getHotbar().getSelectedSlot());
        saveByte(player.getMovement().getState().getIdentifier());
        for (Placeable placeable : player.getHotbar().getContents()) saveGeneric(placeable, Placeable::savePlaceable);
    }

    @Override
    protected Player load() {
        Position position = loadGeneric(Position::load);
        Vector3f rotation = loadVector3f();
        Vector3f velocity = loadVector3f();
        int selectedSlot = loadInt();
        byte movementStateIdentifier = loadByte();
        Placeable[] hotbar = new Placeable[Hotbar.LENGTH];
        for (int slot = 0; slot < Hotbar.LENGTH; slot++) hotbar[slot] = loadGeneric(Placeable::loadPlaceable);

        Player player = new Player(position);
        player.getCamera().setRotation(rotation);
        player.getMovement().setVelocity(velocity);
        player.getHotbar().setSelectedSlot(selectedSlot);
        player.getMovement().setState(movementStateIdentifier);
        player.getHotbar().setContents(hotbar);

        return player;
    }

    @Override
    protected Player getDefault() {
        Vector3l worldCenter = new Vector3l(0, 0, 0);
        return new Player(new Position(worldCenter, new Vector3f()));
    }

    @Override
    protected int getVersionNumber() {
        return 8;
    }
}
