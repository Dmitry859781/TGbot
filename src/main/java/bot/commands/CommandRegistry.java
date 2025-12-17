package bot.commands;

import java.util.HashMap;
import java.util.Map;

import bot.commands.notes.AddNoteCommand;
import bot.commands.notes.EditNoteCommand;
import bot.commands.notes.RemoveNoteCommand;
import bot.commands.notes.ShowNoteCommand;
import bot.commands.reminders.AddDelayedReminderCommand;
import bot.commands.reminders.AddOnceReminderCommand;
import bot.commands.reminders.AddPersistentReminderCommand;
import bot.commands.reminders.AddRecurringReminderCommand;
import bot.commands.reminders.EditDelayedReminderCommand;
import bot.commands.reminders.EditOnceReminderCommand;
import bot.commands.reminders.EditRecurringReminderCommand;
import bot.commands.reminders.ExportReminderCommand;
import bot.commands.reminders.ImportReminderCommand;
import bot.commands.reminders.RemoveReminderCommand;
import bot.commands.reminders.ShowReminderCommand;
import bot.commands.reminders.group.AddGroupCommand;
import bot.commands.reminders.group.AddToGroupCommand;
import bot.commands.reminders.group.DisableGroupCommand;
import bot.commands.reminders.group.EnableGroupCommand;
import bot.commands.reminders.group.ListGroupsCommand;
import bot.commands.reminders.group.RemoveFromGroupCommand;
import bot.commands.reminders.group.ShowGroupCommand;
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
        AddDelayedReminderCommand addDelayedReminder = new AddDelayedReminderCommand();
        AddPersistentReminderCommand addPersistentReminder = new AddPersistentReminderCommand();
        EditDelayedReminderCommand editDelayedReminder = new EditDelayedReminderCommand();
        AddRecurringReminderCommand addRecurringReminder = new AddRecurringReminderCommand();
        EditRecurringReminderCommand editRecurringReminder = new EditRecurringReminderCommand();
        RemoveReminderCommand removeReminder = new RemoveReminderCommand();
        ShowReminderCommand showReminder = new ShowReminderCommand();
        //Timezone
        SetOrEditTimezoneCommand setOrEditTimezone = new SetOrEditTimezoneCommand();
        //Import/Export
        ImportReminderCommand importReminder = new ImportReminderCommand();
        ExportReminderCommand exportReminder = new ExportReminderCommand();
        //Group
        AddGroupCommand addGroup = new AddGroupCommand();
        AddToGroupCommand addToGroup = new AddToGroupCommand();
        DisableGroupCommand disableGroup = new DisableGroupCommand();
        EnableGroupCommand enableGroup = new EnableGroupCommand();
        ListGroupsCommand listGroups = new ListGroupsCommand();
        RemoveFromGroupCommand removeFromGroup = new RemoveFromGroupCommand();
        ShowGroupCommand showGroup = new ShowGroupCommand();
        
        
        COMMANDS.put(about.getCommandName(), about);
        COMMANDS.put(authors.getCommandName(), authors);
        //Note
        COMMANDS.put(addNote.getCommandName(), addNote);
        COMMANDS.put(removeNote.getCommandName(), removeNote);
        COMMANDS.put(editNote.getCommandName(), editNote);
        COMMANDS.put(showNote.getCommandName(), showNote);
        //Reminder
        //Once
        COMMANDS.put(addOnceReminder.getCommandName(), addOnceReminder);
        COMMANDS.put(editOnceReminder.getCommandName(), editOnceReminder);
        //Persistent
        COMMANDS.put(addPersistentReminder.getCommandName(), addPersistentReminder);
        //Delayed
        COMMANDS.put(addDelayedReminder.getCommandName(), addDelayedReminder);
        COMMANDS.put(editDelayedReminder.getCommandName(), editDelayedReminder);
        //Recurring
        COMMANDS.put(addRecurringReminder.getCommandName(), addRecurringReminder);
        COMMANDS.put(editRecurringReminder.getCommandName(), editRecurringReminder);
        COMMANDS.put(removeReminder.getCommandName(), removeReminder);
        COMMANDS.put(showReminder.getCommandName(), showReminder);
        //Timezone
        COMMANDS.put(setOrEditTimezone.getCommandName(), setOrEditTimezone);
        //Import/Export
        COMMANDS.put(importReminder.getCommandName(), importReminder);
        COMMANDS.put(exportReminder.getCommandName(), exportReminder);
        //Group
        COMMANDS.put(addGroup.getCommandName(), addGroup);
        COMMANDS.put(addToGroup.getCommandName(), addToGroup);
        COMMANDS.put(disableGroup.getCommandName(), disableGroup);
        COMMANDS.put(enableGroup.getCommandName(), enableGroup);
        COMMANDS.put(listGroups.getCommandName(), listGroups);
        COMMANDS.put(removeFromGroup.getCommandName(), removeFromGroup);
        COMMANDS.put(showGroup.getCommandName(), showGroup);

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