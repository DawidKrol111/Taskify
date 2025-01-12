package pl.task.Controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.task.Entities.*;
import pl.task.Repo.RoleRepository;
import pl.task.Repo.SubTaskRepository;
import pl.task.Services.SubTaskService;
import pl.task.Services.UserService;
import pl.task.Repo.NoteRepository;
import pl.task.Services.UserServiceImp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subtask")
@CrossOrigin(origins = "http://localhost:5173") // Dostosuj URL frontendowy
public class SubTaskController {

    private final SubTaskService subTaskService;
    private final SubTaskRepository subTaskRepository;
    private final UserServiceImp userServiceImp;
    private final RoleRepository roleRepository;

    public SubTaskController( SubTaskService subTaskService, SubTaskRepository subTaskRepository, UserServiceImp userServiceImp, RoleRepository roleRepository) {
        this.subTaskService = subTaskService;
        this.subTaskRepository = subTaskRepository;
        this.userServiceImp = userServiceImp;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/delete")
    public ResponseEntity<?> removeSubtask(
            @RequestParam("id") Long id
    ) {

        if (id < 0) {
            return new ResponseEntity<>(Map.of("message", "ID ujemne, czyli to nowy subtask, jeszcze nie dodany do bazy. Fakuje ze się udało dla frontendu"), HttpStatus.OK);
        }

        Role role = roleRepository.findByName("ROLE_USER");
        if (userServiceImp.getCurrentUser().getRoles().contains(role)) {
            if (subTaskService.removeSubTask(id).isSuccess()) {
                return new ResponseEntity<>(Map.of("message", "Posiadasz role Usera, skasowano subtaska"), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Map.of("message", "Nie udało się skasować subtaska"), HttpStatus.BAD_REQUEST);
            }

        }
        return new ResponseEntity<>(Map.of("message", "Brak wymaganej roli."), HttpStatus.BAD_REQUEST);

    }


    @PostMapping("/add-file")
    public ResponseEntity<?> addFileToSubtask(@RequestParam("subtaskId") Long subtaskId,
                                              @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {
        if (files == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Pliki są złe."));
        }

        Optional<SubTask> subTask = subTaskRepository.findById(subtaskId);

        if (subTask.isPresent()) {
            if (subTaskService.saveSubtaskFiles(files, subTask.get())) {

                List<String> pathes = new ArrayList<>();
                List<SubTaskFileLink> links = subTask.get().getFileLinks();
                int size = links.size();

                for (int i = 0; i < size; i++) {
                    pathes.add(links.get(i).getFilePath());
                }
                return new ResponseEntity<>(Map.of("message", links), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Map.of("message", "Błąd dodawania"), HttpStatus.BAD_REQUEST);

            }

        }
        return new ResponseEntity<>(Map.of("message", "Błąd podczas dodawania plików."), HttpStatus.OK);
    }


    @PostMapping("/start")
    public ResponseEntity<?> startSubTask(@RequestBody Map<String, Object> payload
    ) {

        Object obj = payload.get("subtaskId");

        String id = obj.toString();
        Long subTaskId = Long.parseLong(id);

        if (subTaskService.startSubTask(subTaskId)) {

            return ResponseEntity.ok(Map.of("message", "Rozpoczęto subtaska"));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Wystąpił błąd podczas rozpoczynania subTaska."));
    }


    @PostMapping("/end")
    public ResponseEntity<?> endSubTask(@RequestBody Map<String, Object> payload) {

        Object obj = payload.get("subtaskId");
        Long subTaskId = Long.parseLong(obj.toString());

        InternalResponse response = subTaskService.endSubTask(subTaskId);


        if (response.getMessage().equals("1")) {

            return ResponseEntity.ok(Map.of("message", "Zakończono subtaska i zadanie", "taskEnded", true));
        }

        if (response.getMessage().equals("2")) {
            return ResponseEntity.ok(Map.of("message", "Zakończona subtaska", "taskEnded", false));
        }

        return new ResponseEntity<>(Map.of("message", "Nie udalo sie zakonczyc SubTaska"), HttpStatus.UNAUTHORIZED);
    }
}
