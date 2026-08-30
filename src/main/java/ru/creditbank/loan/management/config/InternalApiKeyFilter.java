package ru.creditbank.loan.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.creditbank.loan.management.exception.ErrorResponse;

import java.io.IOException;
import java.util.List;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_PATH_PREFIX = "/loan-management-service/internal";

    private final InternalApiKeyProperties properties;
    private final ObjectMapper objectMapper;

    public InternalApiKeyFilter(InternalApiKeyProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String presentedKey = request.getHeader(API_KEY_HEADER);
        if (presentedKey == null || !presentedKey.equals(properties.key())) {
            unauthorized(response);
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken("internal-service", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        String message = "Недействительный внутренний API-ключ";
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), message, List.of(message));
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
