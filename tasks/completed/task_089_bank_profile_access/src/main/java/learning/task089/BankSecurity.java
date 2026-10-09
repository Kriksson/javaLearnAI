package learning.task089;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class BankSecurity {

    private final BankUserRepository bankUserRepository;
    public BankSecurity(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder passwordEncoder) {
        return username -> {
            BankUser user = bankUserRepository.findByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException(username);
            } else {
                return User.withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRole())
                        .build();
            }
        };
    }

    @Bean
    SecurityFilterChain access(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(rules -> rules
                .requestMatchers(HttpMethod.GET, "/bank/info").permitAll()
                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

}
