package com.engine.engine;

import com.engine.persistence.SQLiteStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class DurableContext {
    private final String workflowId;
    private final SQLiteStore store;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);

    public DurableContext(String workflowId, SQLiteStore store) {
        this.workflowId = workflowId;
        this.store = store;
    }

    /**
     * The Step Primitive — the heart of the durable engine.
     * Checks the DB first. If already completed, returns cached result.
     * Otherwise, runs the function and saves the result.
     */
    public <T> T step(String id, Class<T> type, Callable<T> fn) throws Exception {
        // Auto-generate a unique sequence key (Bonus: no manual ID collision)
        int seq = sequenceCounter.incrementAndGet();
        String stepKey = id + "_seq_" + seq;

        // MEMOIZATION: Check if this step already ran
        StepResult existing = store.getStep(workflowId, stepKey);
        if (existing != null && "COMPLETED".equals(existing.status)) {
            System.out.println("  [SKIP] Step '" + id + "' already completed. Using cached result.");
            return mapper.readValue(existing.output, type);
        }

        // Execute the step
        System.out.println("  [RUN]  Executing step: " + id);
        T result = fn.call();

        // Persist the result BEFORE returning (atomic commit)
        String json = mapper.writeValueAsString(result);
        store.saveStep(workflowId, stepKey, "COMPLETED", json);
        System.out.println("  [DONE] Step '" + id + "' saved to DB.");

        return result;
    }

    public String getWorkflowId() {
        return workflowId;
    }
}