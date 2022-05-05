package com.iris.StayAwake;

public class Device {
    private String code;
    private String name;


    public Device (String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
