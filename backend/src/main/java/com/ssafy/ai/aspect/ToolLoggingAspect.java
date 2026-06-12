package com.ssafy.ai.aspect;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * AI Tool 호출 및 응답을 추적하는 AOP Aspect입니다.
 * com.ssafy.ai.tool 패키지 내의 모든 메서드 호출을 모니터링합니다.
 */
@Slf4j
@Aspect
@Component
public class ToolLoggingAspect {

    // tool 패키지 하위의 모든 public 메서드를 대상으로 설정
    @Around("execution(* com.ssafy.ai.tool..*(..))")
    public Object logToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // 1. Tool 호출 시작 로그 (입력 파라미터 포함)
        log.info(">>>> [TOOL CALL] 메서드: {}, 인자: {}", methodName, Arrays.toString(args));

        long start = System.currentTimeMillis();
        try {
            // 실제 메서드 실행
            Object result = joinPoint.proceed();

            // 2. Tool 실행 완료 로그 (응답 값 및 소요 시간 포함)
            long executionTime = System.currentTimeMillis() - start;
            log.info("<<<< [TOOL RESP] 메서드: {}, 응답: {}, 소요시간: {}ms", methodName, result, executionTime);

            return result;
        } catch (Throwable e) {
            // 3. 에러 발생 시 로그
            log.error("!!!! [TOOL ERROR] 메서드: {}, 사유: {}", methodName, e.getMessage());
            throw e;
        }
    }
}
