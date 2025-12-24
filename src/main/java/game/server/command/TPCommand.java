package game.server.command;

import game.server.Game;
import game.utils.Position;
import game.utils.Utils;

final class TPCommand {

    static final String SYNTAX = "[~]X-Coordinate [~]Y-Coordinate [~]Z-Coordinate // Adding ~ before a Coordinate adds that Coordinate to the Players Position";
    static final String EXPLANATION = "Teleports the Player to a specified Position";

    private TPCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        Position position = Game.getPlayer().getPosition();

        int intX = 0, intY = 0, intZ = 0;
        float fractionX = 0.0F, fractionY = 0.0F, fractionZ = 0.0F;
        boolean xSet = false, ySet = false, zSet = false;

        if (tokens.nextIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            intX = Utils.toFakeXZ(position.intX);
            fractionX = position.fractionX;
            xSet = true;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double x)) {
            intX += (int) Math.floor(x);
            fractionX += (float) (x - Math.floor(x));
            xSet = true;
        }

        if (tokens.getIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            intY = Utils.toFakeY(position.intY);
            fractionY = position.fractionY;
            ySet = true;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double y)) {
            intY += (int) Math.floor(y);
            fractionY += (float) (y - Math.floor(y));
            ySet = true;
        }

        if (tokens.getIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            intZ = Utils.toFakeXZ(position.intZ);
            fractionZ = position.fractionZ;
            zSet = true;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double z)) {
            intZ += (int) Math.floor(z);
            fractionZ += (float) (z - Math.floor(z));
            zSet = true;
        }
        tokens.expectFinishedLessEqual();

        if (!xSet || !ySet || !zSet) return CommandResult.fail("Not all coordinates specified");
        Game.getPlayer().setPosition(new Position(Utils.fromFakeXZ(intX), Utils.fromFakeY(intY), Utils.fromFakeXZ(intZ), fractionX, fractionY, fractionZ));
        Game.getServer().scheduleGeneratorRestart();
        return CommandResult.success();
    }
}
