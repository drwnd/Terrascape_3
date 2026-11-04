package game.server.saving;

import core.utils.MainThread;
import core.utils.Saver;
import core.utils.Vector3l;

import game.items.Item;
import game.player.Hotbar;
import game.player.Player;
import game.utils.Position;

import org.joml.Vector3f;

import java.nio.file.Path;

public final class PlayerSaver extends Saver<Player> {

    public static Path getSaveFileLocation(String worldName) {
        return Path.of("saves", worldName, "playerData");
    }

    public PlayerSaver() {
        super(256);
    }

    @Override
    @MainThread
    protected void save(Player player) {
        saveGeneric(player.getPosition());
        saveVector3f(player.getCamera().getRotation());
        saveVector3f(player.getMovement().getVelocity());
        saveInt(player.getHotbar().getSelectedSlot());
        saveByte(player.getMovement().getState().getIdentifier());
        for (Item item : player.getHotbar().getContent()) saveGeneric(item);
    }

    @Override
    @MainThread
    protected Player load() {
        Position position = loadGeneric(Position::load);
        Vector3f rotation = loadVector3f();
        Vector3f velocity = loadVector3f();
        int selectedSlot = loadInt();
        byte movementStateIdentifier = loadByte();
        Item[] hotbar = new Item[Hotbar.LENGTH];
        for (int slot = 0; slot < Hotbar.LENGTH; slot++) hotbar[slot] = loadGeneric(Item::loadGeneric);

        Player player = new Player(position);
        player.getCamera().setRotation(rotation);
        player.getMovement().setVelocity(velocity);
        player.getHotbar().setSelectedSlot(selectedSlot);
        player.getMovement().setState(movementStateIdentifier);
        player.getHotbar().setContent(hotbar);

        return player;
    }

    @Override
    @MainThread
    protected Player getDefault() {
        Vector3l worldCenter = new Vector3l(0, 0, 0);
        return new Player(new Position(worldCenter, new Vector3f()));
    }

    @Override
    protected int getVersionNumber() {
        return 14;
    }
}
