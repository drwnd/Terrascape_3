package game.server.command;

import java.util.ArrayList;

public enum Command {

    ECHO(EchoCommand::execute, EchoCommand.EXPLANATION, EchoCommand.SYNTAX),
    TP(TPCommand::execute, TPCommand.EXPLANATION, TPCommand.SYNTAX),
    CLEAR(ClearCommand::execute, ClearCommand.EXPLANATION, ClearCommand.SYNTAX),
    HELP(HelpCommand::execute, HelpCommand.EXPLANATION, HelpCommand.SYNTAX);

    public static CommandResult execute(String commandString) {
        try {
            ArrayList<Token> tokens = Token.tokenize(commandString.substring(1));
            printTokens(tokens);
            Token commandType = tokens.getFirst();
            if (commandType.type() != TokenType.KEYWORD) return CommandResult.fail("Not a keyword");
            Command command = Command.valueOf(((KeyWordToken) commandType).keyword().toUpperCase());

            return command.executable.execute(tokens);
        } catch (Exception exception) {
            exception.printStackTrace();
            return CommandResult.fail(exception.getClass().getName() + " " + exception.getMessage());
        }
    }

    Command(Executable executable, String explanation, String syntax) {
        this.executable = executable;
        this.explanation = explanation;
        this.syntax = syntax;
    }

    String getExplanation() {
        return explanation;
    }

    String getSyntax() {
        return syntax;
    }

    private static void printTokens(ArrayList<Token> tokens) {
        for (Token token : tokens) {
            switch (token.type()) {
                case STRING -> System.out.println("String   #" + ((StringToken) token).string() + '#');
                case KEYWORD -> System.out.println("Keyword  #" + ((KeyWordToken) token).keyword() + '#');
                case NUMBER -> System.out.println("Number   #" + ((NumberToken) token).number() + '#');
                case OPERATOR -> System.out.println("Operator #" + ((OperatorToken) token).operator() + '#');
            }
        }
    }

    private final Executable executable;
    private final String explanation, syntax;

    private interface Executable {

        CommandResult execute(ArrayList<Token> tokens);

    }
}
