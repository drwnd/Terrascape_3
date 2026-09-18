package game.server.command;

import core.utils.MainThread;

import java.util.ArrayList;

final class TokenList extends ArrayList<Token> {

    private int index = -1;
    private final String command;

    @MainThread
    TokenList(String command) {
        this.command = command;
    }

    @MainThread
    Token get() {
        return index >= size() ? null : get(index);
    }

    @MainThread
    Token getNext() {
        index++;
        return get();
    }

    @MainThread
    String getCommand() {
        return command;
    }

    @MainThread
    boolean isFinished() {
        return index + 1 >= size();
    }

    @MainThread
    void next() {
        index++;
    }

    @MainThread
    void expectFinishedLess() {
        if (index < size() - 1) throw new SyntaxError("Too many tokens");
    }

    @MainThread
    void expectFinishedLessEqual() {
        if (index <= size() - 1) throw new SyntaxError("Too many tokens");
    }


    @MainThread
    KeywordToken expectNextKeyWord() {
        index++;
        return expectGetKeyWord();
    }

    @MainThread
    KeywordToken expectGetKeyWord() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof KeywordToken)) throw new SyntaxError("Expected Keyword but found " + getName(token));
        return (KeywordToken) token;
    }

    @MainThread
    Token nextIncrementKeyword() {
        index++;
        return getIncrementKeyword();
    }

    @MainThread
    Token getIncrementKeyword() {
        Token token = get();
        if (token instanceof KeywordToken) index++;
        return token;
    }


    @MainThread
    NumberToken expectNextNumber() {
        index++;
        return expectGetNumber();
    }

    @MainThread
    NumberToken expectGetNumber() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof NumberToken)) throw new SyntaxError("Expected Number but found " + getName(token));
        return (NumberToken) token;
    }

    @MainThread
    Token nextIncrementNumber() {
        index++;
        return getIncrementNumber();
    }

    @MainThread
    Token getIncrementNumber() {
        Token token = get();
        if (token instanceof NumberToken) index++;
        return token;
    }


    @MainThread
    StringToken expectNextString() {
        index++;
        return expectGetString();
    }

    @MainThread
    StringToken expectGetString() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof StringToken)) throw new SyntaxError("Expected String but found " + getName(token));
        return (StringToken) token;
    }

    @MainThread
    Token nextIncrementString() {
        index++;
        return getIncrementString();
    }

    @MainThread
    Token getIncrementString() {
        Token token = get();
        if (token instanceof StringToken) index++;
        return token;
    }


    @MainThread
    OperatorToken expectNextOperator() {
        index++;
        return expectGetOperator();
    }

    @MainThread
    OperatorToken expectGetOperator() {
        if (index >= size()) throw SyntaxError.TOO_FEW_TOKENS;
        Token token = get();
        if (!(token instanceof OperatorToken)) throw new SyntaxError("Expected Operator but found " + getName(token));
        return (OperatorToken) token;
    }

    @MainThread
    Token nextIncrementOperator() {
        index++;
        return getIncrementOperator();
    }

    @MainThread
    Token getIncrementOperator() {
        Token token = get();
        if (token instanceof OperatorToken) index++;
        return token;
    }

    @MainThread
    private static String getName(Token token) {
        return token == null ? "null" : token.type().name();
    }
}
