package pl.task.Config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.task.Entities.CurrentUser;
import pl.task.Entities.User;
import pl.task.Services.UserService;

import java.util.HashSet;
import java.util.Set;

@Service
public class SpringDataUserDetailsService implements UserDetailsService {

    private final UserService userService;

    public SpringDataUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userService.findByUserName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        // Logowanie nazw ról
        user.getRoles().forEach(r -> {
            System.out.println("Dodano rolę: " + r.getName());
            grantedAuthorities.add(new SimpleGrantedAuthority(r.getName())); // Dodaj rolę do zestawu
        });

        return new CurrentUser(user.getUsername(), user.getPassword(), grantedAuthorities, user);
    }
}
