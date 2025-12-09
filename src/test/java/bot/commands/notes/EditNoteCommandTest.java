package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.InputHandler;
import bot.commands.notes.EditNoteCommand;
import bot.note.NoteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class EditNoteCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    @Mock
    private NoteService mockNoteService;

    private EditNoteCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new EditNoteCommand();

        // Подмена noteService через рефлексию
        try {
            var field = EditNoteCommand.class.getDeclaredField("noteService");
            field.setAccessible(true);
            field.set(command, mockNoteService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(mockMessage.getChatId()).thenReturn(123L);
    }

    @Test
    void shouldListNotes_thenRequestName_thenRequestText_thenUpdateNote_whenUserProvidesValidInputs() {
        Long chatId = 123L;
        List<String> notes = Arrays.asList("Заметка 1", "Заметка 2");
        try {
			when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);
		} catch (SQLException e) {
			e.printStackTrace();
		}

        // Запускаем команду
        command.execute(mockBot, mockMessage, new String[]{});

        // Проверка: список отправлен
        verify(mockBot).sendMessage(chatId, "Какую заметку хотите подредактировать?\n1. Заметка 1\n2. Заметка 2");
        verify(mockBot).sendMessage(chatId, "Введите имя редактируемой заметки.");

        // Получаем и вызываем первый обработчик (имя)
        ArgumentCaptor<InputHandler> nameCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), nameCaptor.capture());
        InputHandler nameHandler = nameCaptor.getValue();
        nameHandler.handle("Заметка 1");

        // Проверка: запрос текста
        verify(mockBot).sendMessage(chatId, "Введите отредактируемый текст для заметки \"Заметка 1\".");

        // Захватываем ВСЕ вызовы setPendingInputHandler (их 2)
        ArgumentCaptor<InputHandler> allHandlers = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot, times(2)).setPendingInputHandler(eq(chatId), allHandlers.capture());

        // Второй обработчик — текст
        InputHandler textHandler = allHandlers.getAllValues().get(1);
        textHandler.handle("Новый текст");

        // Проверка: заметка обновлена
        try {
			verify(mockNoteService).removeNoteFromDB(chatId, "Заметка 1");
		} catch (SQLException e) {
			e.printStackTrace();
		}
        try {
			verify(mockNoteService).addNoteToDB(chatId, "Заметка 1", "Новый текст");
		} catch (SQLException e) {
			e.printStackTrace();
		}
        verify(mockBot).sendMessage(chatId, "Заметка \"Заметка 1\" успешно обновлена!");
    }

    @Test
    void shouldNotifyNoNotes_whenUserHasNoNotes() {
        Long chatId = 123L;
        try {
			when(mockNoteService.getUserNotes(chatId)).thenReturn(List.of());
		} catch (SQLException e) {
			e.printStackTrace();
		}

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "У вас пока нет заметок. Добавьте первую с помощью /addNote");
        verify(mockBot, never()).setPendingInputHandler(anyLong(), any(InputHandler.class));
    }

    @Test
    void shouldNotifyError_whenFetchingNotesFails() {
        Long chatId = 123L;
        try {
			when(mockNoteService.getUserNotes(chatId)).thenThrow(new RuntimeException("DB error"));
		} catch (SQLException e) {
			e.printStackTrace();
		}

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Не удалось вывести список заметок. Попробуйте позже.");
    }

    @Test
    void shouldCancelAndNotify_whenNoteNameIsEmpty() {
        Long chatId = 123L;
        List<String> notes = List.of("Test");
        try {
			when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);
		} catch (SQLException e) {
			e.printStackTrace();
		}

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Какую заметку хотите подредактировать?\n1. Test");
        verify(mockBot).sendMessage(chatId, "Введите имя редактируемой заметки.");

        ArgumentCaptor<InputHandler> nameCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), nameCaptor.capture());
        nameCaptor.getValue().handle("   "); // пустое имя

        verify(mockBot).sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
        try {
			verify(mockNoteService, never()).removeNoteFromDB(anyLong(), anyString());
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }

    @Test
    void shouldCancelAndNotify_whenNoteTextIsEmpty() {
        Long chatId = 123L;
        List<String> notes = List.of("Test");
        try {
			when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);
		} catch (SQLException e) {
			e.printStackTrace();
		}

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Какую заметку хотите подредактировать?\n1. Test");
        verify(mockBot).sendMessage(chatId, "Введите имя редактируемой заметки.");

        ArgumentCaptor<InputHandler> nameCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), nameCaptor.capture());
        InputHandler nameHandler = nameCaptor.getValue();
        nameHandler.handle("Test");

        verify(mockBot).sendMessage(chatId, "Введите отредактируемый текст для заметки \"Test\".");

        // Захватываем все вызовы
        ArgumentCaptor<InputHandler> allHandlers = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot, times(2)).setPendingInputHandler(eq(chatId), allHandlers.capture());

        InputHandler textHandler = allHandlers.getAllValues().get(1);
        textHandler.handle("   "); // пустой текст

        verify(mockBot).sendMessage(chatId, "Текст заметки не может быть пустым. Операция отменена.");
        try {
			verify(mockNoteService, never()).removeNoteFromDB(anyLong(), anyString());
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }

    @Test
    void shouldNotifyError_whenUpdateFails() {
        Long chatId = 123L;
        List<String> notes = List.of("Note");
        try {
			when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);
		} catch (SQLException e) {
			e.printStackTrace();
		}
        try {
			doThrow(new RuntimeException("Save failed"))
			    .when(mockNoteService)
			    .addNoteToDB(chatId, "Note", "New text");
		} catch (SQLException e) {
			e.printStackTrace();
		}

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Какую заметку хотите подредактировать?\n1. Note");
        verify(mockBot).sendMessage(chatId, "Введите имя редактируемой заметки.");

        ArgumentCaptor<InputHandler> nameCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), nameCaptor.capture());
        InputHandler nameHandler = nameCaptor.getValue();
        nameHandler.handle("Note");

        verify(mockBot).sendMessage(chatId, "Введите отредактируемый текст для заметки \"Note\".");

        ArgumentCaptor<InputHandler> allHandlers = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot, times(2)).setPendingInputHandler(eq(chatId), allHandlers.capture());

        InputHandler textHandler = allHandlers.getAllValues().get(1);
        textHandler.handle("New text");

        // Удаление всё равно произошло
        try {
			verify(mockNoteService).removeNoteFromDB(chatId, "Note");
		} catch (SQLException e) {
			e.printStackTrace();
		}
        // Но добавление — нет (из-за ошибки)
        verify(mockBot).sendMessage(chatId, "Ошибка добавления заметки. Попробуйте позже.");
    }
}