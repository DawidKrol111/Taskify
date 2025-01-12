package pl.task.Controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pl.task.Entities.Status;
import pl.task.Entities.SubTask;
import pl.task.Entities.Task;
import pl.task.Entities.User;
import pl.task.Repo.StatusRepository;
import pl.task.Repo.SubTaskRepository;
import pl.task.Repo.TaskRepository;
import pl.task.Repo.UserRepository;
import pl.task.Services.SubTaskService;
import pl.task.Services.TaskService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/verifier/")
public class VerifierController {

    private final TaskService taskService;
    private final UserRepository userRepository;
    private final SubTaskRepository subTaskRepository;
    private final StatusRepository statusRepository;
    private final TaskRepository taskRepository;
    private final SubTaskService subTaskService;

    public VerifierController(TaskService taskService, UserRepository userRepository, SubTaskRepository subTaskRepository, StatusRepository statusRepository, TaskRepository taskRepository, SubTaskService subTaskService) {
        this.taskService = taskService;
        this.userRepository = userRepository;
        this.subTaskRepository = subTaskRepository;
        this.statusRepository = statusRepository;
        this.taskRepository = taskRepository;
        this.subTaskService = subTaskService;
    }


    @PostMapping("/subtask/reject")
    public ResponseEntity<?> reject(
            @RequestParam("subtaskId") Long subtaskId,
            @RequestParam("comment") String comment
    ) {
        SubTask subtask;
        if (subTaskService.isSubtaskExisting(subtaskId).isSuccess()) {
            subtask = subTaskRepository.getById(subtaskId);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Nie ma takiego subtaska'"));
        }

        if (subtask.getStatus().getId() == 5L) {
            subtask.setStatus(statusRepository.getById(6L));
            subtask.setComment(comment);
            subtask.getTask().setStatus(statusRepository.getById(6L));
            subTaskRepository.save(subtask);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("message", "Zmieniono status na do poprawy"));
        } else
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Nie można zmienić statusu"));
    }


    @PostMapping("/subtask/accept")
    public ResponseEntity<?> accept(@RequestParam("subtaskId") Long subtaskId) {
        try {
            // Sprawdzenie, czy istnieje subtask o podanym ID
            Optional<SubTask> subTaskOptional = subTaskRepository.findById(subtaskId);
            if (subTaskOptional.isPresent()) {
                SubTask subTask = subTaskOptional.get();

                // Pobranie statusu "Ukończone" z bazy danych
                Status done = statusRepository.findStatusByName("Ukończone");
                if (done == null) {
                    // Obsługa sytuacji, gdy status "Ukończone" nie istnieje w bazie danych
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("message", "Nie znaleziono statusu 'Ukończone'"));
                }

                // Ustawienie statusu subtaska na "Ukończone"
                subTask.setStatus(done);
                subTaskRepository.save(subTask);

                // Sprawdzanie, czy wszystkie subtaski w zadaniu są zakończone
                Optional<Task> taskOptional = taskRepository.findById(subTask.getTask().getId());
                if (taskOptional.isPresent()) {
                    Task task = taskOptional.get();

                    boolean canFinish = true;
                    List<SubTask> subTasks = task.getSubTasks();

                    // Sprawdzenie statusów wszystkich subtasków
                    for (SubTask iteratedSubTask : subTasks) {
                        if (!iteratedSubTask.getStatus().getName().equals("Ukończone")) {
                            canFinish = false;
                            break; // Jeśli jakikolwiek subtask nie jest ukończony, kończymy iterację
                        }
                    }

                    // Jeżeli wszystkie subtaski są ukończone, zakończ zadanie
                    if (canFinish) {
                        Status finished = statusRepository.findStatusByName("Ukończone");
                        task.setStatus(finished);
                        taskRepository.save(task);
                        return ResponseEntity.status(HttpStatus.OK)
                                .body(Map.of("message", "Zakończono taska i subtaska"));
                    }
                }

                // Zwrócenie odpowiedzi OK po zaakceptowaniu subtaska
                return ResponseEntity.status(HttpStatus.OK)
                        .body(Map.of("message", "Zaakceptowano subtaska"));
            }

            // Jeżeli subtask o podanym ID nie istnieje
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Nie ma takiego subtaska"));

        } catch (Exception e) {
            // Obsługa ogólnych wyjątków
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Wystąpił błąd podczas przetwarzania żądania", "error", e.getMessage()));
        }
    }


    @GetMapping("/to-verify")
    public List<Task> getToVerify() {

        List<User> users = userRepository.findAll();
        List<Long> statuses = new ArrayList<>();
        statuses.add(5L);
        Pageable pageable = PageRequest.of(0, 20);

        Page<Task> completedTasks = taskService.getUsersTasksByStatus(users, statuses, pageable);

        return completedTasks.getContent();
    }

}
