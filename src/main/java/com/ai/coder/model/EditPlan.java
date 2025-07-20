package com.ai.coder.model;


import com.ai.coder.tools.SmartEditTool;
import lombok.Data;

import java.util.List;

@Data
public class EditPlan {
    // Getters and Setters
    private String description;
    private SmartEditTool.EditScope scope;
    private ProjectContext projectContext;
    private List<EditStep> steps;

}