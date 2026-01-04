package game.server.saving;

import core.utils.Saver;

import game.assets.StructureIdentifier;
import game.player.Hotbar;
import game.player.Player;
import game.player.interaction.ChunkRebuildPlaceable;
import game.player.interaction.CubePlaceable;
import game.player.interaction.Placeable;
import game.player.interaction.StructurePlaceable;
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
        saveByte(player.getMovement().getState().getIdentifier());
        for (Placeable placeable : player.getHotbar().getContents()) savePlaceable(placeable);
    }

    @Override
    protected Player load() {
        Position position = loadPosition();
        Vector3f rotation = loadVector3f();
        Vector3f velocity = loadVector3f();
        int selectedSlot = loadInt();
        byte movementStateIdentifier = loadByte();
        Placeable[] hotbar = new Placeable[Hotbar.LENGTH];
        for (int slot = 0; slot < Hotbar.LENGTH; slot++) hotbar[slot] = loadPlaceable();

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
        Vector3i worldCenter = new Vector3i(WORLD_SIZE >>> 1, WORLD_SIZE >>> 1, WORLD_SIZE >>> 1);
        return new Player(new Position(worldCenter, new Vector3f()));
    }

    @Override
    protected int getVersionNumber() {
        return 3;
    }

    private void savePlaceable(Placeable placeable) {
        if (placeable instanceof CubePlaceable) {
            saveByte((byte) 1);
            saveByte(((CubePlaceable) placeable).getMaterial());
            return;
        }
        if (placeable instanceof StructurePlaceable) {
            saveByte((byte) 2);
            saveString(((StructurePlaceable) placeable).getIdentifier().structureName());
            return;
        }
        if (placeable instanceof ChunkRebuildPlaceable) {
            saveByte((byte) 3);
            return;
        }
        saveByte((byte) 0);
    }

    private Placeable loadPlaceable() {
        byte identifier = loadByte();
        if (identifier == 0) return null;
        if (identifier == 1) return new CubePlaceable(loadByte());
        if (identifier == 2) return new StructurePlaceable(new StructureIdentifier(loadString()));
        if (identifier == 3) return new ChunkRebuildPlaceable();
        return null;
    }
}
