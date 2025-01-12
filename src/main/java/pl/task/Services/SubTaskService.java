package pl.task.Services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.task.Entities.*;
import pl.task.Enum.Action;
import pl.task.Repo.StatusRepository;
import pl.task.Repo.SubTaskRepository;
import pl.task.Repo.TaskRepository;
import pl.task.Repo.UserRepository;

import javax.persistence.EntityNotFoundException;
import javax.swing.text.html.Option;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SubTaskService {

    private final SubTaskRepository subTaskRepository;
    private final UserService userService;
    private final StatusRepository statusRepository;
    private final TaskService taskService;
    private final HistoryLogService historyLogService;
    private final UserServiceImp userServiceImp;
    private final FileService fileService;
    private final TaskRepository taskRepository;

    @Autowired
    public SubTaskService(FileService fileService, HistoryLogService historyLogService, UserService userService, SubTaskRepository subTaskRepository, UserRepository userRepository, StatusRepository statusRepository, TaskService taskService, UserServiceImp userServiceImp, TaskRepository taskRepository) {
        this.subTaskRepository = subTaskRepository;
        this.userService = userService;
        this.statusRepository = statusRepository;
        this.taskService = taskService;
        this.historyLogService = historyLogService;
        this.userServiceImp = userServiceImp;
        this.fileService = fileService;
        this.taskRepository = taskRepository;
    }


    private boolean canChangeStatus(SubTask subTask, Long newStatusId) {
        Task task = subTask.getTask();

        if (!task.getDelegated().equals(userService.getCurrentUser().getId())) {
            return false;
        }

        Status currentStatus = subTask.getStatus();
        if (currentStatus.getId().equals(newStatusId)) {
            return false;
        }

        return true;
    }

    public boolean saveSubtaskFiles(List<MultipartFile> files, SubTask subtask) throws IOException {

        User user = userServiceImp.getCurrentUser();
        Task task = subtask.getTask();
        if (Objects.equals(user.getId(), task.getDelegated())) {
            return fileService.saveSubtaskFiles(files, subtask);

        } else {
            return false;
        }

    }



    public boolean startSubTask(Long id) {
        Optional<SubTask> subTaskOpt = subTaskRepository.findById(id);
        if (!subTaskOpt.isPresent()) {
            return false;
        }
        SubTask subTask = subTaskOpt.get();
        Task task = subTask.getTask();

        Status taskStatus = task.getStatus();
        if (!taskStatus.getId().equals(2L)) {
            return false;
        }

        if (!canChangeStatus(subTask, 2L)) {
            return false;
        }

        Status status = statusRepository.getById(2L);
        subTask.setStatus(status);
        subTask.setStarted(LocalDateTime.now());
        subTaskRepository.save(subTask);
        historyLogService.logHistory(subTask.getId(), Action.SUBTASK_STARTED, "by: " + userServiceImp.getCurrentUser().getId());

        return true;
    }

    public InternalResponse endSubTask(Long id) {

        Optional<SubTask> subTaskOpt = subTaskRepository.findById(id);
        if (!subTaskOpt.isPresent()) {

            return InternalResponse.error("Nie ma takiego podzadania");
        }
        SubTask subTask = subTaskOpt.get();

        Status currentStatus = subTask.getStatus();
        if (!currentStatus.getId().equals(2L)) {
            return InternalResponse.error("Podzadanie ma status inny niż 'Rozpoczęte'");
        }

        // Sprawdzenie, czy można zmienić status podzadania
        if (!canChangeStatus(subTask, 1L)) {
            return InternalResponse.error("Nie można zmienić statusu podzadania'");
        }

        // Ustawienie nowego statusu podzadania na "Weryfikowane" (ID = 1)
        Status toVerifyStatus = statusRepository.getById(5L);
        subTask.setStatus(toVerifyStatus);
        subTask.setEnded(LocalDateTime.now());

        // Sprawdzenie, czy wszystkie podzadania są "Ukończone"
        Task task = subTask.getTask();
        List<SubTask> subTasks = task.getSubTasks();
        boolean allSubTasksCompleted = true;

        for (SubTask s : subTasks) {
            if (!s.getStatus().getId().equals(1L) && !s.getStatus().getId().equals(5L)) {
                allSubTasksCompleted = false;
                break;
            }
        }

        if (allSubTasksCompleted) {
            subTaskRepository.save(subTask);
            taskService.statusChanger(task, 5L);
            historyLogService.logHistory(task.getId(), Action.TASK_COMPLETED, "by: " + userServiceImp.getCurrentUser());
            historyLogService.logHistory(subTask.getId(), Action.SUBTASK_COMPLETED, "by: " + userServiceImp.getCurrentUser());

            return new InternalResponse("1", true);
        }

        subTaskRepository.save(subTask);
        historyLogService.logHistory(subTask.getId(), Action.SUBTASK_COMPLETED, "by: " + userServiceImp.getCurrentUser());
        return new InternalResponse("2", true);
    }

    public InternalResponse updateSubTask(SubTask subTask) {

        long subtaskId = subTask.getId();
        SubTask subTaskInDB = subTaskRepository.findById(subtaskId)
                .orElseThrow(() -> new EntityNotFoundException("Subtask not found"));

        subTaskInDB.setName(subTask.getName());
        subTaskInDB.setDescription(subTask.getDescription());
        subTaskRepository.save(subTaskInDB);
        System.out.println("\"udalo sie\" = " + "udalo sie");
        System.out.println("\"udalo sie\" = " + "udalo sie");
        return new InternalResponse("Updated subtask", true);
    }


    public InternalResponse removeSubTask(Long id) {
        if (subTaskRepository.findById(id).isPresent()) {
            SubTask subtask = subTaskRepository.getById(id);
            Task task = subtask.getTask();
            List<SubTask> subtasks = task.getSubTasks();
            if (subtasks.size() <= 1) {
                return new InternalResponse("Nie mozna skasować ostatniego subtaska", false);
            }

            subtasks.remove(subtask);
            subTaskRepository.delete(subtask); // Use delete() with the entity instance
            return new InternalResponse("Skasowano subtaska o id: " + id, true);
        }
        return new InternalResponse("Nie ma takiego subtaska: " + id, true);
    }


    public InternalResponse addSubTask(Task task, String description, String name) {
        if (task == null) {
            return new InternalResponse("Zadanie nie może być null", false);
        }

        if (description == null || description.trim().isEmpty()) {
            return new InternalResponse("Opis nie może być pusty", false);
        }

        if (name == null || name.trim().isEmpty()) {
            return new InternalResponse("Nazwa nie może być pusta", false);
        }

        SubTask subtask = new SubTask();
        subtask.setTask(task);
        subtask.setDescription(description);
        subtask.setStatus(statusRepository.getById(3L)); // Zakładamy, że status 3L zawsze istnieje
        subtask.setName(name);

        try {
            subTaskRepository.save(subtask);
        } catch (Exception e) {
            return new InternalResponse("Błąd podczas zapisywania subtaska: " + e.getMessage(), false);
        }

        return new InternalResponse("Stworzono subtaska", true);
    }


    public InternalResponse isSubtaskExisting(Long id) {
        Optional<SubTask> optSubtask = subTaskRepository.findById(id);
        if (optSubtask.isPresent()) {
            return new InternalResponse("Znaleziono subtaska", true);
        } else
            return new InternalResponse("Nie znaleziono subtaska", false);
    }


    public List<SubTask> subTasksParserFromJson(String subTaskString){
        List<SubTask> subTasks = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(subTaskString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SubTask subTask = new SubTask();
                subTask.setId(jsonObject.getLong("id")); // Przykładowe pole
                subTask.setName(jsonObject.getString("name")); // Przykładowe pole
                subTask.setDescription(jsonObject.getString("description")); // Przykładowe pole
                subTasks.add(subTask);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subTasks;
    }
}

