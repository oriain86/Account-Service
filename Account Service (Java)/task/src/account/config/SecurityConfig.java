package account.config;

import account.security.CustomAuthenticationFailureHandler;
import account.security.CustomAuthenticationSuccessHandler;
import account.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                )
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(
                        frameOptions -> frameOptions.disable()))
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to signup and password change
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup",
                                "/api/auth/changepass").permitAll()
                        // Auditor endpoint for events
                        .requestMatchers(HttpMethod.GET, "/api/security/events").hasRole("AUDITOR")
                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                        // Accountant endpoints
                        .requestMatchers("/api/acct/**").hasRole("ACCOUNTANT")
                        // Employee endpoints (only for User and Accountant)
                        .requestMatchers("/api/empl/**").hasAnyRole("USER", "ACCOUNTANT")
                        // Other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Use BCrypt with strength 13
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(13);
    }
}
