package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.InputHandler;
import bot.commands.notes.ShowNoteCommand;
import bot.note.NoteService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.List;

import static org.mockito.Mockito.*;

public class ShowNoteCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    @Mock
    private NoteService mockNoteService;

    private ShowNoteCommand command;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        command = new ShowNoteCommand();

        // Подмена noteService через рефлексию
        try {
            var field = ShowNoteCommand.class.getDeclaredField("noteService");
            field.setAccessible(true);
            field.set(command, mockNoteService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(mockMessage.getChatId()).thenReturn(123L);
    }

    @Test
    void shouldShowNoteList_thenRequestName_thenDisplayNoteText() throws SQLException {
        Long chatId = 123L;
        List<String> notes = List.of("Список дел", "Идея");
        String noteText = "Купить хлеб и молоко";

        when(mockNoteService.getUserNotes(chatId)).thenReturn(notes);
        when(mockNoteService.getNote(chatId, "Список дел")).thenReturn(noteText);

        // Запуск команды
        command.execute(mockBot, mockMessage, new String[]{});

        // Проверка: список отправлен
        verify(mockBot).sendMessage(chatId, "Какую заметку хотите посмотреть?\n1. Список дел\n2. Идея");

        // Захват обработчика ввода имени
        ArgumentCaptor<InputHandler> handlerCaptor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), handlerCaptor.capture());

        // Имитация ввода имени
        handlerCaptor.getValue().handle("Список дел");

        // Проверка: отправлено название и текст
        verify(mockBot).sendMessage(chatId, "Заметка \"Список дел\".");
        verify(mockBot).sendMessage(chatId, noteText);
    }

    @Test
    void shouldNotifyNoNotes_whenUserHasNoNotes() throws SQLException {
        Long chatId = 123L;
        when(mockNoteService.getUserNotes(chatId)).thenReturn(List.of());

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "У вас пока нет заметок. Добавьте первую с помощью /addNote");
        verify(mockBot, never()).setPendingInputHandler(anyLong(), any(InputHandler.class));
    }

    @Test
    void shouldCancelAndNotify_whenNoteNameIsEmpty() throws SQLException {
        Long chatId = 123L;
        when(mockNoteService.getUserNotes(chatId)).thenReturn(List.of("Test"));

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Какую заметку хотите посмотреть?\n1. Test");

        ArgumentCaptor<InputHandler> captor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), captor.capture());

        captor.getValue().handle("   "); // пустой ввод

        verify(mockBot).sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
        verify(mockNoteService, never()).getNote(anyLong(), anyString());
    }

    @Test
    void shouldNotifyError_whenFetchingNoteFails() throws SQLException {
        Long chatId = 123L;
        when(mockNoteService.getUserNotes(chatId)).thenReturn(List.of("Ошибка"));
        doThrow(SQLException.class).when(mockNoteService).getNote(chatId, "Ошибка");

        command.execute(mockBot, mockMessage, new String[]{});

        verify(mockBot).sendMessage(chatId, "Какую заметку хотите посмотреть?\n1. Ошибка");

        ArgumentCaptor<InputHandler> captor = ArgumentCaptor.forClass(InputHandler.class);
        verify(mockBot).setPendingInputHandler(eq(chatId), captor.capture());

        captor.getValue().handle("Ошибка");

        verify(mockBot).sendMessage(chatId, "Не удалось получить заметку. Попробуйте позже.");
    }
}