package game.server.command;

record KeyWordToken(String keyword) implements Token {

    @Override
    public TokenType type() {
        return TokenType.KEYWORD;
    }
}
