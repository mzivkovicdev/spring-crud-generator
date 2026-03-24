<#setting number_format="computer">
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.persistence.OptimisticLockException;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
    retryFor = {
        OptimisticLockException.class,
        ObjectOptimisticLockingFailureException.class
    }<#if maxAttempts?? || delayMs?? || maxDelayMs?? || multiplier??>,
    <#if maxAttempts??>maxAttempts = ${maxAttempts},</#if>
    <#if delayMs?? || maxDelayMs?? || multiplier??>backoff = @Backoff(
        <#if delayMs??>delay      = ${delayMs}</#if><#if maxDelayMs??>,
        maxDelay   = ${maxDelayMs}</#if><#if multiplier??>,
        multiplier = ${multiplier}</#if>
    )</#if></#if>
)
public @interface OptimisticLockingRetry {

}