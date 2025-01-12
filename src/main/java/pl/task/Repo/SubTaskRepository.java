package pl.task.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.task.Entities.SubTask;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {



}