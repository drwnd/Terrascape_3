package game.server.command;

public enum Command {

    ECHO(EchoCommand::execute, EchoCommand.EXPLANATION, EchoCommand.SYNTAX),
    TP(TPCommand::execute, TPCommand.EXPLANATION, TPCommand.SYNTAX),
    CLEAR(ClearCommand::execute, ClearCommand.EXPLANATION, ClearCommand.SYNTAX),
    RECORD(RecordCommand::execute, RecordCommand.EXPLANATION, RecordCommand.SYNTAX),
    RELOAD(ReloadCommand::execute, ReloadCommand.EXPLANATION, ReloadCommand.SYNTAX),
    CRASH(CrashCommand::execute, CrashCommand.EXPLANATION, CrashCommand.SYNTAX),
    HELP(HelpCommand::execute, HelpCommand.EXPLANATION, HelpCommand.SYNTAX);

    public static CommandResult execute(String commandString) {
        try {
            TokenList tokens = Token.tokenize(commandString.substring(1));
//            printTokens(tokens);
            Command command = getCommand(tokens.expectNextKeyWord().keyword().toUpperCase());

            return command.executable.execute(tokens);
        } catch (CrashException exception) {
            throw exception;
        } catch (Exception exception) {
            exception.printStackTrace();
            return CommandResult.fail(exception.getClass().getSimpleName() + " " + exception.getMessage());
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

    static Command getCommand(String name) {
        try {
            return valueOf(name);
        } catch (Exception ignore) {
            throw new SyntaxError("Unrecognized Command : " + name);
        }
    }

//    private static void printTokens(TokenList tokens) {
//        for (Token token : tokens) {
//            switch (token.type()) {
//                case STRING -> System.out.println("String   #" + ((StringToken) token).string() + '#');
//                case KEYWORD -> System.out.println("Keyword  #" + ((KeywordToken) token).keyword() + '#');
//                case NUMBER -> System.out.println("Number   #" + ((NumberToken) token).number() + '#');
//                case OPERATOR -> System.out.println("Operator #" + ((OperatorToken) token).operator() + '#');
//            }
//        }
//    }

    private final Executable executable;
    private final String explanation, syntax;

    private interface Executable {

        CommandResult execute(TokenList tokens);

    }
}
