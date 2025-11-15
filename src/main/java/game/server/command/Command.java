package game.server.command;

public enum Command {

    ECHO(EchoCommand::execute);

    public static CommandResult execute(String commandString) {
        try {
            String[] tokens = commandString.substring(1).split(" ");
            Command command = valueOf(tokens[0].toUpperCase());
            return command.executable.execute(tokens, commandString);
        } catch (Exception exception) {
            return CommandResult.fail(exception.getMessage());
        }
    }

    Command(Executable executable) {
        this.executable = executable;
    }

    private final Executable executable;

    private interface Executable {

        CommandResult execute(String[] tokens, String commandString);

    }
}
