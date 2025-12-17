package bot.commands.reminders.group;

import bot.TelegramBot;
import bot.commands.Command;

import org.telegram.telegrambots.meta.api.objects.Message;

public class AddGroupCommand implements Command {

    @Override
    public String getCommandName() {
        return "addGroup";
    }

    @Override
    public String getDescription() {
        return "Создать новую группу";
    }

    @Override
    public String getUsage() {
        return "/addGroup <groupName>";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        if (args.length == 0) {
            bot.sendMessage(chatId, "Укажите имя группы. Пример: /addGroup Работа");
            return;
        }

        String groupName = String.join(" ", args);
        bot.sendMessage(chatId, "Группа \"" + groupName + "\" готова к использованию.");
    }
}