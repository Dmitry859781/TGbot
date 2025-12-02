package bot.Note;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteServiceIntegrationTest {

    private NoteService noteService;
    private Connection testConnection;
    private static final String PROD_DB_URL = "jdbc:sqlite:database/Note.db";

    @BeforeEach
    void setUp() throws SQLException {
        // Подключаемся к реальной БД и начинаем транзакцию
        testConnection = DriverManager.getConnection(PROD_DB_URL);
        testConnection.setAutoCommit(false); // отключаем автокоммит
        
        // Создаём обычный NoteService, но БД уже открыта в транзакции
        noteService = new NoteService(); // использует тот же файл

        // Сохраняем текущее состояние
        // Вместо этого мы просто откатим транзакцию
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Откатываем ВСЕ изменения, сделанные в тесте
        if (testConnection != null && !testConnection.isClosed()) {
            testConnection.rollback();
            testConnection.close();
        }
    }

    @Test
    void testAddAndGetNoteInRealDB() throws SQLException {
        // given
        Long userId = 999999L; // уникальный ID, чтобы не мешать реальным данным
        String noteName = "IntegrationTestNote";
        String text = "This note will be rolled back.";

        // when
        noteService.addNoteToDB(userId, noteName, text);
        String retrieved = noteService.getNote(userId, noteName);

        // then
        assertEquals(text, retrieved);

        // Проверим, что оно появилось в списке
        List<String> userNotes = noteService.getUserNotes(userId);
        assertTrue(userNotes.contains(noteName));
    }

    @Test
    void testRemoveNoteInRealDB() throws SQLException {
        // given
        Long userId = 999999L;
        String noteName = "ToDeleteInTest";
        noteService.addNoteToDB(userId, noteName, "Temporary");

        // when
        noteService.removeNoteFromDB(userId, noteName);
        String retrieved = noteService.getNote(userId, noteName);

        // then
        assertNull(retrieved);
    }
}