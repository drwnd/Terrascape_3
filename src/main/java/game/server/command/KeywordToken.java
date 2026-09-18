package game.server.command;

import core.utils.MainThread;

record KeywordToken(String keyword) implements Token {

    @Override
    @MainThread
    public TokenType type() {
        return TokenType.KEYWORD;
    }
}
