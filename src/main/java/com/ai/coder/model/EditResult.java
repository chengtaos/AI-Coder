package com.ai.coder.model;

import lombok.Data;

import java.util.List;

@Data
public class EditResult {
    private List<String> executedSteps;
    private List<String> errors;
    private String summary;
    private String details;

    public void generateSummary() {
        int successCount = executedSteps.size();
        int errorCount = errors.size();

        this.summary = String.format("Smart edit completed: %d steps executed, %d errors",
                successCount, errorCount);

        StringBuilder detailsBuilder = new StringBuilder();
        detailsBuilder.append("Executed Steps:\n");
        for (String step : executedSteps) {
            detailsBuilder.append("✓ ").append(step).append("\n");
        }

        if (!errors.isEmpty()) {
            detailsBuilder.append("\nErrors:\n");
            for (String error : errors) {
                detailsBuilder.append("✗ ").append(error).append("\n");
            }
        }

        this.details = detailsBuilder.toString();
    }
}