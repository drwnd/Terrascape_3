package game.server.saving;

import core.utils.Saver;
import game.server.Game;
import game.server.PlayerRecord;
import game.utils.Position;
import org.joml.Vector3f;

import java.util.ArrayList;

public final class PlayerRecordSaver extends Saver<PlayerRecord> {

    public static String getSaveFileLocation(String recordName) {
        return "saves/%s/records/%s".formatted(Game.getWorld().getName(), recordName);
    }

    @Override
    protected void save(PlayerRecord record) {
        saveInt(record.positions().size());
        saveInt(record.rotations().size());

        for (Position position : record.positions()) savePosition(position);
        for (Vector3f rotation : record.rotations()) saveVector3f(rotation);
    }

    @Override
    protected PlayerRecord load() {
        int positionCount = loadInt();
        int rotationCount = loadInt();

        ArrayList<Position> positions = new ArrayList<>(positionCount);
        ArrayList<Vector3f> rotations = new ArrayList<>(rotationCount);

        for (int count = 0; count < positionCount; count++) positions.add(loadPosition());
        for (int count = 0; count < rotationCount; count++) rotations.add(loadVector3f());

        return new PlayerRecord(positions, rotations);
    }

    @Override
    protected PlayerRecord getDefault() {
        return new PlayerRecord(new ArrayList<>(), new ArrayList<>());
    }

    @Override
    protected int getVersionNumber() {
        return 0;
    }
}
