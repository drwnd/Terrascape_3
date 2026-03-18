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

        long longX = 0, longY = 0, longZ = 0;
        float fractionX = 0.0F, fractionY = 0.0F, fractionZ = 0.0F;
        boolean xSet = false, ySet = false, zSet = false;
        boolean xAbsolute = true, yAbsolute = true, zAbsolute = true;

        if (tokens.nextIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            longX = position.longX;
            fractionX = position.fractionX;
            xSet = true;
            xAbsolute = false;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double x)) {
            longX += (long) Math.floor(x);
            fractionX += (float) (x - Math.floor(x));
            xSet = true;
        }

        if (tokens.getIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            longY = position.longY;
            fractionY = position.fractionY;
            ySet = true;
            yAbsolute = false;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double y)) {
            longY += (long) Math.floor(y);
            fractionY += (float) (y - Math.floor(y));
            ySet = true;
        }

        if (tokens.getIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            longZ = position.longZ;
            fractionZ = position.fractionZ;
            zSet = true;
            zAbsolute = false;
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double z)) {
            longZ += (long) Math.floor(z);
            fractionZ += (float) (z - Math.floor(z));
            zSet = true;
        }

        if (tokens.getIncrementKeyword() instanceof KeywordToken(String keyword)) {
            if ("abs".equalsIgnoreCase(keyword)) {
                if (xAbsolute) longX = longX - Long.MAX_VALUE;
                if (yAbsolute) longY = longY - Long.MAX_VALUE;
                if (zAbsolute) longZ = longZ - Long.MAX_VALUE;
            } else return CommandResult.fail("Unrecognized Coordinate type " + keyword);
        }

        tokens.expectFinishedLessEqual();

        if (!xSet || !ySet || !zSet) return CommandResult.fail("Not all coordinates specified");
        Game.getPlayer().setPosition(new Position(longX, longY, longZ, fractionX, fractionY, fractionZ));
        Game.getServer().scheduleGeneratorRestart();
        return CommandResult.success();
    }
}
