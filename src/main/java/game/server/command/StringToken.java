package game.server.command;

record StringToken(String string) implements Token {

    @Override
    public TokenType type() {
        return TokenType.STRING;
    }
}
