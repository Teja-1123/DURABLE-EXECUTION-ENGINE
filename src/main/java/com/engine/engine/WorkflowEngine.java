package com.engine.engine;

import com.engine.persistence.SQLiteStore;

@FunctionalInterface
public interface WorkflowDefinition {
    void run(DurableContext ctx) throws Exception;
}

public class WorkflowEngine {
    private final SQLiteStore store;

    public WorkflowEngine(String dbPath) {
        this.store = new SQLiteStore(dbPath);
    }

    /**
     * Starts or RESUMES a workflow by its unique ID.
     * If some steps completed before a crash, they will be skipped automatically.
     */
    public void runWorkflow(String workflowId, WorkflowDefinition workflow) {
        System.out.println("\n========================================");
        System.out.println("Starting/Resuming Workflow: " + workflowId);
        System.out.println("========================================");

        DurableContext ctx = new DurableContext(workflowId, store);
        try {
            workflow.run(ctx);
            System.out.println("\n✅ Workflow COMPLETED successfully!");
        } catch (Exception e) {
            System.err.println("\n❌ Workflow FAILED at step: " + e.getMessage());
        }
    }
}