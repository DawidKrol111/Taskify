package pl.task.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.task.Entities.Note;


public interface NoteRepository extends JpaRepository<Note, Long> {
}
