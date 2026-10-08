package learning.task085;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecuriySettings {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
                                          @Value("${guild.security.admin-password}") String admPassword,
                                          @Value("${guild.security.reader-password}") String readerPassword) {
        var adminAccount = User.withUsername("admin")
                .password(passwordEncoder().encode(admPassword))
                .roles("ADMIN")
                .build();

        var readerAccount = User.withUsername("reader")
                .password(passwordEncoder().encode(readerPassword))
                .roles("READER")
                .build();

        return new InMemoryUserDetailsManager(adminAccount, readerAccount);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(rules -> rules
                .requestMatchers("/guild/info").permitAll()
                .requestMatchers("/recruits/*").hasAnyRole("ADMIN", "READER")
                .requestMatchers("guild/recruit-count").hasRole("ADMIN")
                .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
