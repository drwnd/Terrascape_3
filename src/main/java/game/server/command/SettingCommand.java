package game.server.command;

import core.renderables.KeySelector;
import core.settings.*;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.Option;
import game.server.Game;
import game.server.Server;

final class SettingCommand {

    static final String SYNTAX = "{get Setting_Name, set Setting_Name value, reload, list}";
    static final String EXPLANATION = "Queries a Setting, sets a setting value, reloads all Settings from file or shows a list of all settings";

    private SettingCommand() {

    }

    static CommandResult execute(TokenList tokens) {
        String keyword = tokens.expectNextKeyWord().keyword();

        if ("get".equalsIgnoreCase(keyword)) {
            String settingName = tokens.expectNextKeyWord().keyword();
            tokens.expectFinishedLess();

            Enum<?> setting = Settings.getSettingWithName(settingName);
            if (setting == null) return CommandResult.fail(settingName + " is not a Setting");

            String value = switch (setting) {
                case FloatSetting floatSetting -> Float.toString(floatSetting.value());
                case KeySetting keySetting -> KeySelector.getDisplayString(keySetting.value());
                case ToggleSetting toggleSetting -> Boolean.toString(toggleSetting.value());
                case OptionSetting optionSetting -> optionSetting.value().name();

                default -> "";
            };

            Game.getServer().sendServerMessage(settingName.toUpperCase() + " : " + value, ColorOption.WHITE);

        } else if ("set".equalsIgnoreCase(keyword)) {
            String settingName = tokens.expectNextKeyWord().keyword();

            Enum<?> setting = Settings.getSettingWithName(settingName);
            if (setting == null) return CommandResult.fail(settingName + " is not a Setting");
            Token value = tokens.getNext();
            tokens.expectFinishedLess();

            CommandResult result = switch (setting) {
                case FloatSetting floatSetting -> set(floatSetting, value);
                case KeySetting keySetting -> set(keySetting, value);
                case ToggleSetting toggleSetting -> set(toggleSetting, value);
                case OptionSetting optionSetting -> set(optionSetting, value);

                default -> CommandResult.fail("Unrecognized Setting type");
            };
            if (result.successful()) Settings.writeToFile();
            return result;

        } else if ("reload".equalsIgnoreCase(keyword)) {
            tokens.expectFinishedLess();
            Settings.loadFromFile();
        } else if ("list".equalsIgnoreCase(keyword)) {
            tokens.expectFinishedLess();
            Server server = Game.getServer();

            server.sendServerMessage("Float settings:", ColorOption.ORANGE);
            for (FloatSetting setting : FloatSetting.values()) server.sendServerMessage(setting.name(), ColorOption.WHITE);
            server.sendServerMessage("Key settings:", ColorOption.ORANGE);
            for (KeySetting setting : KeySetting.values()) server.sendServerMessage(setting.name(), ColorOption.WHITE);
            server.sendServerMessage("Toggle settings:", ColorOption.ORANGE);
            for (ToggleSetting setting : ToggleSetting.values()) server.sendServerMessage(setting.name(), ColorOption.WHITE);
            server.sendServerMessage("Option settings:", ColorOption.ORANGE);
            for (OptionSetting setting : OptionSetting.values()) server.sendServerMessage(setting.name(), ColorOption.WHITE);

        } else return CommandResult.fail("Unrecognized keyword : " + keyword);
        return CommandResult.success();
    }

    private static CommandResult set(FloatSetting setting, Token value) {
        if (!(value instanceof NumberToken(double number))) return CommandResult.fail("Value must be a Number for that setting");
        Settings.update(setting, (float) number);
        return CommandResult.success();
    }

    private static CommandResult set(KeySetting setting, Token value) {
        int codePoint;
        switch (value) {
            case KeywordToken(String keyword) -> {
                if (keyword.length() > 1) return CommandResult.fail("Key must be a single Character");
                codePoint = Character.toUpperCase(keyword.charAt(0));
            }
            case NumberToken number -> {
                if (!number.isInteger() || number.number() < 0 || number.number() > 0xFFFF) return CommandResult.fail("Codepoint must be within 0 to 65535");
                codePoint = (int) number.number();
            }
            case OperatorToken(char operator) -> codePoint = operator;
            case null, default -> {
                return CommandResult.fail("Value must be a single character or a number for that setting");
            }
        }

        Settings.update(setting, codePoint);
        return CommandResult.success();
    }

    private static CommandResult set(ToggleSetting setting, Token value) {
        if (!(value instanceof KeywordToken(String keyword)) || (!"true".equalsIgnoreCase(keyword) && !"false".equalsIgnoreCase(keyword)))
            return CommandResult.fail("Value must be true / false for that setting");
        boolean toggle = Boolean.parseBoolean(keyword);
        Settings.update(setting, toggle);
        return CommandResult.success();
    }

    private static CommandResult set(OptionSetting setting, Token value) {
        if (!(value instanceof KeywordToken(String keyword))) return CommandResult.fail("Value must be a keyword for that setting");
        Option option = setting.value().value(keyword);
        Settings.update(setting, option);
        return CommandResult.success();
    }
}
