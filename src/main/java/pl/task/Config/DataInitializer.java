package pl.task.Config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.task.Entities.Status;
import pl.task.Entities.User;
import pl.task.Repo.RoleRepository;
import pl.task.Repo.StatusRepository;
import pl.task.Entities.Role;
import pl.task.Services.UserServiceImp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private UserServiceImp userRepository;
    @Autowired
    private UserServiceImp userServiceImp;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        String appDirectory = System.getProperty("user.dir");

        String directoryPath = appDirectory + "/upload/avatars";

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Nie można utworzyć katalogu: " + directoryPath);
            }
        }

        // Add roles if they don't exist
        if (roleRepository.findByName("ROLE_USER") == null) {
            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            userRole.setDescription("Basic role");
            roleRepository.save(userRole);
        }

        if (roleRepository.findByName("ROLE_ADMIN") == null) {
            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            adminRole.setDescription("Admin role");
            roleRepository.save(adminRole);
        }

        if (roleRepository.findByName("ROLE_VERIFIER") == null) {
            Role verifierRole = new Role();
            verifierRole.setName("ROLE_VERIFIER");
            verifierRole.setDescription("Verifier role");
            roleRepository.save(verifierRole);
        }


        if (userRepository.findByUserName("Nikt") == null) {
            User user = new User();
            user.setUsername("Nikt");
            user.setAvatar("avatars/default.webp");
            user.setPassword("SEKRETNEHASLO"); // {noop} indicates no password encoding
            user.setEnabled(1);
            List<Role> roleList = roleRepository.findAll();
            Set<Role> roleSet = new HashSet<>(roleList);
            user.setRoles(roleSet);
            userServiceImp.saveUser(user);
        }

        if (userRepository.findByUserName("admin") == null) {
            User user = new User();
            user.setUsername("admin");
            user.setAvatar("avatars/default.webp");
            user.setEmail("dawid.krol@k-sport.com.pl");
            user.setPassword("admin"); // {noop} indicates no password encoding
            user.setEnabled(1);
            Role admin = roleRepository.findById(2).get();

            List<Role> roleList = new ArrayList<>();
            roleList.add(admin);
            Set<Role> roleSet = new HashSet<>(roleList);
            user.setRoles(roleSet);
            userServiceImp.saveUser(user);
        }

        List<Status> statuses = getStatusList();

        statuses.forEach(status -> {
            if(statusRepository.findStatusByName(status.getName()) == null){
                statusRepository.save(status);
            }
         });
    }

    private static @NotNull List<Status> getStatusList() {
        List<Status> statuses = new ArrayList<>();

        Status done = new Status();
        done.setName("Ukończone");
        statuses.add(done);

        Status started = new Status();
        started.setName("Rozpoczęte");
        statuses.add(started);

        Status notStarted = new Status();
        notStarted.setName("Nierozpoczęte");
        statuses.add(notStarted);

        Status held = new Status();
        held.setName("Wstrzymane");
        statuses.add(held);


        Status beingVerified = new Status();
        beingVerified.setName("Weryfikowane");
        statuses.add(beingVerified);

        Status redo = new Status();
        redo.setName("Do poprawy");
        statuses.add(redo);


        Status canceled = new Status();
        canceled.setName("Anulowane");
        statuses.add(canceled);
        return statuses;
    }
}