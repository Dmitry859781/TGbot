package bot.commands.notes;

import java.sql.SQLException;
import java.util.List;

import org.telegram.telegrambots.meta.api.objects.Message;

import bot.TelegramBot;
import bot.commands.Command;
import bot.note.NoteService;

public class EditNoteCommand implements Command{

	@Override
	public String getCommandName() {
		return "editNote";
	}

	@Override
	public String getDescription() {
		return "Редактировать заметку";
	}

	@Override
	public String getUsage() {
		return "/editNote";
	}

	private final NoteService noteService = new NoteService();
	
	@Override
	public void execute(TelegramBot bot, Message message, String[] args) {
        Long chatId = message.getChatId();
        
		try {
			List<String> noteNames = noteService.getUserNotes(chatId);
			
			//Шаг 1: Выводим список заметок
			if (noteNames.isEmpty()) {
	            bot.sendMessage(chatId, "У вас пока нет заметок. Добавьте первую с помощью /addNote");
	            return;
			}
			
			// StringBuilder - изменяемый String
        	// Заменить на StringBuffer при добавлении многопоточности
        	StringBuilder response = new StringBuilder("Какую заметку хотите подредактировать?\n");
            for (int i = 0; i < noteNames.size(); i++) {
                response.append(i + 1).append(". ").append(noteNames.get(i)).append("\n");
            }
            bot.sendMessage(chatId, response.toString().trim());
	        
	        // Шаг 1: Запрашиваем имя заметки
	        bot.sendMessage(chatId, "Введите имя редактируемой заметки.");
	
	        // Регистрируем обработчик для получения имени заметки
	        bot.setPendingInputHandler(chatId, (noteName) -> {
	            if (noteName == null || noteName.trim().isEmpty()) {
	                bot.sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
	                return;
	            }
	
	            String cleanName = noteName.trim();
	            
	            // Шаг 2: Запрашиваем отредактируемый текст заметки, передавая имя через замыкание
	            bot.sendMessage(chatId, "Введите отредактируемый текст для заметки \"" + cleanName + "\".");
	
	            // Регистрируем обработчик для получения текста
	            bot.setPendingInputHandler(chatId, (noteText) -> {
	                if (noteText == null || noteText.trim().isEmpty()) {
	                    bot.sendMessage(chatId, "Текст заметки не может быть пустым. Операция отменена.");
	                    return;
	                }
	
	                try {
	                	noteService.removeNoteFromDB(chatId, cleanName);
	                    noteService.addNoteToDB(chatId, cleanName, noteText.trim());
	                    bot.sendMessage(chatId, "Заметка \"" + cleanName + "\" успешно обновлена!");
	                } catch (Exception e) {
	                    //e.printStackTrace(); // Не обрабатываем ошибку добавления
	                    bot.sendMessage(chatId, "Ошибка добавления заметки. Попробуйте позже.");
	                }
	            });
	        });
			} catch (Exception e) {
		        bot.sendMessage(chatId, "Не удалось вывести список заметок. Попробуйте позже.");
		}
    }

}
