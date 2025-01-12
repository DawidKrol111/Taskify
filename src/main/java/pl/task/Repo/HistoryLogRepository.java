package pl.task.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.task.Entities.HistoryLog;

public interface HistoryLogRepository extends JpaRepository<HistoryLog, Long> {


}
