package learning.task086;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecuritySettings {

    private final BankUserRepository bankUserRepository;

    public SecuritySettings(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return username -> {
            BankUser bankUser = bankUserRepository.findByUsername(username);
            if (bankUser == null) {
                throw new UsernameNotFoundException("User not found");
            } else {
                return User.withUsername(bankUser.getUsername())
                        .password(bankUser.getPasswordHash())
                        .roles(bankUser.getRole())
                        .build();
            }
        };
    }

    @Bean
    SecurityFilterChain access(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(rules -> rules
                .requestMatchers("/bank/info").permitAll()
                .requestMatchers("/bank/me").authenticated()
                .requestMatchers("/bank/client-count").hasRole("AUDITOR")
                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
