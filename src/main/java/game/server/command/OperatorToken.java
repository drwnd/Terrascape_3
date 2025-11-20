package game.server.command;

record OperatorToken(char operator) implements Token {

    @Override
    public TokenType type() {
        return TokenType.OPERATOR;
    }
}
