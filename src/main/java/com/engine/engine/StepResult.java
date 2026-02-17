package com.engine.engine;

public class StepResult {
    public String stepKey;
    public String status;   // "COMPLETED" or "FAILED"
    public String output;   // JSON-serialized result

    public StepResult(String stepKey, String status, String output) {
        this.stepKey = stepKey;
        this.status = status;
        this.output = output;
    }
}