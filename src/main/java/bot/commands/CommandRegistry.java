package bot.commands;

import java.util.HashMap;
import java.util.Map;

import bot.commands.notes.AddNoteCommand;
import bot.commands.notes.EditNoteCommand;
import bot.commands.notes.RemoveNoteCommand;
import bot.commands.notes.ShowNoteCommand;

public class CommandRegistry {
    private static final Map<String, Command> COMMANDS = new HashMap<>();

    static {
        // Регистрируем команды
        AboutCommand about = new AboutCommand();
        AuthorsCommand authors = new AuthorsCommand();
        AddNoteCommand addNote = new AddNoteCommand();
        RemoveNoteCommand removeNote = new RemoveNoteCommand();
        EditNoteCommand editNote = new EditNoteCommand();
        ShowNoteCommand showNote = new ShowNoteCommand();
        
        COMMANDS.put(about.getCommandName(), about);
        COMMANDS.put(authors.getCommandName(), authors);
        COMMANDS.put(addNote.getCommandName(), addNote);
        COMMANDS.put(removeNote.getCommandName(), removeNote);
        COMMANDS.put(editNote.getCommandName(), editNote);
        COMMANDS.put(showNote.getCommandName(), showNote);

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