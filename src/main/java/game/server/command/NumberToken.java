package game.server.command;

import core.utils.MainThread;

record NumberToken(double number) implements Token {

    @MainThread
    public boolean isInteger() {
        if (Double.isNaN(number) || Double.isInfinite(number) || number <= Integer.MIN_VALUE || number >= Integer.MAX_VALUE) return false;
        return number == Math.floor(number);
    }

    @Override
    @MainThread
    public TokenType type() {
        return TokenType.NUMBER;
    }
}
