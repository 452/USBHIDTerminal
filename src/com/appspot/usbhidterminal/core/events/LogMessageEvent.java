package com.appspot.usbhidterminal.core.events;

public class LogMessageEvent {

    private final String data;

    public LogMessageEvent(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}