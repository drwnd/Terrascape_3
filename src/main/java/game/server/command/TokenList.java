package game.server.command;

import java.util.ArrayList;

public final class TokenList extends ArrayList<Token> {

    private int index = -1;
    private final String command;

    TokenList(String command) {
        this.command = command;
    }

    Token get() {
        return index >= size() ? null : get(index);
    }

/**
 * Returns the next.
 * @return result
 */
    Token getNext() {
        index++;
        return get();
    }

    String getCommand() {
        return command;
    }

    boolean isFinished() {
        return index + 1 >= size();
    }

    void next() {
        index++;
    }

    void expectFinishedLess() {
        if (index < size() - 1) throw new SyntaxError("Too many tokens");
    }

    void expectFinishedLessEqual() {
        if (index <= size() - 1) throw new SyntaxError("Too many tokens");
    }


/**
 * Performs expect next key word.
 * @return result
 */
    KeywordToken expectNextKeyWord() {
        index++;
        return expectGetKeyWord();
    }

/**
 * Performs expect get key word.
 * @return result
 */
    KeywordToken expectGetKeyWord() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof KeywordToken)) throw new SyntaxError("Expected Keyword but found " + getName(token));
        return (KeywordToken) token;
    }

/**
 * Performs next increment keyword.
 * @return result
 */
    Token nextIncrementKeyword() {
        index++;
        return getIncrementKeyword();
    }

/**
 * Returns the increment keyword.
 * @return result
 */
    Token getIncrementKeyword() {
        Token token = get();
        if (token instanceof KeywordToken) index++;
        return token;
    }


/**
 * Performs expect next number.
 * @return result
 */
    NumberToken expectNextNumber() {
        index++;
        return expectGetNumber();
    }

/**
 * Performs expect get number.
 * @return result
 */
    NumberToken expectGetNumber() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof NumberToken)) throw new SyntaxError("Expected Number but found " + getName(token));
        return (NumberToken) token;
    }

/**
 * Performs next increment number.
 * @return result
 */
    Token nextIncrementNumber() {
        index++;
        return getIncrementNumber();
    }

/**
 * Returns the increment number.
 * @return result
 */
    Token getIncrementNumber() {
        Token token = get();
        if (token instanceof NumberToken) index++;
        return token;
    }


/**
 * Performs expect next string.
 * @return result
 */
    StringToken expectNextString() {
        index++;
        return expectGetString();
    }

/**
 * Performs expect get string.
 * @return result
 */
    StringToken expectGetString() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof StringToken)) throw new SyntaxError("Expected String but found " + getName(token));
        return (StringToken) token;
    }

/**
 * Performs next increment string.
 * @return result
 */
    Token nextIncrementString() {
        index++;
        return getIncrementString();
    }

/**
 * Returns the increment string.
 * @return result
 */
    Token getIncrementString() {
        Token token = get();
        if (token instanceof StringToken) index++;
        return token;
    }


/**
 * Performs expect next operator.
 * @return result
 */
    OperatorToken expectNextOperator() {
        index++;
        return expectGetOperator();
    }

/**
 * Performs expect get operator.
 * @return result
 */
    OperatorToken expectGetOperator() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof OperatorToken)) throw new SyntaxError("Expected Operator but found " + getName(token));
        return (OperatorToken) token;
    }

/**
 * Performs next increment operator.
 * @return result
 */
    Token nextIncrementOperator() {
        index++;
        return getIncrementOperator();
    }

/**
 * Returns the increment operator.
 * @return result
 */
    Token getIncrementOperator() {
        Token token = get();
        if (token instanceof OperatorToken) index++;
        return token;
    }

    private static String getName(Token token) {
        return token == null ? "null" : token.type().name();
    }
}
