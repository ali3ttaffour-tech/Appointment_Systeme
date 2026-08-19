package com.example.appointment.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private final AiLogForwarder aiLogForwarder;

    // قبل تنفيذ أي method داخل controller
    @Before("execution(* com.example.appointment.controller.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {

        String args = Arrays.stream(joinPoint.getArgs())
                .map(LogSanitizer::sanitize)
                .collect(Collectors.joining(", "));

        String message = String.format(
                "Method: %s | Arguments: %s",
                joinPoint.getSignature().getName(),
                args
        );

        log.info(message);
        aiLogForwarder.submit(message);
    }

    // بعد نجاح التنفيذ
    @AfterReturning(
            pointcut = "execution(* com.example.appointment.service.*.*(..))",
            returning = "result"
    )
    public void logAfterReturning(JoinPoint joinPoint, Object result) {

        String sanitizedResult = LogSanitizer.sanitize(result);
        String message = String.format(
                "Method Completed: %s | Result: %s",
                joinPoint.getSignature().getName(),
                sanitizedResult
        );

        log.info(message);
        aiLogForwarder.submit(message);
    }

    // عند حدوث خطأ
    @AfterThrowing(
            pointcut = "execution(* com.example.appointment.service.*.*(..))",
            throwing = "ex"
    )
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {

        String message = String.format(
                "Exception in method: %s | Message: %s",
                joinPoint.getSignature().getName(),
                LogSanitizer.sanitize(ex.getMessage())
        );

        log.error(message);
        aiLogForwarder.submit(message);
    }
}
