package game.server.command;

import game.server.Game;
import game.utils.Position;

final class TPCommand {

    static final String SYNTAX = "[~]X-Coordinate [~]Y-Coordinate [~]Z-Coordinate [abs] // Adding ~ before a Coordinate adds that Coordinate to the Players Position. Adding \"abs\" moves the origin to the corner of the world";
    static final String EXPLANATION = "Teleports the Player to a specified Position";

    private TPCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        Position position = Game.getPlayer().getPosition();

        int intX = 0, intY = 0, intZ = 0;
        float fractionX = 0.0F, fractionY = 0.0F, fractionZ = 0.0F;
        boolean xSet = false, ySet = false, zSet = false;
        boolean xAbsolute = true, yAbsolute = true, zAbsolute = true;

        if (tokens.nextIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            intX = position.intX;
            fractionX = position.fractionX;
            xSet = true;
            xAbsolute = false;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double x)) {
            intX += (int) Math.floor(x);
            fractionX += (float) (x - Math.floor(x));
            xSet = true;
        }

        if (tokens.getIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            intY = position.intY;
            fractionY = position.fractionY;
            ySet = true;
            yAbsolute = false;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double y)) {
            intY += (int) Math.floor(y);
            fractionY += (float) (y - Math.floor(y));
            ySet = true;
        }

        if (tokens.getIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            intZ = position.intZ;
            fractionZ = position.fractionZ;
            zSet = true;
            zAbsolute = false;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double z)) {
            intZ += (int) Math.floor(z);
            fractionZ += (float) (z - Math.floor(z));
            zSet = true;
        }

        if (tokens.getIncrementKeyword() instanceof KeywordToken(String keyword)) {
            if ("abs".equalsIgnoreCase(keyword)) {
                if (xAbsolute) intX = intX - Integer.MAX_VALUE;
                if (yAbsolute) intY = intY - Integer.MAX_VALUE;
                if (zAbsolute) intZ = intZ - Integer.MAX_VALUE;
            } else return CommandResult.fail("Unrecognized Coordinate type " + keyword);
        }

        tokens.expectFinishedLessEqual();

        if (!xSet || !ySet || !zSet) return CommandResult.fail("Not all coordinates specified");
        Game.getPlayer().setPosition(new Position(intX, intY, intZ, fractionX, fractionY, fractionZ));
        Game.getServer().scheduleGeneratorRestart();
        return CommandResult.success();
    }
}
