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

    /**
     * Checks if all tokens have been consumed. Throws a SyntaxError if there are more than one token remaining.
     *
     * @throws SyntaxError if there are too many tokens
     */
    void expectFinishedLess() {
        if (index < size() - 1) throw new SyntaxError("Too many tokens");
    }

    /**
     * Checks if all tokens have been consumed. Throws a SyntaxError if there are any tokens remaining.
     *
     * @throws SyntaxError if there are too many tokens
     */
    void expectFinishedLessEqual() {
        if (index <= size() - 1) throw new SyntaxError("Too many tokens");
    }


    /**
     * Advances to the next token and expects it to be a keyword.
     *
     * @return the next token as a KeywordToken
     * @throws SyntaxError if the next token is not a keyword or if there are no more tokens
     */
    KeywordToken expectNextKeyWord() {
        index++;
        return expectGetKeyWord();
    }

    /**
     * Returns the current token and expects it to be a keyword.
     *
     * @return the current token as a KeywordToken
     * @throws SyntaxError if the current token is not a keyword or if the index is out of bounds
     */
    KeywordToken expectGetKeyWord() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof KeywordToken)) throw new SyntaxError("Expected Keyword but found " + getName(token));
        return (KeywordToken) token;
    }

    Token nextIncrementKeyword() {
        index++;
        return getIncrementKeyword();
    }

    /**
     * Advances the index if the current token is a keyword.
     *
     * @return the current token
     */
    Token getIncrementKeyword() {
        Token token = get();
        if (token instanceof KeywordToken) index++;
        return token;
    }


    /**
     * Advances to the next token and expects it to be a number.
     *
     * @return the next token as a NumberToken
     * @throws SyntaxError if the next token is not a number or if there are no more tokens
     */
    NumberToken expectNextNumber() {
        index++;
        return expectGetNumber();
    }

    /**
     * Returns the current token and expects it to be a number.
     *
     * @return the current token as a NumberToken
     * @throws SyntaxError if the current token is not a number or if the index is out of bounds
     */
    NumberToken expectGetNumber() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof NumberToken)) throw new SyntaxError("Expected Number but found " + getName(token));
        return (NumberToken) token;
    }

    Token nextIncrementNumber() {
        index++;
        return getIncrementNumber();
    }

    /**
     * Advances the index if the current token is a number.
     *
     * @return the current token
     */
    Token getIncrementNumber() {
        Token token = get();
        if (token instanceof NumberToken) index++;
        return token;
    }


    /**
     * Advances to the next token and expects it to be a string.
     *
     * @return the next token as a StringToken
     * @throws SyntaxError if the next token is not a string or if there are no more tokens
     */
    StringToken expectNextString() {
        index++;
        return expectGetString();
    }

    /**
     * Returns the current token and expects it to be a string.
     *
     * @return the current token as a StringToken
     * @throws SyntaxError if the current token is not a string or if the index is out of bounds
     */
    StringToken expectGetString() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof StringToken)) throw new SyntaxError("Expected String but found " + getName(token));
        return (StringToken) token;
    }

    Token nextIncrementString() {
        index++;
        return getIncrementString();
    }

    /**
     * Advances the index if the current token is a string.
     *
     * @return the current token
     */
    Token getIncrementString() {
        Token token = get();
        if (token instanceof StringToken) index++;
        return token;
    }


    /**
     * Advances to the next token and expects it to be an operator.
     *
     * @return the next token as an OperatorToken
     * @throws SyntaxError if the next token is not an operator or if there are no more tokens
     */
    OperatorToken expectNextOperator() {
        index++;
        return expectGetOperator();
    }

    /**
     * Returns the current token and expects it to be an operator.
     *
     * @return the current token as an OperatorToken
     * @throws SyntaxError if the current token is not an operator or if the index is out of bounds
     */
    OperatorToken expectGetOperator() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof OperatorToken)) throw new SyntaxError("Expected Operator but found " + getName(token));
        return (OperatorToken) token;
    }

    Token nextIncrementOperator() {
        index++;
        return getIncrementOperator();
    }

    /**
     * Advances the index if the current token is an operator.
     *
     * @return the current token
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
