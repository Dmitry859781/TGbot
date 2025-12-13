package bot.commands.timezone;

import bot.TelegramBot;
import bot.commands.Command;
import bot.timezone.UserTimezoneService;

import org.telegram.telegrambots.meta.api.objects.Message;

public class SetOrEditTimezoneCommand implements Command {

    @Override
    public String getCommandName() {
        return "setOrEditTimezone";
    }

    @Override
    public String getDescription() {
        return "Установить или изменить ваш часовой пояс";
    }

    @Override
    public String getUsage() {
        return "/setOrEditTimezone";
    }

    private final UserTimezoneService timezoneService = new UserTimezoneService();

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();

        try {
            Integer currentTimezone = timezoneService.getTimezone(chatId);
            StringBuilder prompt = new StringBuilder();

            if (currentTimezone != null) {
                String sign = currentTimezone >= 0 ? "+" : "";
                prompt.append("Ваш текущий часовой пояс: UTC").append(sign).append(currentTimezone).append("\n\n");
            }

            prompt.append(
                "Пожалуйста, укажите ваш часовой пояс как смещение от UTC.\n\n" +
                "Примеры:\n" +
                "• Москва (зимой) → +3\n" +
                "• Екатеринбург → +5\n" +
                "• Владивосток → +10\n" +
                "• Лондон → +0\n" +
                "• Нью-Йорк → -5\n\n" +
                "Просто отправьте число со знаком: +3, -4, +0 и т.д."
            );

            bot.sendMessage(chatId, prompt.toString());

            bot.setPendingInputHandler(chatId, (input) -> {
                if (input == null || input.trim().isEmpty()) {
                    bot.sendMessage(chatId, "Смещение не может быть пустым. Попробуйте снова: /setTimezone");
                    return;
                }

                String raw = input.trim();
                try {
                    int offset = parseOffsetAsInt(raw);
                    if (offset < -12 || offset > 14) {
                        throw new IllegalArgumentException("Допустимый диапазон: от -12 до +14");
                    }

                    timezoneService.saveTimezoneOffset(chatId, offset);
                    String sign = offset >= 0 ? "+" : "";
                    bot.sendMessage(chatId, "Часовой пояс установлен: UTC" + sign + offset);
                } catch (Exception e) {
                    bot.sendMessage(chatId,
                        "Неверный формат.\n" +
                        "Пожалуйста, введите смещение от UTC в формате:\n" +
                        "+3, -5, +0 и т.д.\n" +
                        "Попробуйте снова: /setTimezone"
                    );
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при загрузке часового пояса. Попробуйте позже.");
        }
    }
    private int parseOffsetAsInt(String input) {
        String clean = input.trim().replaceAll("[^0-9+-]", "");
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Пустой ввод");
        }
        // Обрабатываем +0, -0
        if (clean.equals("+0") || clean.equals("-0") || clean.equals("0")) {
            return 0;
        }
        return Integer.parseInt(clean);
    }
}