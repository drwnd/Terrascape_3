package game.server.command;

import core.utils.MainThread;

record OperatorToken(char operator) implements Token {

    @Override
    @MainThread
    public TokenType type() {
        return TokenType.OPERATOR;
    }
}
