package pl.task.Entities;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
public class CurrentUser extends User {
    private final pl.task.Entities.User user;
    public CurrentUser(String username, String password,
                       Collection<? extends GrantedAuthority> authorities,
                       pl.task.Entities.User user) {
        super(username, password, authorities);
        this.user = user;
    }
    public pl.task.Entities.User getUser() {return user;}
}