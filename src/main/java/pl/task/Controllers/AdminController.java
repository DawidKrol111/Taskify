package pl.task.Controllers;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.task.Entities.*;
import pl.task.Enum.Action;
import pl.task.Repo.*;
import pl.task.Services.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserServiceImp userServiceImp;
    private final HistoryLogService historyLogService;
    private final TaskService taskService;
    private final StatusRepository statusRepository;
    private final SubTaskRepository subTaskRepository;
    private final SubTaskService subTaskService;
    private final StatusService statusService;
    ObjectMapper objectMapper = new ObjectMapper();


    public AdminController(TaskService taskService, HistoryLogService historyLogService, TaskRepository taskRepository, UserRepository userRepository, RoleRepository roleRepository, UserServiceImp userServiceImp, StatusRepository statusRepository, SubTaskRepository subTaskRepository, SubTaskService subTaskService, StatusService statusService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userServiceImp = userServiceImp;
        this.historyLogService = historyLogService;
        this.taskService = taskService;
        this.statusRepository = statusRepository;
        this.subTaskRepository = subTaskRepository;
        this.subTaskService = subTaskService;
        this.statusService = statusService;
    }

    @PostMapping("/delete-subtask")
    public ResponseEntity<?> removeSubtask(
            @RequestParam("id") Long id
    ) {

        if (id < 0) {
            return new ResponseEntity<>(Map.of("message", "ID ujemne, czyli to nowy subtask, jeszcze nie dodany do bazy. Fakuje ze się udało dla frontendu"), HttpStatus.OK);
        }

        if (subTaskService.removeSubTask(id).isSuccess()) {
            return new ResponseEntity<>(Map.of("message", "Skasowano subtaska"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Map.of("message", "Nie udało się skasować subtaska"), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/search")
    public List<Task> search(
            @RequestParam("query") String query
    ) {
        return taskService.searchTask(query);
    }


    @PostMapping("/update-task")
    public ResponseEntity<?> updateTask(
            @RequestParam("id") Long id,
            @RequestParam("assignedTo") Long delegated,
            @RequestParam("taskName") String taskName,
            @RequestParam("priority") int priority,
            @RequestParam("status") String status,
            @RequestParam("subtasks") String subTaskString
    ) {

        List<SubTask> subTasks = subTaskService.subTasksParserFromJson(subTaskString);
        Status statusStatus = statusService.parseStatusFromJson(status);

        Task task = new Task();
        task.setId(id);
        task.setName(taskName);
        task.setDelegated(delegated);
        task.setPriority(priority);
        task.setStatus(statusStatus);
        task.setSubTasks(subTasks);

        //Sukces
        if (taskService.updateTask(task, userServiceImp.getCurrentUser(), taskService, subTaskService ).isSuccess()) {
            return new ResponseEntity<>(Map.of("message", "Udało się"), HttpStatus.OK);
        }
        return new ResponseEntity<>(Map.of("message", "Nie udało się"), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/get-users")
    public List<User> getAllUsers() {

        List<User> users = userRepository.findAll();
        List<User> hiddenUsers = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            User hiddenUser = new User();
            User currUser = users.get(i);

            hiddenUser.setUsername(currUser.getUsername());
            hiddenUser.setId(currUser.getId());
            hiddenUser.setRoles(currUser.getRoles());
            hiddenUser.setEnabled(currUser.getEnabled());
            hiddenUsers.add(hiddenUser);
        }
        return hiddenUsers;
    }

    @GetMapping("/all-tasks")
    public List<Task> lastTasks(@RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 30);

        List<Long> statusesId = statusRepository.findAll().stream()
                .map(Status::getId)
                .collect(Collectors.toList());
        return taskService.getUsersTasksByStatus(userRepository.findAll(), statusesId, pageable).getContent();
    }


    @PostMapping("/add-user")
    public ResponseEntity<?> addUser(@RequestBody Map<String, Object> payload) {
        User user = new User();
        String login = String.valueOf(payload.get("login"));
        String password = String.valueOf(payload.get("password"));
        String email = String.valueOf(payload.get("email"));
        String roleName = String.valueOf(payload.get("role"));
        Role role = roleRepository.findByName(roleName);
        user.setPassword(password);
        user.setUsername(login);
        user.setEmail(email);
        user.setEnabled(1);
        user.setAvatar("default.webp");

        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);

        InternalResponse internalResponse = userServiceImp.validateUser(user);
        String message = internalResponse.getMessage();

        if (internalResponse.isSuccess()) {
            userServiceImp.saveUser(user);
            historyLogService.logHistory(user.getId(), Action.USER_CREATED, "by: " + userServiceImp.getCurrentUser().getId());

            return new ResponseEntity<>(Map.of("message", user.getId()), HttpStatus.OK);
        }
        return new ResponseEntity<>(Map.of("message", message), HttpStatus.BAD_REQUEST);

    }


    @PostMapping("/remove-role")
    public ResponseEntity<?> deactivateUser(@RequestBody Map<String, Object> payload) {
        Integer roleId = Integer.parseInt(payload.get("roleId").toString());
        Long userId = Long.parseLong(payload.get("userId").toString());
        Optional<Role> role = roleRepository.findById(roleId);

        if (role.isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Nie ma takiej roli"), HttpStatus.BAD_REQUEST);
        }
        User user;
        if (userRepository.findById(userId).isPresent()) {
            user = userRepository.findById(userId).get();
        } else {
            return new ResponseEntity<>(Map.of("message", "Użytkownik nie został znaleziony"), HttpStatus.BAD_REQUEST);
        }
        Set<Role> roles = user.getRoles();

        if (roles.remove(role.get())) {
            user.setRoles(roles);
            if (user.getRoles().isEmpty()) {
                user.setEnabled(0);
            }
            userRepository.save(user);
            historyLogService.logHistory(user.getId(), Action.USER_ROLE_UPDATED, "Role removed by: " + userServiceImp.getCurrentUser().getId());

            return new ResponseEntity<>(Map.of("message", "Rola skasowana"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Map.of("message", "Rola nie została znaleziona u użytkownika"), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String newPassword = payload.get("newPassword");
        Long C = Long.parseLong(payload.get("userId"));

        System.out.println("C = " + C);
        System.out.println("C = " + C);
        System.out.println("C = " + C);

        // Walidacja danych wejściowych
        if (newPassword == null) {
            return new ResponseEntity<>(Map.of("message", "Nazwa użytkownika i nowe hasło są wymagane"), HttpStatus.BAD_REQUEST);
        }

        // Resetowanie hasła
        try {
            String message = userServiceImp.resetPassword(newPassword, C).getMessage();
            historyLogService.logHistory(C, Action.USER_PASSWORD_CHANGED, "by: " + userServiceImp.getCurrentUser());

            return new ResponseEntity<>(Map.of("message", message), HttpStatus.OK);
        } catch (UsernameNotFoundException e) {
            return new ResponseEntity<>(Map.of("message", "Nie znaleziono użytkownika"), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(Map.of("message", "Wystąpił błąd podczas resetowania hasła"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/add-role")
    public ResponseEntity<?> addRole(@RequestBody Map<String, Object> payload) {
        Integer roleId = Integer.parseInt(payload.get("roleId").toString());
        Long userId = Long.parseLong(payload.get("userId").toString());
        Optional<Role> role = roleRepository.findById(roleId);

        if (role.isEmpty()) {
            return new ResponseEntity<>(Map.of("message", "Nie ma takiej roli"), HttpStatus.BAD_REQUEST);
        }

        Optional<User> optUser = userRepository.findById(userId);
        User user;

        if (optUser.isPresent()) {
            user = optUser.get();
        } else {
            return new ResponseEntity<>(Map.of("message", "Nie ma takiego użtkownika"), HttpStatus.BAD_REQUEST);
        }

        Set<Role> roles = user.getRoles();

        if (roles.contains(role.get())) {
            return new ResponseEntity<>(Map.of("message", "Użytkownik już posiada tę rolę"), HttpStatus.BAD_REQUEST);
        }

        roles.add(role.get());
        user.setRoles(roles);
        user.setEnabled(1);
        historyLogService.logHistory(user.getId(), Action.USER_ROLE_UPDATED, "Role added by: " + userServiceImp.getCurrentUser().getId());
        userRepository.save(user);

        return new ResponseEntity<>(Map.of("message", "Rola została dodana"), HttpStatus.OK);
    }

}
