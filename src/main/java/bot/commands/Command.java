package bot.commands;

import java.sql.SQLException;

import org.telegram.telegrambots.meta.api.objects.Message;

import bot.TelegramBot;

public interface Command {
 String getCommandName();       // например: "help"
 String getDescription();       // краткое описание
 String getUsage();             // как использовать (например: "/help <command>")
 void execute(TelegramBot bot, Message message, String[] args);
}