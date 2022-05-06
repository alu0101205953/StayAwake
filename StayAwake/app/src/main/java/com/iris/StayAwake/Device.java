package com.iris.StayAwake;

public class Device {

    private String address;
    private String name;


    public Device (String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }
}
