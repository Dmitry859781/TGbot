package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.reminder.ReminderService;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

public class RemoveReminderCommand implements Command {

    @Override
    public String getCommandName() {
        return "removeReminder";
    }

    @Override
    public String getDescription() {
        return "Удалить напоминание";
    }

    @Override
    public String getUsage() {
        return "/removeReminder";
    }

    private final ReminderService reminderService = new ReminderService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        try {
            List<String> reminderNames = reminderService.getUserReminderNames(chatId);

            if (reminderNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас пока нет напоминаний. Добавьте первое с помощью /addOnceReminder или /addRecurringReminder");
                return;
            }

            StringBuilder response = new StringBuilder("Какое напоминание хотите удалить?\n");
            for (int i = 0; i < reminderNames.size(); i++) {
                response.append(i + 1).append(". ").append(reminderNames.get(i)).append("\n");
            }
            bot.sendMessage(chatId, response.toString().trim());

            bot.setPendingInputHandler(chatId, (input) -> {
                if (input == null || input.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Имя напоминания не может быть пустым. Попробуйте снова: /removeReminder");
                    return;
                }

                String cleanInput = input.trim();

                // Добавить поддержку ввода по номеру (1, 2, 3...), в Note тоже
                String reminderName = cleanInput;

                try {
                    reminderService.removeReminder(chatId, reminderName);
                    bot.sendMessage(chatId, "Напоминание \"" + reminderName + "\" удалено!");
                } catch (Exception e) {
                    e.printStackTrace();
                    bot.sendMessage(chatId, "Не удалось удалить напоминание. Попробуйте позже.");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Не удалось загрузить список напоминаний. Попробуйте позже.");
        }
    }
}
