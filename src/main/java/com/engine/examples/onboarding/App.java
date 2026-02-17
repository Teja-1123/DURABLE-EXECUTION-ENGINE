package com.engine.examples.onboarding;

import com.engine.engine.WorkflowEngine;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws Exception {
        WorkflowEngine engine = new WorkflowEngine("workflow.db");

        // Use a FIXED workflow ID so resume works across runs
        String workflowId = "onboarding-001";

        Scanner scanner = new Scanner(System.in);
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Durable Execution Engine — Demo CLI    ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println("Workflow ID: " + workflowId);
        System.out.println("\nOptions:");
        System.out.println("  1 → Run (or Resume) workflow");
        System.out.println("  2 → Run with crash simulation (exits after Step 1)");
        System.out.println("  3 → Reset workflow (delete DB)");
        System.out.print("\nEnter choice: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> {
                engine.runWorkflow(workflowId, OnboardingWorkflow::run);
            }
            case "2" -> {
                System.out.println("\n⚠️  CRASH MODE: Will exit after Step 1 completes.");
                System.out.println("Run option 1 again afterward to resume from Step 2.");
                engine.runWorkflow(workflowId, ctx -> {
                    // Only run Step 1, then crash
                    String empId = ctx.step("createRecord", String.class, () -> {
                        System.out.println("     → Creating employee record...");
                        Thread.sleep(500);
                        return "EMP-" + System.currentTimeMillis();
                    });
                    System.out.println("     Employee ID: " + empId);
                    System.out.println("\n💥 SIMULATED CRASH — exiting now!");
                    System.exit(1);
                });
            }
            case "3" -> {
                java.io.File db = new java.io.File("workflow.db");
                if (db.delete()) {
                    System.out.println("✅ Database reset. Next run will start fresh.");
                } else {
                    System.out.println("⚠️  No database found (already fresh).");
                }
            }
            default -> System.out.println("Invalid choice.");
        }
    }
}