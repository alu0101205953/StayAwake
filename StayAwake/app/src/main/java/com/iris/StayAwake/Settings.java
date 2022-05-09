package com.iris.StayAwake;

public class Settings {
    private String variable;
    private String value;
    private String description;

    public Settings (String variable, String value, String description) {
        this.variable = variable;
        this.value = value;
        this.description = description;
    }

    public String getVariable() {
        return variable;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
