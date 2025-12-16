package game.server.command;

record KeywordToken(String keyword) implements Token {

    @Override
    public TokenType type() {
        return TokenType.KEYWORD;
    }
}
