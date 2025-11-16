package game.server.saving;

import core.utils.Saver;

import game.player.Player;
import game.utils.Position;

import org.joml.Vector3f;
import org.joml.Vector3i;

import static game.utils.Constants.*;

public final class PlayerSaver extends Saver<Player> {

    public static String getSaveFileLocation(String worldName) {
        return "saves/%s/playerData".formatted(worldName);
    }

    public PlayerSaver() {
        super(52);
    }

    @Override
    protected void save(Player player) {
        savePosition(player.getPosition());
        saveVector3f(player.getCamera().getRotation());
        saveVector3f(player.getMovement().getVelocity());
        saveInt(player.getHotbar().getSelectedSlot());
    }

    @Override
    protected Player load() {
        Position position = loadPosition();
        Vector3f rotation = loadVector3f();
        Vector3f velocity = loadVector3f();
        int selectedSlot = loadInt();

        Player player = new Player(position);
        player.getCamera().setRotation(rotation);
        player.getMovement().setVelocity(velocity);
        player.getHotbar().setSelectedSlot(selectedSlot);

        return player;
    }

    @Override
    protected Player getDefault() {
        Vector3i worldCenter = new Vector3i(WORLD_SIZE_XZ_MASK / 2, WORLD_SIZE_Y_MASK / 2, WORLD_SIZE_XZ_MASK / 2);
        return new Player(new Position(worldCenter, new Vector3f()));
    }

    @Override
    protected int getVersionNumber() {
        return 1;
    }
}
