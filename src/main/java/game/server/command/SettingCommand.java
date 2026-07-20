package game.server.command;

import core.renderables.KeySelector;
import core.settings.*;
import core.settings.optionSettings.ColorOption;
import core.settings.optionSettings.Option;

import game.server.Game;
import game.server.Server;

import java.util.ArrayList;

final class SettingCommand {

    static final String SYNTAX = "{get Setting_Name, reset Setting_Name, set Setting_Name value, reload, list}";
    static final String EXPLANATION = "Queries a Setting, sets a setting value, reloads all Settings from file or shows a list of all settings";

    private SettingCommand() {

    }

/**
 * Performs execute.
 *
 * @param tokens parameter
 * @return result
 */
    static CommandResult execute(TokenList tokens) {
        String keyword = tokens.expectNextKeyWord().keyword();

        if ("get".equalsIgnoreCase(keyword)) {
            String settingName = tokens.expectNextKeyWord().keyword();
            tokens.expectFinishedLess();

            Setting setting = Settings.getSettingWithName(settingName);
            if (setting == null) return CommandResult.fail(settingName + " is not a Setting");

            String value = switch (setting) {
                case FloatSetting floatSetting -> Float.toString(floatSetting.value());
                case IntSetting intSetting -> Integer.toString(intSetting.value());
                case ToggleSetting toggleSetting -> Boolean.toString(toggleSetting.value());
                case OptionSetting optionSetting -> optionSetting.value().name();
                case KeySetting keySetting -> KeySelector.getDisplayString(keySetting.keybind());

                default -> "";
            };

            Game.getServer().sendServerMessage(settingName.toUpperCase() + " : " + value, ColorOption.WHITE);

        } else if ("set".equalsIgnoreCase(keyword)) {
            String settingName = tokens.expectNextKeyWord().keyword();

            Setting setting = Settings.getSettingWithName(settingName);
            if (setting == null) return CommandResult.fail(settingName + " is not a Setting");
            Token value = tokens.getNext();
            tokens.expectFinishedLess();

            CommandResult result = switch (setting) {
                case FloatSetting floatSetting -> set(floatSetting, value);
                case IntSetting intSetting -> set(intSetting, value);
                case ToggleSetting toggleSetting -> set(toggleSetting, value);
                case OptionSetting optionSetting -> set(optionSetting, value);
                case KeySetting keySetting -> set(keySetting, value);

                default -> CommandResult.fail("Unrecognized Setting type");
            };
            if (result.successful()) Settings.writeToFile();
            return result;

        } else if ("reset".equalsIgnoreCase(keyword)) {
            String settingName = tokens.expectNextKeyWord().keyword();

            Setting setting = Settings.getSettingWithName(settingName);
            if (setting == null) return CommandResult.fail(settingName + " is not a Setting");
            tokens.expectFinishedLess();

            switch (setting) {
                case FloatSetting floatSetting -> floatSetting.setValue(floatSetting.defaultValue());
                case IntSetting intSetting -> intSetting.setValue(intSetting.defaultValue());
                case ToggleSetting toggleSetting -> toggleSetting.setValue(toggleSetting.value());
                case OptionSetting optionSetting -> optionSetting.setValue(optionSetting.defaultValue());
                case KeySetting keySetting -> keySetting.setKeybind(keySetting.defaultKeybind());
                default -> {
                    return CommandResult.fail("Unrecognized Setting type");
                }
            }
            Settings.writeToFile();
            return CommandResult.success();
        } else if ("reload".equalsIgnoreCase(keyword)) {
            tokens.expectFinishedLess();
            Settings.loadFromFile();
        } else if ("list".equalsIgnoreCase(keyword)) {
            tokens.expectFinishedLess();
            Server server = Game.getServer();
            ArrayList<Setting> settings = Settings.getSettings();

            server.sendServerMessage("Float settings:", ColorOption.ORANGE);
            for (Setting setting : settings) if (setting instanceof FloatSetting) server.sendServerMessage(setting.name(), ColorOption.WHITE);
            server.sendServerMessage("Int settings:", ColorOption.ORANGE);
            for (Setting setting : settings) if (setting instanceof IntSetting) server.sendServerMessage(setting.name(), ColorOption.WHITE);
            server.sendServerMessage("Key settings:", ColorOption.ORANGE);
            for (Setting setting : settings) if (setting instanceof KeySetting) server.sendServerMessage(setting.name(), ColorOption.WHITE);
            server.sendServerMessage("Toggle settings:", ColorOption.ORANGE);
            for (Setting setting : settings) if (setting instanceof ToggleSetting) server.sendServerMessage(setting.name(), ColorOption.WHITE);
            server.sendServerMessage("Option settings:", ColorOption.ORANGE);
            for (Setting setting : settings) if (setting instanceof OptionSetting) server.sendServerMessage(setting.name(), ColorOption.WHITE);

        } else return CommandResult.fail("Unrecognized keyword : " + keyword);
        return CommandResult.success();
    }

/**
 * Performs set.
 *
 * @param setting parameter
 * @param value parameter
 * @return result
 */
    private static CommandResult set(FloatSetting setting, Token value) {
        if (!(value instanceof NumberToken(double number))) return CommandResult.fail("Value must be a Number for that setting");
        if (number < setting.min() || number > setting.max())
            return CommandResult.fail("Value %f is out of allowed range [%f, %f]".formatted(number, setting.min(), setting.max()));
        setting.setValue((float) number);
        return CommandResult.success();
    }

/**
 * Performs set.
 *
 * @param setting parameter
 * @param value parameter
 * @return result
 */
    private static CommandResult set(IntSetting setting, Token value) {
        if (!(value instanceof NumberToken(double number)) || (int) number != number)
            return CommandResult.fail("Value must be an Integer for that setting");
        if ((int) number < setting.min() || (int) number > setting.max())
            return CommandResult.fail("Value %d is out of allowed range [%d, %d]".formatted((int) number, setting.min(), setting.max()));
        setting.setValue((int) number);
        return CommandResult.success();
    }

/**
 * Performs set.
 *
 * @param setting parameter
 * @param value parameter
 * @return result
 */
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

        setting.setKeybind(codePoint);
        return CommandResult.success();
    }

/**
 * Performs set.
 *
 * @param setting parameter
 * @param value parameter
 * @return result
 */
    private static CommandResult set(ToggleSetting setting, Token value) {
        if (!(value instanceof KeywordToken(String keyword)) || (!"true".equalsIgnoreCase(keyword) && !"false".equalsIgnoreCase(keyword)))
            return CommandResult.fail("Value must be true / false for that setting");
        boolean toggle = Boolean.parseBoolean(keyword);
        setting.setValue(toggle);
        return CommandResult.success();
    }

/**
 * Performs set.
 *
 * @param setting parameter
 * @param value parameter
 * @return result
 */
    private static CommandResult set(OptionSetting setting, Token value) {
        String optionValue;

        if (value instanceof KeywordToken(String keyword)) optionValue = keyword;
        else if (value instanceof StringToken(String string)) optionValue = string;
        else return CommandResult.fail("Value must be a keyword or a string for that setting");

        Option option = setting.value().value(optionValue);
        setting.setValue(option);
        return CommandResult.success();
    }
}
