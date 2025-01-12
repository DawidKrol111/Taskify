package pl.task.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import pl.task.Entities.*;
import pl.task.Enum.Action;
import pl.task.Repo.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final UserRepository userRepository;
    private final StatusRepository statusRepository;
    private final UserServiceImp userServiceImp;
    private final RoleRepository roleRepository;
    private final HistoryLogService historyLogService;
    private final FileService fileService;

    @Autowired
    public TaskService(FileService fileService, HistoryLogService historyLogService, TaskRepository taskRepository, SubTaskRepository subTaskRepository, UserRepository userRepository, StatusRepository statusRepository, UserServiceImp userServiceImp, RoleRepository roleRepository) {
        this.taskRepository = taskRepository;
        this.subTaskRepository = subTaskRepository;
        this.userRepository = userRepository;
        this.statusRepository = statusRepository;
        this.userServiceImp = userServiceImp;
        this.roleRepository = roleRepository;
        this.historyLogService = historyLogService;
        this.fileService = fileService;
    }


    public InternalResponse canChangeStatus(User user, Task task, Long statusId) {
        if (user.getRoles().isEmpty()) {
            return InternalResponse.error("Brak roli");
        }
        if (userServiceImp.isUserAdmin(user).isSuccess()) {
            return InternalResponse.success("Admin może wszystko");
        }

        if (task.getStatus().getId() == 1) {
            return InternalResponse.error("Tylko admin może zmienić status zakończonego zadania");
        }

        if (task.getStatus().getId() == 2 && statusId == 5) {
            return InternalResponse.success("User może oddać zadanie do weryfikacji");
        }

        if (task.getStatus().getId() == 3 && statusId == 2) {
            return InternalResponse.success("User może rozpocząć zadanie");
        }

        if (task.getStatus().getId() == 4) {
            return InternalResponse.error("Tylko admin może zmienić status wstrzymanego zadania");
        }

        if (task.getStatus().getId() == 5 && userServiceImp.isUserVerifier(user).isSuccess()) {
            if (statusId == 6 || statusId == 2) {
                return InternalResponse.success("Weryfikator może zmienić status na Ukończony lub Do poprawy");
            } else {
                return InternalResponse.error("Weryfikator nie może zmienić stanu zadania na inny niż ukończony lub do poprawy");
            }
        }

        return InternalResponse.error("Zmiana roli niemożliwa");
    }


    @Transactional
    public InternalResponse taskAssigner(User user, Task task) {
        if (user.getRoles().isEmpty()) {
            return InternalResponse.error("Brak roli");
        }

        if (user.getRoles().contains(roleRepository.getById(2))) {
            task.setDelegated(user.getId());
            taskRepository.save(task);
            return InternalResponse.success("Admin może wszystko");
        }

        if (task.getDelegated().equals(1L) || task.getDelegated().equals(user.getId())) {
            task.setDelegated(user.getId());
            taskRepository.save(task);
            return InternalResponse.success("User może przypisać do siebie zadanie");
        }

        return InternalResponse.error("Błąd podczas zmiany statusu");
    }


    public Page<Task> getUsersTasksByStatus(List<User> users, List<Long> statusIds, Pageable pageable) {
        try {
            List<Long> delegatedList = users.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());


            Page<Task> tasksFound = taskRepository.findByDelegatedInAndStatusIdInOrderByTimeCreatedDesc(
                    delegatedList, statusIds, pageable
            );
            return tasksFound;

        } catch (Exception e) {
            e.printStackTrace();

            return Page.empty();
        }
    }


    public List<Task> searchTask(String string) {
        Map<Long, Task> taskMap = new HashMap<>();
        List<Task> taskList = new ArrayList<>();

        try {
            Long parsedValue = Long.parseLong(string);
            if (taskRepository.findById(parsedValue).isPresent()) {
                taskMap.put(parsedValue, taskRepository.getById(parsedValue));
            }

        } catch (NumberFormatException e) {

            System.out.println("Niepoprawny format liczby");
        }

        List<User> users = userRepository.findByUsernameContainingIgnoreCase(string);
        users.forEach(value -> taskRepository.findByDelegated(value.getId())
                .forEach(task -> taskMap.put(task.getId(), task)));


        taskRepository.findByNameContainingIgnoreCase(string)
                .forEach(task -> taskMap.put(task.getId(), task));


        taskList.addAll(taskMap.values());
        return taskList;
    }


    @Transactional
    public InternalResponse statusChanger(Task task, Long statusId) {
        if (task == null || !canChangeStatus(userServiceImp.getCurrentUser(), task, statusId).isSuccess()) {
            return InternalResponse.error("Błąd podczas zmiany statusu");
        }
        Status status = statusRepository.getById(statusId);
        task.setStatus(status);
        if (status.getName().equals("Do poprawy")) {
            List<SubTask> subTasks = task.getSubTasks();
            subTasks.forEach(subTask -> {
                subTask.setStatus(status);
                subTaskRepository.save(subTask);
            });
        }

        taskRepository.save(task);
        return InternalResponse.success("Zmieniono status na: " + status.getName());
    }



    @Transactional
    public InternalResponse updateTask(Task task, User currentUser, TaskService taskService, SubTaskService subTaskService) {

        InternalResponse responseSuccess = new InternalResponse("Success", true);
        InternalResponse responseError = new InternalResponse("Error", false);

        Optional<Task> taskObj = taskRepository.findById(task.getId());

        Set<Integer> validPriorities = Set.of(1, 2, 3);
        Set<Long> validUsers = userRepository.findAll().stream()
                .filter(user -> user.getEnabled() != 0)
                .map(User::getId)
                .collect(Collectors.toSet());


        if (taskObj.isEmpty()) {
            return responseError;
        }

        Task taskFromDB = taskRepository.getById(task.getId());

        if (task.getName().isEmpty()) {
            return responseError;
        }
        if (!validPriorities.contains(task.getPriority())) {
            return responseError;
        }
        if (!validUsers.contains(task.getDelegated())) {
            return responseError;
        }

        boolean isOwner = taskFromDB.getCreatedBy().equals(currentUser.getId());
        boolean isAdmin = userServiceImp.isUserAdmin(currentUser).isSuccess();

        if (!isOwner || !isAdmin) {
            return responseError;
        }



        if(task.getStatus() != null){
            taskFromDB.setStatus(task.getStatus());
        }
        else {
            task.setStatus(taskFromDB.getStatus());
        }



        if (task.getStatus().getId() == 1 || task.getStatus().getId() == 5) {
            if (task.getEnded() == null) {
                taskFromDB.setEnded(LocalDateTime.now());
            }
        }


        taskFromDB.setName(task.getName());
        taskFromDB.setDelegated(task.getDelegated());
        taskFromDB.setPriority(task.getPriority());




        for (int i = 0; i < task.getSubTasks().size(); i++) {

            SubTask subTask = task.getSubTasks().get(i);
            if (subTaskRepository.findById(subTask.getId()).isPresent() && taskService.isSubtaskPartOfTask(taskFromDB, subTask).isSuccess()) {
                subTaskService.updateSubTask(task.getSubTasks().get(i));
            }

            if (taskService.isSubtaskPartOfTask(taskFromDB, subTask).isSuccess()) {

                if (subTask.getId() > 0) {
                    subTaskService.updateSubTask(subTask);
                }
            } else {
                subTaskService.addSubTask(taskFromDB, subTask.getDescription(), subTask.getName());
            }
        }





        taskRepository.save(taskFromDB);
        return responseSuccess;
    }



    @Transactional
    public boolean addTaskWithFiles(String taskName, String priority, String assignedTo, List<SubTask> subtasks, List<MultipartFile> files) throws IOException {
        priority = Optional.ofNullable(priority).orElse("1");
        assignedTo = Optional.ofNullable(assignedTo).orElse("1");
        subtasks = Optional.ofNullable(subtasks).orElseGet(ArrayList::new);
        files = Optional.ofNullable(files).orElseGet(ArrayList::new);

        Task task = new Task();
        try {
            long assignedId = Long.parseLong(assignedTo);
            if (assignedId == 0) {
                assignedId = 1;
            }

            task.setName(taskName);
            task.setPriority(Integer.parseInt(priority));
            task.setDelegated(assignedId);
            task.setSubTasks(subtasks);
            task.setTimeCreated(LocalDateTime.now());
            task.setStatus(statusRepository.findStatusByName("Nierozpoczęte"));
            task.setCreatedBy(userServiceImp.getCurrentUser().getId());
            taskRepository.save(task);

            for (SubTask subTask : subtasks) {
                subTask.setTask(task);
                subTask.setStatus(statusRepository.findStatusByName("Nierozpoczęte"));
                subTaskRepository.save(subTask);
            }

            if (!files.isEmpty()) {
                fileService.saveTaskFiles(files, task);
            }

            historyLogService.logHistory(task.getId(), Action.TASK_CREATED, "by: " + userServiceImp.getCurrentUser().getId());
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Błąd: assignedTo nie jest prawidłową liczbą: " + assignedTo);
        }
        return false;
    }

    public List<Task> getActiveTasks() {
        Status status = statusRepository.findStatusByName("Ukończone");
        return taskRepository.findByStatusIdNot(status.getId());
    }


    public InternalResponse isTaskExisting(Long id) {
        Optional<Task> task = taskRepository.findById(id);
        if (task.isPresent()) {
            return new InternalResponse("Znaleziono zadanie", true);
        } else {
            return new InternalResponse("Nie znaleziono zadania", false);
        }
    }

    public InternalResponse isSubtaskPartOfTask(Task task, SubTask propSubTask) {

        List<Long> subTaskIds = task.getSubTasks().stream()
                .map(SubTask::getId)  // Zmapuj subtask na jego ID
                .toList();   // Zbierz wyniki do listy

        if (subTaskIds.contains(propSubTask.getId())) {
            return new InternalResponse("Task matches Subtask", true);
        }
        return new InternalResponse("Task doesn't match subtask", false);
    }
}
