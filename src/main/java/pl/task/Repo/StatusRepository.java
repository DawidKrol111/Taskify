package pl.task.Repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.task.Entities.Role;
import pl.task.Entities.Status;
import pl.task.Entities.User;


@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    Status findStatusByName(String name);
}