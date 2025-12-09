package bot.commands;

import java.util.HashMap;
import java.util.Map;

import bot.commands.notes.AddNoteCommand;
import bot.commands.notes.EditNoteCommand;
import bot.commands.notes.RemoveNoteCommand;
import bot.commands.notes.ShowNoteCommand;
import bot.commands.reminders.AddOnceReminderCommand;
import bot.commands.reminders.AddRecurringReminderCommand;
import bot.commands.reminders.EditOnceReminderCommand;
import bot.commands.reminders.EditRecurringReminderCommand;
import bot.commands.reminders.RemoveReminderCommand;
import bot.commands.reminders.ShowReminderCommand;
import bot.commands.timezone.SetOrEditTimezoneCommand;

public class CommandRegistry {
    private static final Map<String, Command> COMMANDS = new HashMap<>();

    static {
        // Регистрируем команды
        AboutCommand about = new AboutCommand();
        AuthorsCommand authors = new AuthorsCommand();
        //Note
        AddNoteCommand addNote = new AddNoteCommand();
        RemoveNoteCommand removeNote = new RemoveNoteCommand();
        EditNoteCommand editNote = new EditNoteCommand();
        ShowNoteCommand showNote = new ShowNoteCommand();
        //Reminder
        AddOnceReminderCommand addOnceReminder = new AddOnceReminderCommand();
        EditOnceReminderCommand editOnceReminder = new EditOnceReminderCommand();
        AddRecurringReminderCommand addRecurringReminder = new AddRecurringReminderCommand();
        EditRecurringReminderCommand editRecurringReminder = new EditRecurringReminderCommand();
        RemoveReminderCommand removeReminder = new RemoveReminderCommand();
        ShowReminderCommand showReminder = new ShowReminderCommand();
        //Timezone
        SetOrEditTimezoneCommand setOrEditTimezone = new SetOrEditTimezoneCommand();
        
        COMMANDS.put(about.getCommandName(), about);
        COMMANDS.put(authors.getCommandName(), authors);
        //Note
        COMMANDS.put(addNote.getCommandName(), addNote);
        COMMANDS.put(removeNote.getCommandName(), removeNote);
        COMMANDS.put(editNote.getCommandName(), editNote);
        COMMANDS.put(showNote.getCommandName(), showNote);
        //Reminder
        COMMANDS.put(addOnceReminder.getCommandName(), addOnceReminder);
        COMMANDS.put(editOnceReminder.getCommandName(), editOnceReminder);
        COMMANDS.put(addRecurringReminder.getCommandName(), addRecurringReminder);
        COMMANDS.put(editRecurringReminder.getCommandName(), editRecurringReminder);
        COMMANDS.put(removeReminder.getCommandName(), removeReminder);
        COMMANDS.put(showReminder.getCommandName(), showReminder);
        //Timezone
        COMMANDS.put(setOrEditTimezone.getCommandName(), setOrEditTimezone);

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