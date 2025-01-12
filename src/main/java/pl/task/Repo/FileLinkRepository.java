package pl.task.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.task.Entities.FileLink;

public interface FileLinkRepository extends JpaRepository<FileLink, Long> {
}