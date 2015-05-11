package com.appspot.usbhidterminal.core.events;

public class USBDataReceiveEvent {
    private final String data;

    public USBDataReceiveEvent(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

}