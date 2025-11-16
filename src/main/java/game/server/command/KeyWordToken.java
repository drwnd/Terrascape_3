package game.server.command;

public record KeyWordToken(String keyword) implements Token {

    @Override
    public TokenType type() {
        return TokenType.KEYWORD;
    }
}
