package com.task.reifensbank.logging;

import com.task.reifensbank.util.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@RequiredArgsConstructor
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {


    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        LogService.logEndpoint(request.getMethod(), request.getRequestURI());
        return true;
    }
}
