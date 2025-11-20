package game.server.command;

import game.server.Game;
import game.utils.Position;

import java.util.ArrayList;

final class TPCommand {

    static final String SYNTAX = "[~]X-Coordinate [~]Y-Coordinate [~]Z-Coordinate // Adding ~ before a Coordinate adds that Coordinate to the Players Position";
    static final String EXPLANATION = "Teleports the Player to a specified Position";

    private TPCommand() {

    }

    static CommandResult execute(ArrayList<Token> tokens) {
        int tokenIndex = 1;
        Position position = Game.getPlayer().getPosition();

        int intX = 0, intY = 0, intZ = 0;
        float fractionX = 0.0F, fractionY = 0.0F, fractionZ = 0.0F;
        boolean xSet = false, ySet = false, zSet = false;

        if (Token.isOperator(tokenIndex, tokens)) {
            OperatorToken operatorToken = (OperatorToken) tokens.get(tokenIndex);
            if (operatorToken.operator() != '~') return CommandResult.fail("Bad Operator : " + operatorToken.operator());
            intX = position.intX;
            fractionX = position.fractionX;
            xSet = true;
            tokenIndex++;
        }
        if (Token.isNumber(tokenIndex, tokens)) {
            double x = ((NumberToken) tokens.get(tokenIndex)).number();
            intX += (int) Math.floor(x);
            fractionX += (float) (x - Math.floor(x));
            xSet = true;
            tokenIndex++;
        }

        if (Token.isOperator(tokenIndex, tokens)) {
            OperatorToken operatorToken = (OperatorToken) tokens.get(tokenIndex);
            if (operatorToken.operator() != '~') return CommandResult.fail("Bad Operator : " + operatorToken.operator());
            intY = position.intY;
            fractionY = position.fractionY;
            ySet = true;
            tokenIndex++;
        }
        if (Token.isNumber(tokenIndex, tokens)) {
            double y = ((NumberToken) tokens.get(tokenIndex)).number();
            intY += (int) Math.floor(y);
            fractionY += (float) (y - Math.floor(y));
            ySet = true;
            tokenIndex++;
        }

        if (Token.isOperator(tokenIndex, tokens)) {
            OperatorToken operatorToken = (OperatorToken) tokens.get(tokenIndex);
            if (operatorToken.operator() != '~') return CommandResult.fail("Bad Operator : " + operatorToken.operator());
            intZ = position.intZ;
            fractionZ = position.fractionZ;
            zSet = true;
            tokenIndex++;
        }
        if (Token.isNumber(tokenIndex, tokens)) {
            double z = ((NumberToken) tokens.get(tokenIndex)).number();
            intZ += (int) Math.floor(z);
            fractionZ += (float) (z - Math.floor(z));
            zSet = true;
        }

        if (!xSet || !ySet || !zSet) return CommandResult.fail("Not all coordinates specified");
        Game.getPlayer().setPosition(new Position(intX, intY, intZ, fractionX, fractionY, fractionZ));
        Game.getServer().scheduleGeneratorRestart();
        return CommandResult.success();
    }
}
