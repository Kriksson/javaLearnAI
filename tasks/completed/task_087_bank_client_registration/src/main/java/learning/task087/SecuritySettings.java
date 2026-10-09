package learning.task087;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

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
    UserDetailsService userDetailsService() {
        return username -> {
            BankUser bankUser = bankUserRepository.findByUsername(username);
            if (bankUser == null) {
                throw new UsernameNotFoundException(username);
            } else {
                return User.withUsername(username)
                        .password(bankUser.getPasswordHash())
                        .roles(bankUser.getRole())
                        .build();
            }
        };
    }

    @Bean
    SecurityFilterChain access(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(
                            CsrfFilter.DEFAULT_CSRF_MATCHER
                        )
                )
                .authorizeHttpRequests(rules -> rules
                        .requestMatchers(HttpMethod.GET,
                                "/bank/info",
                                "/bank/csrf")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/bank/register"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/bank/client-count")
                        .hasRole("AUDITOR")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
