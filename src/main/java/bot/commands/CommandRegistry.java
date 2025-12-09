package bot.commands;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {
    private static final Map<String, Command> COMMANDS = new HashMap<>();

    static {
        // Регистрируем команды
        AboutCommand about = new AboutCommand();
        AuthorsCommand authors = new AuthorsCommand();
        
        COMMANDS.put(about.getCommandName(), about);
        COMMANDS.put(authors.getCommandName(), authors);

        // Help-команда должна быть зарегистрирована ПОСЛЕДНЕЙ, чтобы видеть все команды
        HelpCommand help = new HelpCommand(COMMANDS);
        COMMANDS.put(help.getCommandName(), help);
    }

    public static Command getCommand(String commandName) {
        return COMMANDS.get(commandName);
    }

    public static Map<String, Command> getAllCommands() {
        return new HashMap<>(COMMANDS); // защищаем от модификации извне
    }
}