package com.engine.examples.onboarding;

import com.engine.engine.DurableContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnboardingWorkflow {

    public static void run(DurableContext ctx) throws Exception {
        // ── STEP 1: Create Employee Record (Sequential) ──────────────────────
        String employeeId = ctx.step("createRecord", String.class, () -> {
            System.out.println("     → Creating employee record in HR system...");
            Thread.sleep(500); // Simulate DB work
            return "EMP-" + System.currentTimeMillis();
        });
        System.out.println("     Employee ID: " + employeeId);

        // ── SIMULATE CRASH POINT (uncomment to test crash recovery) ──────────
        // System.out.println("\n💥 SIMULATING CRASH after Step 1!");
        // System.exit(1);

        // ── STEPS 2 & 3: Provision Laptop & Access (PARALLEL) ────────────────
        ExecutorService executor = Executors.newFixedThreadPool(2);

        CompletableFuture<String> laptopFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return ctx.step("provisionLaptop", String.class, () -> {
                    System.out.println("     → Provisioning laptop...");
                    Thread.sleep(1000); // Simulate IT request
                    return "LAPTOP-" + employeeId;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        CompletableFuture<String> accessFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return ctx.step("provisionAccess", String.class, () -> {
                    System.out.println("     → Provisioning system access...");
                    Thread.sleep(800); // Simulate access request
                    return "ACCESS-GRANTED-" + employeeId;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);

        // Wait for both parallel steps to finish
        String laptop = laptopFuture.get();
        String access = accessFuture.get();
        executor.shutdown();

        System.out.println("     Laptop: " + laptop);
        System.out.println("     Access: " + access);

        // ── STEP 4: Send Welcome Email (Sequential) ───────────────────────────
        Boolean emailSent = ctx.step("sendWelcomeEmail", Boolean.class, () -> {
            System.out.println("     → Sending welcome email to new employee...");
            Thread.sleep(300);
            return true;
        });
        System.out.println("     Welcome email sent: " + emailSent);
    }
}