package game.server.command;

import core.utils.MainThread;

import game.assets.EntityType;
import game.entities.Entity;
import game.server.Game;
import game.utils.Position;

public final class SummonCommand {

    static final String SYNTAX = "(ENTITY_TYPE | random) [~]X-Coordinate [~]Y-Coordinate [~]Z-Coordinate [abs] // Adding ~ before a Coordinate adds that Coordinate to the Players Position.";
    static final String EXPLANATION = "Summons an Entity of the EntityType specified by ENTITY_TYPE at the specified Position. random spawns a random Entity";

    private SummonCommand() {

    }

    @MainThread
    static CommandResult execute(TokenList tokens) {
        Position position = Game.getPlayer().getPosition();
        String entityName = tokens.expectNextKeyWord().keyword();
        EntityType entityType;
        if ("random".equalsIgnoreCase(entityName)) {
            EntityType[] types = EntityType.values();
            entityType = types[(int) (Math.random() * types.length)];
        } else
            entityType = EntityType.valueOf(entityName.toUpperCase());

        long longX = 0, longY = 0, longZ = 0;
        float fractionX = 0.0F, fractionY = 0.0F, fractionZ = 0.0F;
        boolean xSet = false, ySet = false, zSet = false;

        if (tokens.nextIncrementOperator() instanceof OperatorToken(char operator)) {
            if (operator != '~') return CommandResult.fail("Bad Operator : " + operator);
            longX = position.longX;
            fractionX = position.fractionX;
            xSet = true;
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
        }
        if (tokens.getIncrementNumber() instanceof NumberToken(double z)) {
            longZ += (long) Math.floor(z);
            fractionZ += (float) (z - Math.floor(z));
            zSet = true;
        }
        tokens.expectFinishedLessEqual();
        if (!xSet || !ySet || !zSet) return CommandResult.fail("Not all coordinates specified");

        Entity entity = entityType.getEntity(new Position(longX, longY, longZ, fractionX, fractionY, fractionZ));
        Game.getServer().spawnEntity(entity);
        return CommandResult.success();
    }
}
