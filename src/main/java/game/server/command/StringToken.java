package game.server.command;

import core.utils.MainThread;

record StringToken(String string) implements Token {

    @Override
    @MainThread
    public TokenType type() {
        return TokenType.STRING;
    }
}
