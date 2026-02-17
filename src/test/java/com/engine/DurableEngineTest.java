package com.engine;

import com.engine.engine.DurableContext;
import com.engine.engine.WorkflowEngine;
import com.engine.persistence.SQLiteStore;
import org.junit.jupiter.api.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DurableEngineTest {

    static final String TEST_DB = "test_workflow.db";
    static final String WORKFLOW_ID = "test-wf-001";

    @AfterAll
    static void cleanup() {
        new File(TEST_DB).delete();
    }

    @Test
    @Order(1)
    void testStepRunsOnFirstExecution() throws Exception {
        SQLiteStore store = new SQLiteStore(TEST_DB);
        DurableContext ctx = new DurableContext(WORKFLOW_ID, store);
        AtomicInteger callCount = new AtomicInteger(0);

        String result = ctx.step("greet", String.class, () -> {
            callCount.incrementAndGet();
            return "Hello, World!";
        });

        assertEquals("Hello, World!", result);
        assertEquals(1, callCount.get(), "Step should run exactly once");
    }

    @Test
    @Order(2)
    void testStepIsMemoizedOnSecondRun() throws Exception {
        SQLiteStore store = new SQLiteStore(TEST_DB);
        DurableContext ctx = new DurableContext(WORKFLOW_ID, store);
        AtomicInteger callCount = new AtomicInteger(0);

        // Re-create DurableContext (simulating a restart) - step should NOT re-execute
        // Note: seq counter resets, so step_key will be greet_seq_1 again
        String result = ctx.step("greet", String.class, () -> {
            callCount.incrementAndGet();
            return "This should NOT run";
        });

        assertEquals("Hello, World!", result, "Should return cached result");
        assertEquals(0, callCount.get(), "Function should NOT have been called again");
    }

    @Test
    @Order(3)
    void testSequenceHandlesLoops() throws Exception {
        SQLiteStore store = new SQLiteStore(TEST_DB);
        DurableContext ctx = new DurableContext("loop-test-001", store);

        // Simulate a loop with the same step ID — sequence ensures uniqueness
        for (int i = 0; i < 3; i++) {
            int index = i;
            String result = ctx.step("loopStep", String.class, () -> "iteration-" + index);
            assertEquals("iteration-" + index, result);
        }
    }

    @Test
    @Order(4)
    void testFullWorkflowEngine() {
        WorkflowEngine engine = new WorkflowEngine("engine_test.db");
        AtomicInteger stepsRun = new AtomicInteger(0);

        engine.runWorkflow("full-test-001", ctx -> {
            ctx.step("step1", String.class, () -> { stepsRun.incrementAndGet(); return "a"; });
            ctx.step("step2", String.class, () -> { stepsRun.incrementAndGet(); return "b"; });
            ctx.step("step3", String.class, () -> { stepsRun.incrementAndGet(); return "c"; });
        });

        assertEquals(3, stepsRun.get());
        new File("engine_test.db").delete();
    }
}