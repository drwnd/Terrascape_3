package game.server.command;

import java.util.ArrayList;

interface Token {

    TokenType type();

    static boolean isKeyWord(int tokenIndex, ArrayList<Token> tokens) {
        return tokens.size() > tokenIndex && tokens.get(tokenIndex).type() == TokenType.KEYWORD;
    }

    static boolean isString(int tokenIndex, ArrayList<Token> tokens) {
        return tokens.size() > tokenIndex && tokens.get(tokenIndex).type() == TokenType.STRING;
    }

    static boolean isNumber(int tokenIndex, ArrayList<Token> tokens) {
        return tokens.size() > tokenIndex && tokens.get(tokenIndex).type() == TokenType.NUMBER;
    }

    static boolean isOperator(int tokenIndex, ArrayList<Token> tokens) {
        return tokens.size() > tokenIndex && tokens.get(tokenIndex).type() == TokenType.OPERATOR;
    }

    static ArrayList<Token> tokenize(String command) {
        ArrayList<Token> tokens = new ArrayList<>();
        command += ' ';
        char[] chars = command.toCharArray();
        int index = 0;
        while (index < chars.length) index = addNextToken(command, chars, tokens, index);

        return tokens;
    }

    private static int addNextToken(String command, char[] chars, ArrayList<Token> tokens, int startIndex) {
        while (startIndex < chars.length && chars[startIndex] == ' ') startIndex++;
        if (startIndex == chars.length) return chars.length;
        char startChar = chars[startIndex];

        if (startChar == '"') {
            int stringEndIndex = nextIndexOf('"', chars, ++startIndex);
            tokens.add(new StringToken(command.substring(startIndex, stringEndIndex)));
            return stringEndIndex + 1;
        }
        if (Character.isDigit(startChar) || startChar == '-') {
            int numberEndIndex = nextIndexOf(' ', chars, startIndex);
            double number = Double.parseDouble(command.substring(startIndex, numberEndIndex));
            tokens.add(new NumberToken(number));
            return numberEndIndex + 1;
        }
        if (Character.isLetter(startChar)) {
            int keywordEndIndex = nextIndexOf(' ', chars, startIndex);
            tokens.add(new KeyWordToken(command.substring(startIndex, keywordEndIndex)));
            return keywordEndIndex + 1;
        }

        tokens.add(new OperatorToken(startChar));
        return startIndex + 1;
    }

    private static int nextIndexOf(char target, char[] chars, int startIndex) {
        while (startIndex < chars.length && chars[startIndex] != target) startIndex++;
        return startIndex == chars.length ? -65536 : startIndex;
    }
}
