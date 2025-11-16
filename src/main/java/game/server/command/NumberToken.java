package game.server.command;

public record NumberToken(double number) implements Token {

    public boolean isInteger() {
        if (Double.isNaN(number) || Double.isInfinite(number) || number <= Integer.MIN_VALUE || number >= Integer.MAX_VALUE) return false;
        return number == Math.floor(number);
    }

    @Override
    public TokenType type() {
        return TokenType.NUMBER;
    }
}
