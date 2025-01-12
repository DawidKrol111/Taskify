package pl.task.Services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import pl.task.Config.SecurityConfig;
import pl.task.Entities.InternalResponse;
import pl.task.Entities.Role;
import pl.task.Entities.User;
import pl.task.Repo.RoleRepository;
import pl.task.Repo.UserRepository;

import java.util.*;

@Service
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserServiceImp(UserRepository userRepository, RoleRepository roleRepository,
                          BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findByUserName(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public InternalResponse isUserActive(User user) {

        if (user.getRoles().isEmpty()) {
            return new InternalResponse("Brak ról", false);
        } else {
            return new InternalResponse("Użytkownik aktywny", true);
        }
    }
    @Override
    public InternalResponse isUserAdmin(User user) {
        if (user.getRoles().contains(roleRepository.getById(2))) {
            return new InternalResponse("User jest adminem", true);
        } else {
            return new InternalResponse("User nie jest adminem", false);
        }
    }
  @Override
    public InternalResponse isUserVerifier(User user) {
        if (user.getRoles().contains(roleRepository.getById(3))) {
            return new InternalResponse("User jest verifierem", true);
        } else {
            return new InternalResponse("User nie jest verifierem", false);
        }
    }


    @Override
    public List<User> getUsers() {

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



    @Override
    public InternalResponse validateUser(User user) {
        if (user.getUsername().length() < 3) {

            return new InternalResponse("Nazwa użytkownika musi mieć przynajmniej 3 znaki", false);
        }

        if (userRepository.findByUsername(user.getUsername()) != null) {
            return new InternalResponse("Login zajęty", false);
        }

        // Sprawdzenie ról
        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty()) {
            return InternalResponse.error("Brak przypisanych ról");
        }


        for (Role role : roles) {
            // Sprawdzenie, czy nazwa roli jest niepusta
            if (role.getName() == null || role.getName().isEmpty()) {
                return InternalResponse.error("Jedna z ról ma pustą nazwę");
            }

            // Sprawdzenie, czy rola istnieje w bazie danych
            Role foundRole = roleRepository.findByName(role.getName());
            if (foundRole == null) {
                return InternalResponse.error("Rola " + role.getName() + " nie istnieje w bazie danych");
            }
        }

        if (!isValidEmail(user.getEmail())) {
            return new InternalResponse("Niepoprawny e-mail", false);
        }

        if (user.getPassword().length() < 4) {
            return new InternalResponse("Hasło musi mieć przynajmniej 4 znaki", false);
        }

        if (!user.getUsername().matches("^[a-zA-Z0-9._-]+$")) {
            return new InternalResponse("Login może zawierać tylko litery, cyfry oraz znaki: . _ -", false);
        }

        return new InternalResponse("Walidacja poprawna", true);
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email != null && email.matches(emailRegex);
    }

    @Override
    public void saveUserNoHash(User user) {
        userRepository.save(user);
    }

    @Override
    public void saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(1);
        if (user.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER");
            user.setRoles(new HashSet<>(Arrays.asList(userRole)));
        }
        userRepository.save(user);
    }

    @Override
    public User getCurrentUser() {

        if (SecurityConfig.secure) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
                String username = ((UserDetails) authentication.getPrincipal()).getUsername();
                return userRepository.findByUsername(username);
            }
        } else {
            return userRepository.findByUsername("dawid");
        }

        return null;
    }

    @Override
    public void deactivateUser(User user) {

        user.getRoles().clear();
        userRepository.save(user);
    }

    @Override
    public InternalResponse resetPassword(String password, Long userId) {

        User user = userRepository.getById(userId);
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);
        return new InternalResponse("Zmieniono hasło", true);
    }


}
