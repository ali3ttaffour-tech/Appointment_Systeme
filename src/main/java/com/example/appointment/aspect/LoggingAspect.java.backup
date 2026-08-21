package com.example.appointment.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // قبل تنفيذ أي method داخل controller
    @Before("execution(* com.example.appointment.controller.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {

        log.info(
                "Method: {} | Arguments: {}",
                joinPoint.getSignature().getName(),
                joinPoint.getArgs()
        );

    }

    // بعد نجاح التنفيذ
    @AfterReturning(
            pointcut = "execution(* com.example.appointment.service.*.*(..))",
            returning = "result"
    )
    public void logAfterReturning(JoinPoint joinPoint, Object result) {

        log.info("Method Completed: {} | Result: {}",
                joinPoint.getSignature().getName(),
                result);

    }

    // عند حدوث خطأ
    @AfterThrowing(
            pointcut = "execution(* com.example.appointment.service.*.*(..))",
            throwing = "ex"
    )
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {

        log.error("Exception in method: {} | Message: {}",
                joinPoint.getSignature().getName(),
                ex.getMessage());

    }
}