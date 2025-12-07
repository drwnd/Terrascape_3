package game.server.command;

import game.player.Player;
import game.player.movement.FlyingState;
import game.player.movement.MovementState;
import game.server.Function;
import game.server.Game;
import game.server.PlayerRecord;
import game.server.saving.PlayerRecordSaver;
import game.utils.Position;
import org.joml.Vector3f;

import java.util.ArrayList;

final class RecordCommand {

    static final String SYNTAX = "{start, stop, play, cancel} recordName [playback rotation? true/false]";
    static final String EXPLANATION = "Starts or stops recording the Players Position and Rotation or plays them back";

    private RecordCommand() {

    }

    static CommandResult execute(ArrayList<Token> tokens) {
        if (!(tokens.get(1) instanceof KeyWordToken(String keyword))) return CommandResult.fail("First token must be a valid keyword");
        if (!(tokens.get(2) instanceof KeyWordToken(String recordName))) return CommandResult.fail("Second token must be a keyword");

        if ("start".equalsIgnoreCase(keyword)) Game.getServer().addFunction(new RecordFunction(), recordName);
        else if ("cancel".equalsIgnoreCase(keyword)) Game.getServer().removeFunction(recordName);

        else if ("stop".equalsIgnoreCase(keyword)) {
            Function function = Game.getServer().removeFunction(recordName);
            if (!(function instanceof RecordFunction recordFunction)) {
                if (function != null) Game.getServer().addFunction(function, recordName);
                return CommandResult.fail("That function wasn't a Recorder");
            }

            PlayerRecord record = recordFunction.toRecord();
            new PlayerRecordSaver().save(record, PlayerRecordSaver.getSaveFileLocation(recordName));

        } else if ("play".equalsIgnoreCase(keyword)) {
            boolean playBackRotations = true;
            if (Token.isKeyWord(3, tokens)) playBackRotations = Boolean.parseBoolean(((KeyWordToken) tokens.get(3)).keyword());

            PlayerRecord record = new PlayerRecordSaver().load(PlayerRecordSaver.getSaveFileLocation(recordName));
            Game.getServer().addFunction(new RecordPlaybackFunction(record, playBackRotations), recordName);

        } else return CommandResult.fail("Keyword is invalid");
        return CommandResult.success();
    }

    private static class RecordFunction implements Function {

        private final ArrayList<Position> positions = new ArrayList<>();
        private final ArrayList<Vector3f> rotations = new ArrayList<>();

        public boolean run() {

            positions.add(Game.getPlayer().getPosition());
            rotations.add(Game.getPlayer().getCamera().getRotation());

            return true;
        }

        public PlayerRecord toRecord() {
            return new PlayerRecord(positions, rotations);
        }
    }

    private static class RecordPlaybackFunction implements Function {

        private final MovementState state = new FlyingState();
        private final ArrayList<Position> positions;
        private final ArrayList<Vector3f> rotations;
        private final boolean playbackRotations;
        private int index = 0;

        RecordPlaybackFunction(PlayerRecord record, boolean playbackRotations) {
            this.positions = record.positions();
            this.rotations = record.rotations();
            this.playbackRotations = playbackRotations;
        }

        public boolean run() {
            Player player = Game.getPlayer();
            if (index >= positions.size() && index >= rotations.size()) return false;

            if (index < positions.size()) player.setPosition(positions.get(index));
            if (index < rotations.size() && playbackRotations) player.getCamera().setRotation(rotations.get(index));

            if (index < positions.size() - 1) {
                Position current = positions.get(index);
                Position next = positions.get(index + 1);

                Vector3f movement = next.vectorFrom(current);
                player.getMovement().setVelocity(movement);
            }

            player.setNoClip(true);
            player.getMovement().setState(state);
            index++;
            return true;
        }
    }
}
