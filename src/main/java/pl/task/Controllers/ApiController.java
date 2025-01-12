package pl.task.Controllers;

import org.springframework.web.bind.annotation.*;


import pl.task.Entities.Role;
import pl.task.Entities.Status;
import pl.task.Repo.RoleRepository;
import pl.task.Repo.StatusRepository;
import pl.task.Repo.TaskRepository;
import pl.task.Repo.UserRepository;
import pl.task.Services.UserServiceImp;

import java.util.List;


@CrossOrigin(origins = "http://localhost:5173") // Dostosuj URL frontendowy
@RestController
@RequestMapping("/api")
public class ApiController {

    private final RoleRepository roleRepository;
    private final StatusRepository statusRepository;

    public ApiController(StatusRepository statusRepository, RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
        this.statusRepository = statusRepository;
    }


    @GetMapping("/roles")
    public List<Role> returnRoles(){
            return  roleRepository.findAll();
    }


    @GetMapping("/statuses")
    public List<Status> returnStatus(){
        return  statusRepository.findAll();
    }
}


