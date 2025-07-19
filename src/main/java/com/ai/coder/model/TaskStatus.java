package com.ai.coder.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskStatus {
    private String taskId;
    private String status; // RUNNING, COMPLETED, ERROR
    private String currentAction;
    private String summary;
    private int currentTurn;
    private int totalEstimatedTurns;
    private long startTime;
    private long lastUpdateTime;
    private List<String> actionHistory;
    private String errorMessage;
    private double progressPercentage;

    public TaskStatus(String taskId) {
        this.taskId = taskId;
        this.status = "RUNNING";
        this.startTime = System.currentTimeMillis();
        this.lastUpdateTime = this.startTime;
        this.actionHistory = new ArrayList<>();
        this.progressPercentage = 0.0;
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void setCurrentAction(String currentAction) {
        this.currentAction = currentAction;
        this.lastUpdateTime = System.currentTimeMillis();
        if (currentAction != null && !currentAction.trim().isEmpty()) {
            this.actionHistory.add(currentAction);
        }
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
        updateProgress();
    }

    public void setTotalEstimatedTurns(int totalEstimatedTurns) {
        this.totalEstimatedTurns = totalEstimatedTurns;
        updateProgress();
    }

    private void updateProgress() {
        if (totalEstimatedTurns > 0) {
            this.progressPercentage = Math.min(100.0, (double) currentTurn / totalEstimatedTurns * 100.0);
        }
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}
