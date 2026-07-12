package game.player.interaction;

import core.assets.AssetManager;
import core.utils.Saver;
import core.utils.Vector3l;
import game.assets.StructureIdentifier;
import game.server.Chunk;
import game.server.generation.Structure;
import game.settings.IntSettings;

import java.util.ArrayList;

public final class StructureSelector implements Placeable {

    @Override
    public void place(Vector3l position, int lod) {

    }

    @Override
    public ArrayList<Chunk> getAffectedChunks() {
        return new ArrayList<>();
    }

    @Override
    public Structure getStructure() {
        return AssetManager.get(new StructureIdentifier("BluePrint"));
    }

    @Override
    public boolean intersectsAABB(Vector3l position, Vector3l min, Vector3l max) {
        return true;
    }

    @Override
    public boolean allowBreak() {
        return false;
    }

    @Override
    public void offsetPosition(Vector3l position, int targetedSide) {
        int breakPlaceAlign = 1 << IntSettings.BREAK_PLACE_ALIGN.value();
        int mask = -breakPlaceAlign;

        RepeatPlaceable.offsetPositionFromGround(position, targetedSide, getLengthX(), getLengthY(), getLengthZ());
        position.x &= mask;
        position.y &= mask;
        position.z &= mask;
    }

    @Override
    public void spawnParticles(Vector3l position) {

    }

    @Override
    public void save(Saver<?> saver) {
        saver.saveByte((byte) 19);
    }

    public static StructureSelector load(Saver<?> saver) {
        return new StructureSelector();
    }
}
