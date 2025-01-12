

package pl.task.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.task.Entities.SubTaskFileLink;

public interface SubTaskFileLinkRepository extends JpaRepository<SubTaskFileLink, Long> {
}