package bot.commands;

import org.telegram.telegrambots.meta.api.objects.Message;
import bot.TelegramBot;

public class AuthorsCommand implements Command {
    @Override
    public String getCommandName() {
        return "authors";
    }

    @Override
    public String getDescription() {
        return "Показать авторов бота";
    }

    @Override
    public String getUsage() {
        return "/authors";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
    	bot.sendMessage(message.getChatId(), "Автор: Дмитрий Екимов");
    }
}