package bot.commands.reminders.group;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.ReminderService;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.List;

public class ListGroupsCommand implements Command {

    @Override
    public String getCommandName() {
        return "listGroups";
    }

    @Override
    public String getDescription() {
        return "Показать список всех групп";
    }

    @Override
    public String getUsage() {
        return "/listGroups";
    }

    private final ReminderService reminderService = new ReminderService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        try {
            List<String> groups = reminderService.getUserGroups(chatId);

            if (groups.isEmpty()) {
                bot.sendMessage(chatId, "У вас нет групп с напоминаниями.");
                return;
            }

            StringBuilder response = new StringBuilder("Список ваших групп:\n");
            for (String group : groups) {
                response.append("- ").append(group.isEmpty() ? "<без группы>" : group).append("\n");
            }

            bot.sendMessage(chatId, response.toString().trim());

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "❌ Ошибка при загрузке групп.");
        }
    }
}