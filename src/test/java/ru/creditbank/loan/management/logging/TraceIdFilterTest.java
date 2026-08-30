package ru.creditbank.loan.management.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter("loan-management-service");

    @Test
    void doFilter_noTraceIdHeader_generatesTraceIdAndPopulatesMdc() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(null);

        doAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNotNull();
            assertThat(MDC.get(TraceIdFilter.SPAN_ID_MDC_KEY)).isNotNull();
            assertThat(MDC.get(TraceIdFilter.SERVICE_MDC_KEY)).isEqualTo("loan-management-service");
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response).setHeader(eq(TraceIdFilter.TRACE_ID_HEADER), anyString());
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void doFilter_traceIdHeaderPresent_echoesSameValueBack() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        String incomingTraceId = "some-value";
        when(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(incomingTraceId);

        doAnswer(invocation -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isEqualTo(incomingTraceId);
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilter(request, response, filterChain);

        verify(response).setHeader(TraceIdFilter.TRACE_ID_HEADER, incomingTraceId);
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void doFilter_afterCompletion_mdcIsCleared() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader(TraceIdFilter.TRACE_ID_HEADER)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(TraceIdFilter.SPAN_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(TraceIdFilter.SERVICE_MDC_KEY)).isNull();
    }
}
