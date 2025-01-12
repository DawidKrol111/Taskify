package pl.task.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;


@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {



    public static boolean secure = true;

    @Override
    protected void configure(HttpSecurity http) throws Exception {


        if (secure) {
            http

                    .authorizeRequests()
                    .antMatchers("/login").permitAll()
                    .and()
                    .authorizeRequests()
                    .antMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .and()

                    .authorizeRequests()
                    .antMatchers("/api/verifier/**")
                    .hasRole("VERIFIER") // Zmieniamy z ADMIN na VERIFIER
                    .and()

                    .formLogin()
                    .defaultSuccessUrl("/", true)
                    .permitAll()
                    .and()
                    .logout()
                    .logoutUrl("/logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessUrl("/")
                    .permitAll()
                    .and().authorizeRequests().anyRequest().hasAnyRole("USER", "ADMIN", "VERIFIER")
                    .and()
                    .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
        } else {
            http
                    .authorizeRequests()
                    .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                    .antMatchers("/login").permitAll()
                    .and()
                    .formLogin()
                    .defaultSuccessUrl("/", true)
                    .permitAll()
                    .and()
                    .logout()
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .permitAll()
                    .and().authorizeRequests().anyRequest().permitAll()
                    .and()
                    .csrf().ignoringAntMatchers("/**");

        }
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}
