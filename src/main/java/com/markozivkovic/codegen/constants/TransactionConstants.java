package com.markozivkovic.codegen.constants;

public class TransactionConstants {
    
    private TransactionConstants() {
        
    }
    
    public static final String SPRING_FRAMEWORK_TRANSACTION_ANNOTATION_TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";
    public static final String TRANSACTIONAL_ANNOTATION = "@Transactional";
    public static final String OPTIMISTIC_LOCKING_RETRY = "OptimisticLockingRetry";
    public static final String OPTIMISTIC_LOCKING_RETRY_ANNOTATION = "@OptimisticLockingRetry";
}
