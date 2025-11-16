package game.server.command;

public record OperatorToken(char operator) implements Token {
    @Override
    public TokenType type() {
        return TokenType.OPERATOR;
    }
}
