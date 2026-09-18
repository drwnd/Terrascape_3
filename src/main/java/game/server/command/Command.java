package game.server.command;

import core.rendering_api.Debug;
import core.utils.MainThread;

public enum Command {

    ECHO(EchoCommand::execute, EchoCommand.EXPLANATION, EchoCommand.SYNTAX),
    TP(TPCommand::execute, TPCommand.EXPLANATION, TPCommand.SYNTAX),
    CLEAR(ClearCommand::execute, ClearCommand.EXPLANATION, ClearCommand.SYNTAX),
    RECORD(RecordCommand::execute, RecordCommand.EXPLANATION, RecordCommand.SYNTAX),
    RELOAD(ReloadCommand::execute, ReloadCommand.EXPLANATION, ReloadCommand.SYNTAX),
    CRASH(CrashCommand::execute, CrashCommand.EXPLANATION, CrashCommand.SYNTAX),
    SETTING(SettingCommand::execute, SettingCommand.EXPLANATION, SettingCommand.SYNTAX),
    TIME(TimeCommand::execute, TimeCommand.EXPLANATION, TimeCommand.SYNTAX),
    STRUCTURE(StructureCommand::execute, StructureCommand.EXPLANATION, StructureCommand.SYNTAX),
    HELP(HelpCommand::execute, HelpCommand.EXPLANATION, HelpCommand.SYNTAX);

    @MainThread
    public static CommandResult execute(String commandString) {
        try {
            TokenList tokens = Token.tokenize(commandString.substring(1));
            Command command = getCommand(tokens.expectNextKeyWord().keyword().toUpperCase());

            return command.executable.execute(tokens);
        } catch (CrashException crash) {
            throw crash;
        } catch (SyntaxError syntaxError) {
            return CommandResult.fail(syntaxError.getMessage());
        } catch (Exception exception) {
            Debug.err(exception);
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

    @MainThread
    static Command getCommand(String name) {
        try {
            return valueOf(name);
        } catch (Exception ignore) {
            throw new SyntaxError("Unrecognized Command : " + name);
        }
    }

    private final Executable executable;
    private final String explanation, syntax;

    private interface Executable {

        @MainThread
        CommandResult execute(TokenList tokens);

    }
}
