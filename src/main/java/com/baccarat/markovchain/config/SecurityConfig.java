package com.baccarat.markovchain.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Autowired
    private UserDetailsService userDetailsService;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/baccarat/**")
                        .ignoringRequestMatchers("/join")
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/img/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/auth").permitAll() // Allow access to the custom login page
                        .requestMatchers("/logout").permitAll() // Allow access to logout
                        .requestMatchers("/api/baccarat/**").permitAll() // Allow access to baccarat API
                        .requestMatchers("/register").permitAll() // Allow access to registration
                        .requestMatchers("/join").permitAll() // Allow access to join
                        .requestMatchers("/").authenticated() // Home page must be authenticated
                        .requestMatchers(HttpMethod.POST, "/register").permitAll() // Allow POST requests to /register
                        .requestMatchers(HttpMethod.POST, "/api/baccarat/play").permitAll() // Allow POST requests to /register
                        .requestMatchers(HttpMethod.POST, "/join").permitAll() // Allow POST requests to /join
                        .requestMatchers("/login").denyAll() // Deny access to the default login URL
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/auth") // Specify the custom login page
                        .defaultSuccessUrl("/", true) // Redirect to homepage after login
                        .failureUrl("/auth") // Redirect to login page on failure
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth") // Redirect to login page after logout
                        .permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Redirect to custom login page on 403 or 401
                            response.sendRedirect("/auth");
                        }));

        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of("https://player-companion.com"));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
            configuration.setAllowCredentials(true);
            configuration.setAllowedHeaders(List.of("X-XSRF-TOKEN", "Content-Type", "Authorization"));
            return configuration;
        }));


        return http.build();
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(Arrays.asList("https://player-companion.com")); // Your domain
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // Allowed HTTP methods
//        configuration.setAllowCredentials(true); // Allow credentials (if needed)
//        configuration.setAllowedHeaders(Arrays.asList("*")); // Allow all headers
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration); // Apply to all endpoints
//        return source;
//    }


    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(NoOpPasswordEncoder.getInstance());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }


}
