package pl.task.Controllers;


import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pl.task.Config.SecurityConfig;
import pl.task.Entities.CurrentUser;
import pl.task.Entities.InternalResponse;
import pl.task.Entities.Role;
import pl.task.Entities.User;
import pl.task.Repo.RoleRepository;
import pl.task.Repo.TaskRepository;
import pl.task.Repo.UserRepository;
import pl.task.Services.UserServiceImp;

import java.util.*;

@CrossOrigin(origins = "http://localhost:5173") // Dostosuj URL frontendowy
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserServiceImp userServiceImp;

    public UserController(TaskRepository taskRepository, UserRepository userRepository, RoleRepository roleRepository, UserServiceImp userServiceImp) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userServiceImp = userServiceImp;
    }


    @GetMapping("/get-users")
    public List<User> getUsers() {

        List<User> users = userRepository.findAll();
        List<User> usersEncrypted = new ArrayList<>();


        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getEnabled() == 1) {
                User user = users.get(i);
                User userEnc = new User();
                userEnc.setId(user.getId());
                userEnc.setUsername(user.getUsername());
                userEnc.setPassword("SECRET");
                usersEncrypted.add(userEnc);
            }
        }
        return usersEncrypted;
    }


    @GetMapping("/info")
    public User userInfo() {
        User user = userServiceImp.getCurrentUser();
        user.setPassword("XD");
        return user;
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}