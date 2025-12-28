package com.example.terminologyservice.dto;

import jakarta.validation.constraints.NotBlank;

public class ResolveRequest {

    @NotBlank
    private String text;

    @NotBlank
    private String system;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }
}
