package game.server.command;

import core.utils.MainThread;

class SyntaxError extends RuntimeException {

    static final SyntaxError TOO_FEW_TOKENS = new SyntaxError("Too few Tokens");

    @MainThread
    SyntaxError(String message) {
        super(message);
    }
}
