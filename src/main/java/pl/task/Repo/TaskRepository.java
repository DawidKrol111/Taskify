package pl.task.Repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.task.Entities.Note;
import pl.task.Entities.Task;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatusIdNot(Long statusId);

    List<Task> findByDelegatedAndStatusId(Long delegated, Long statusId);

    List<Task> findByDelegated(Long delegated);

    List<Task> findTop20ByDelegatedAndStatusIdOrderByTimeCreatedDesc(Long delegated, Long statusId);

    List<Task> findTop20ByOrderByTimeCreatedDesc();

    Page<Task> findByDelegatedInAndStatusIdInOrderByTimeCreatedDesc(List<Long> delegatedIds, List<Long> statusIds, Pageable pageable);

    List<Task> findByNameContainingIgnoreCase(String name);

    @Query("SELECT t FROM Task t WHERE t.delegated = :userId OR LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Task> findByDelegatedOrNameContaining(@Param("userId") Long userId, @Param("name") String name);

}
