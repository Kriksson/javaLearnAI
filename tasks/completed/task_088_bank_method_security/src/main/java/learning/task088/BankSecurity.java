package learning.task088;

import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class BankSecurity {

    private final BankUserRepository bankUserRepository;
    public  BankSecurity(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder passwordEncoder) {
        return username ->{
            BankUser bankUser = bankUserRepository.findByUsername(username);
            return User.withUsername(username)
                    .password(bankUser.getPasswordHash())
                    .roles(bankUser.getRole())
                    .build();
        };
    }

    @Bean
    SecurityFilterChain access(HttpSecurity http) throws Exception {
        return http.
                authorizeHttpRequests(rules ->
                        rules
                                .requestMatchers("/bank/info").permitAll()
                                .requestMatchers("/bank/me").authenticated()
                                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
