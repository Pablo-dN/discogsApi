package com.challenge.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Aspect
@Component
public class ServiceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLoggingAspect.class);

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Before("execution(* com.challenge..*(..))")
    public void logMethodEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);


        Object[] args = joinPoint.getArgs();
        String argsString = args != null && args.length > 0
                ? Arrays.toString(args)
                : "No arguments";

        logger.info("[{}] Entering method: {} with arguments: {}", timestamp, methodName, argsString);
    }
}