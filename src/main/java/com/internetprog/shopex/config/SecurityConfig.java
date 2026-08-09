package com.internetprog.shopex.config;

import com.internetprog.shopex.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Authentication itself is auto-configured by Spring Security from the
 * CustomUserDetailsService bean plus the PasswordEncoder bean below — no
 * explicit AuthenticationProvider is needed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
        http
            // Runs once authentication is established but before access decisions,
            // so a session belonging to a just-disabled account is ended immediately.
            .addFilterBefore(new DisabledUserFilter(userRepository), AuthorizationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/products", "/products/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/products/*/reviews").authenticated()
                .requestMatchers("/cart", "/cart/**").permitAll()
                .requestMatchers("/checkout", "/checkout/**").authenticated()
                .requestMatchers("/orders/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/profile", "/profile/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
