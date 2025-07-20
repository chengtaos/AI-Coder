package com.ai.coder.model;

/**
 * 工具统计信息内部类
 */
public class ToolStats {
    private long totalCalls = 0;
    private long successCount = 0;
    private long errorCount = 0;
    private long totalExecutionTime = 0;

    public void incrementCalls() {
        totalCalls++;
    }

    public void incrementSuccess() {
        successCount++;
    }

    public void incrementError() {
        errorCount++;
    }

    public void addExecutionTime(long time) {
        totalExecutionTime += time;
    }

    public long getTotalCalls() {
        return totalCalls;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public long getAverageExecutionTime() {
        return totalCalls > 0 ? totalExecutionTime / totalCalls : 0;
    }
}