package com.markozivkovic.codegen.constants;

public class GeneratorConstants {
    
    private GeneratorConstants() {}
    
    public static final class Transaction {
        private Transaction() {}
        public static final String OPTIMISTIC_LOCKING_RETRY = "OptimisticLockingRetry";
        public static final String OPTIMISTIC_LOCKING_RETRY_ANNOTATION = "@OptimisticLockingRetry";
    }

}
