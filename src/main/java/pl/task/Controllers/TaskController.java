package pl.task.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.task.Entities.*;
import pl.task.Enum.Action;
import pl.task.Repo.StatusRepository;
import pl.task.Repo.TaskRepository;
import pl.task.Repo.UserRepository;
import pl.task.Services.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final UserServiceImp userServiceImp;
    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final HistoryLogService historyLogService;
    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final EmailService emailService;
    private final SubTaskService subTaskService;
    private final StatusService statusService;
    ObjectMapper objectMapper = new ObjectMapper();


    @Autowired
    public TaskController(HistoryLogService historyLogService, TaskService taskService, UserServiceImp userServiceImp, TaskRepository taskRepository, UserRepository userRepository, StatusRepository statusRepository, EmailService emailService, SubTaskService subTaskService, StatusService statusService) {
        this.userServiceImp = userServiceImp;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.historyLogService = historyLogService;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.emailService = emailService;
        this.subTaskService = subTaskService;
        this.statusService = statusService;
    }


    @PostMapping("/assign")
    public ResponseEntity<?> assignTask(@RequestParam("taskId") Long taskId) {

        User user = userServiceImp.getCurrentUser();
        InternalResponse response = taskService.isTaskExisting(taskId);

        if (response.isSuccess()) {
            taskService.taskAssigner(user, taskRepository.getById(taskId));
            return new ResponseEntity<>(Map.of("message", response.getMessage()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Map.of("message", response.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/update")
    public ResponseEntity<?> uupdateTask(
            @RequestParam("id") Long id,
            @RequestParam("assignedTo") Long delegated,
            @RequestParam("taskName") String taskName,
            @RequestParam("priority") int priority,
            @RequestParam("subtasks") String subTaskString
    ) {

        List<SubTask> subTasks = subTaskService.subTasksParserFromJson(subTaskString);

        Task task = new Task();
        task.setId(id);
        task.setName(taskName);
        task.setDelegated(delegated);
        task.setPriority(priority);
        task.setSubTasks(subTasks);

        //Sukces
        if (taskService.updateTask(task, userServiceImp.getCurrentUser(), taskService, subTaskService).isSuccess()) {
            return new ResponseEntity<>(Map.of("message", "Udało się"), HttpStatus.OK);
        }
        return new ResponseEntity<>(Map.of("message", "Nie udało się"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/user/pending")
    public ResponseEntity<?> getUsersPendingTasks() {
        try {
            List<User> users = new ArrayList<>();
            users.add(userServiceImp.getCurrentUser());
            List<Long> statuses = new ArrayList<>();
            statuses.add(1L);
            statuses.add(2L);
            Pageable pageable = PageRequest.of(0, 20);

            Page<Task> completedTasks = taskService.getUsersTasksByStatus(users, statuses, pageable);

            return ResponseEntity.ok(completedTasks);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Nie znaleziono użytkownika, lub nie posiada on ukończonych zadań", HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping("/user/done")
    public ResponseEntity<?> getUsersDoneTasks() {
        try {
            List<User> users = new ArrayList<>();
            users.add(userServiceImp.getCurrentUser());
            List<Long> statuses = new ArrayList<>();
            statuses.add(1L);
            statuses.add(5L);
            Pageable pageable = PageRequest.of(0, 20);

            Page<Task> completedPageTasks = taskService.getUsersTasksByStatus(users, statuses, pageable);
            List<Task> completedTasks = completedPageTasks.getContent();

            return ResponseEntity.ok(completedTasks);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Nie znaleziono użytkownika, lub nie posiada on ukończonych zadań", HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping("/start-task")
    public ResponseEntity<?> startTask(@RequestBody Map<String, Object> payload) {

        Object obj = payload.get("taskId");
        String id = obj.toString();
        Long taskId = Long.parseLong(id);
        Optional<Task> task = taskRepository.findById(taskId);
        if (task.isPresent()) {

            if (taskService.statusChanger(task.get()
                    , 2L).isSuccess()) {
                return ResponseEntity.ok(Map.of("message", "Task status updated successfully"));
            } else {
                return new ResponseEntity<>(Map.of("message", "Wystąpił błąd podczas zmiany statusu zadania"), HttpStatus.UNAUTHORIZED);
            }
        } else {
            return new ResponseEntity<>(Map.of("message", "Wystąpił błąd podczas zmiany statusu zadania"), HttpStatus.UNAUTHORIZED);

        }

    }


    @GetMapping("/your-tasks")
    public List<Task> yourTasks() {

        List<User> users = new ArrayList<>();
        users.add(userServiceImp.getCurrentUser());
        List<Long> statuses = new ArrayList<>();
        statuses.add(2L);
        statuses.add(3L);
        statuses.add(6L);
        Pageable pageable = PageRequest.of(0, 20);

        Page<Task> completedPageTasks = taskService.getUsersTasksByStatus(users, statuses, pageable);
        List<Task> completedTasks = completedPageTasks.getContent();

        return completedTasks;
    }


    @GetMapping("/unassigned")
    public List<Task> getUnassignedUnfinishedTasks(@RequestParam(defaultValue = "0") int page) {

        List<User> users = new ArrayList<>();
        User user = userRepository.getById(1L);
        users.add(user);
        List<Long> statuses = new ArrayList<>();
        statuses.add(2L);
        statuses.add(3L);
        Pageable pageable = PageRequest.of(page, 20);

        Page<Task> completedPageTasks = taskService.getUsersTasksByStatus(users, statuses, pageable);
        return completedPageTasks.getContent();
    }

    @PostMapping("/add")
    public ResponseEntity<?> addTask(@RequestParam("taskName") String taskName,
                                     @RequestParam("priority") String priority,
                                     @RequestParam("assignedTo") String assignedTo,
                                     @RequestParam("subtasks") String subtasksString,
                                     @RequestParam(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {

        List<SubTask> subtasks = objectMapper.readValue(subtasksString, new TypeReference<>() {
        });
        if (files == null) {
            files = new ArrayList<>();
        }
        try {
            if (taskService.addTaskWithFiles(taskName, priority, assignedTo, subtasks, files)) {
                long assignedId = Long.parseLong(assignedTo);

                emailService.sendSimpleEmail(userRepository.findById(assignedId).get().getEmail(), "Dostałeś nowe zadanie: " + taskName,
                        "Dostałeś zadanie o nazwie: " + taskName + ", i priorytecie: " + priority);
            }
            ;
            return ResponseEntity.ok(Map.of("message", "Dodano Zadanie"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Wystąpił błąd podczas dodawania zadania"));
        }
    }
}

