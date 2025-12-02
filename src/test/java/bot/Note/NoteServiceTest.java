package bot.Note;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class NoteServiceTest {

    private NoteService noteService;
    private static final String TEST_DB_URL = "jdbc:sqlite:target/test-note.db";

    @BeforeEach
    void setUp() throws IOException {
        // Удаляем старую БД
        Path dbPath = Path.of("target/test-note.db");
        Files.deleteIfExists(dbPath);
        Files.createDirectories(dbPath.getParent());

        noteService = new NoteService(TEST_DB_URL);
    }

    @Test
    void testAddNote() throws SQLException {
        Long userId = 123L;
        String noteName = "test_note";
        String text = "Это тестовая заметка";
        
        noteService.addNoteToDB(userId, noteName, text);
        
        String retrievedText = noteService.getNote(userId, noteName);
        assertThat(retrievedText).isEqualTo(text);
    }
    
    @Test
    void testGetNote_WhenNoteExists() throws SQLException {
        Long userId = 456L;
        String noteName = "existing_note";
        String expectedText = "Существующая заметка";
        noteService.addNoteToDB(userId, noteName, expectedText);
        
        String actualText = noteService.getNote(userId, noteName);
        assertThat(actualText).isEqualTo(expectedText);
    }
    
    @Test
    void testGetNote_WhenNoteNotExists() throws SQLException {
        Long userId = 999L;
        String noteName = "non_existing_note";
        String result = noteService.getNote(userId, noteName);
        assertThat(result).isNull();
    }
    
    @Test
    void testRemoveNote() throws SQLException {
        Long userId = 789L;
        String noteName = "note_to_remove";
        String text = "Заметка для удаления";
        noteService.addNoteToDB(userId, noteName, text);
        
        assertThat(noteService.getNote(userId, noteName)).isEqualTo(text);
        
        noteService.removeNoteFromDB(userId, noteName);
        
        String result = noteService.getNote(userId, noteName);
        assertThat(result).isNull();
    }
    
    @Test
    void testGetUserNotes() throws SQLException {
        Long userId = 111L;
        noteService.addNoteToDB(userId, "note1", "Текст 1");
        noteService.addNoteToDB(userId, "note2", "Текст 2");
        noteService.addNoteToDB(userId, "note3", "Текст 3");
        noteService.addNoteToDB(222L, "other_user_note", "Чужая заметка");
        
        List<String> userNotes = noteService.getUserNotes(userId);
        assertThat(userNotes)
            .hasSize(3)
            .containsExactlyInAnyOrder("note1", "note2", "note3");
    }
    
    @Test
    void testGetUserNotes_WhenNoNotes() throws SQLException {
        Long userId = 333L;
        List<String> userNotes = noteService.getUserNotes(userId);
        assertThat(userNotes).isEmpty();
    }
}