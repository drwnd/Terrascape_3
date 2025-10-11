package game.server.saving;

import org.joml.Vector3f;
import org.joml.Vector3i;
import game.player.Player;
import game.utils.Position;

public final class PlayerSaver extends Saver<Player> {

    public static String getSaveFileLocation(String worldName) {
        return "saves/%s/playerData".formatted(worldName);
    }

    public PlayerSaver() {
        super(61);
    }

    @Override
    void save(Player player) {
        savePosition(player.getPosition());
        saveVector3f(player.getCamera().getRotation());
        saveVector3f(player.getMovement().getVelocity());
        saveByteArray(player.getHotbar().getContents());
        saveInt(player.getHotbar().getSelectedSlot());
    }

    @Override
    Player load() {
        Position position = loadPosition();
        Vector3f rotation = loadVector3f();
        Vector3f velocity = loadVector3f();
        byte[] hotbarContent = loadByteArray();
        int selectedSlot = loadInt();

        Player player = new Player(position);
        player.getCamera().setRotation(rotation);
        player.getMovement().setVelocity(velocity);
        player.getHotbar().setContent(hotbarContent);
        player.getHotbar().setSelectedSlot(selectedSlot);

        return player;
    }

    @Override
    Player getDefault() {
        return new Player(new Position(new Vector3i(), new Vector3f()));
    }

    @Override
    int getVersionNumber() {
        return 0;
    }
}
