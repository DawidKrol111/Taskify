package pl.task.Services;

import pl.task.Entities.InternalResponse;
import pl.task.Entities.User;

import java.util.List;

public interface UserService {

    User findByUserName(String name);

    void saveUser(User user);

    void saveUserNoHash(User user);

    User getCurrentUser();

    InternalResponse validateUser(User user);

    void deactivateUser(User user);

    InternalResponse resetPassword(String password, Long userId);

    InternalResponse isUserActive(User user);
    InternalResponse isUserAdmin(User user);
    InternalResponse isUserVerifier(User user);
    List<User> getUsers();
}
