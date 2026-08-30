package ru.creditbank.loan.management.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String SPAN_ID_MDC_KEY = "spanId";
    public static final String SERVICE_MDC_KEY = "service";

    private String serviceName;

    public TraceIdFilter() {
    }

    public TraceIdFilter(String serviceName) {
        this.serviceName = serviceName;
    }

    @Value("${spring.application.name}")
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String incomingTraceId = request.getHeader(TRACE_ID_HEADER);
        String traceId = StringUtils.hasText(incomingTraceId) ? incomingTraceId : UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
            MDC.put(SPAN_ID_MDC_KEY, spanId);
            MDC.put(SERVICE_MDC_KEY, serviceName);
            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
