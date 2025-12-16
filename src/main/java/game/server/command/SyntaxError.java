package game.server.command;

class SyntaxError extends RuntimeException {

    static final SyntaxError TOO_FEW_TOKENS = new SyntaxError("Too few Tokens");

    SyntaxError(String message) {
        super(message);
    }
}
