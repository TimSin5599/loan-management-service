package ru.creditbank.loan.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.creditbank.loan.management.exception.ErrorResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, InternalApiKeyProperties.class})
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           InternalApiKeyFilter internalApiKeyFilter,
                           ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.internalApiKeyFilter = internalApiKeyFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, "Не авторизован"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpStatus.FORBIDDEN, "Недостаточно прав для выполнения операции")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/loan-management-service/internal/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String message)
            throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(status.value(), message, List.of(message)));
    }
}
