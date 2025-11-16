package game.server.command;

public record StringToken(String string) implements Token {
    @Override
    public TokenType type() {
        return TokenType.STRING;
    }
}
