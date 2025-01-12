package pl.task.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.task.Entities.Note;
import pl.task.Entities.User;
import pl.task.Repo.NoteRepository;
import pl.task.Services.UserServiceImp;

import java.util.*;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "http://localhost:5173") // Dostosuj URL frontendowy
public class NotesController {

    private final NoteRepository noteRepository;
    private final UserServiceImp userServiceImp;

    public NotesController(NoteRepository noteRepository, UserServiceImp userServiceImp) {
        this.noteRepository = noteRepository;
        this.userServiceImp = userServiceImp;
    }

    @PostMapping
    public ResponseEntity<?> addNote(@RequestBody Map<String, Object> payload) {
        Object obj = payload.get("content");

        String content = obj.toString();

        try {
            if (content.length() > 500) {
                return new ResponseEntity<>(Map.of("message", "Notatka może mieć max 500 znaków"), HttpStatus.UNAUTHORIZED);
            }

            User user = userServiceImp.getCurrentUser();
            if (user == null) {
                return new ResponseEntity<>(Map.of("message", "Uzytkownik nie jest zalogowany"), HttpStatus.UNAUTHORIZED);
            }

            Note newNote = new Note();
            newNote.setUser(user);
            newNote.setContent(content);
            noteRepository.save(newNote);

            List<Note> notes = user.getNotes();
            notes.add(newNote);
            user.setNotes(notes);

            userServiceImp.saveUserNoHash(user);

            return new ResponseEntity<>(Map.of("message", newNote.getId()), HttpStatus.OK);
        } catch (Exception e) {

            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping()
    public ResponseEntity<?> updateNote(@RequestBody Map<String, Object> payload) {
        Object contObj = payload.get("content");
        Object idObj = payload.get("id");

        String content = contObj.toString();
        Long id = Long.parseLong(idObj.toString());
        try {
            User user = userServiceImp.getCurrentUser();
            if (user == null) {
                return new ResponseEntity<>(Map.of("message", "Użytkownik nie jest zalogowany"), HttpStatus.UNAUTHORIZED);
            }

            Optional<Note> note = noteRepository.findById(id);
            Note noteOut = null;
            if(note.isPresent()){
                noteOut = note.get();
                noteOut.setContent(content);
            }
            else {
                return new ResponseEntity<>(Map.of("message", "Nie ma takiej notatki"), HttpStatus.BAD_REQUEST);
            }
            if(Objects.equals(noteOut.getUser().getId(), user.getId())){

                noteRepository.save(noteOut);
                return new ResponseEntity<>(Map.of("message", "Notatka zapisana"), HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(Map.of("message", "Notatka zapisana"), HttpStatus.UNAUTHORIZED);

            }
    }
        catch (Exception e){
            e.printStackTrace();
            return new ResponseEntity<>(Map.of("message", "Błąd wewnątrzny"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


        @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable Long id) {
        try {
            User user = userServiceImp.getCurrentUser();
            if (user == null) {
                return new ResponseEntity<>(Map.of("message", "Użytkownik nie jest zalogowany"), HttpStatus.UNAUTHORIZED);
            }

            // Szukamy notatki w bazie danych
            Optional<Note> noteOptional = noteRepository.findById(id);
            if (noteOptional.isEmpty()) {
                return new ResponseEntity<>(Map.of("message", "Notatka nie istnieje"), HttpStatus.NOT_FOUND);
            }

            Note note = noteOptional.get();

            // Sprawdzamy, czy notatka należy do zalogowanego użytkownika
            if (!note.getUser().getId().equals(user.getId())) {
                return new ResponseEntity<>(Map.of("message", "Brak uprawnień do usunięcia tej notatki"), HttpStatus.FORBIDDEN);
            }

            // Usuwamy notatkę z bazy danych
            noteRepository.delete(note);

            // Aktualizujemy listę notatek użytkownika
            List<Note> notes = user.getNotes();
            notes.remove(note);
            user.setNotes(notes);
            userServiceImp.saveUserNoHash(user);

            return new ResponseEntity<>(Map.of("message", "Notatka została usunięta"), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping
    public List<Note> getNotes() {
        try {
            User user = userServiceImp.getCurrentUser();
            if (user == null) {

                return new ArrayList<>();
            }

            return user.getNotes();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
