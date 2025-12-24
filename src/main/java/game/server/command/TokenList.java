package game.server.command;

import java.util.ArrayList;

public final class TokenList extends ArrayList<Token> {

    private int index = -1;

    Token get() {
        return index >= size() ? null : get(index);
    }

    Token getNext() {
        index++;
        return get();
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


    KeywordToken expectNextKeyWord() {
        index++;
        return expectGetKeyWord();
    }

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

    Token getIncrementKeyword() {
        Token token = get();
        if (token instanceof KeywordToken) index++;
        return token;
    }


    NumberToken expectNextNumber() {
        index++;
        return expectGetNumber();
    }

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

    Token getIncrementNumber() {
        Token token = get();
        if (token instanceof NumberToken) index++;
        return token;
    }


    StringToken expectNextString() {
        index++;
        return expectGetString();
    }

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

    Token getIncrementString() {
        Token token = get();
        if (token instanceof StringToken) index++;
        return token;
    }


    OperatorToken expectNextOperator() {
        index++;
        return expectGetOperator();
    }

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

    Token getIncrementOperator() {
        Token token = get();
        if (token instanceof OperatorToken) index++;
        return token;
    }

    private static String getName(Token token) {
        return token == null ? "null" : token.type().name();
    }
}
