package com.appspot.usbhidterminal.core.events;

public class USBDataReceiveEvent {
    private final String data;
    private final int bytesCount;

    public USBDataReceiveEvent(String data, int bytesCount) {
        this.data = data;
        this.bytesCount = bytesCount;
    }

    public String getData() {
        return data;
    }

    public int getBytesCount() {
        return bytesCount;
    }

}