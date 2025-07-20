package com.ai.coder.model;

import lombok.Data;

@Data
public   class EditStep {
    // Getters and Setters
    private String action;
    private String targetFile;
    private String description;

}