package com.appspot.usbhidterminal.core;

public class USBUtils {

	public static int toInt(byte b) {
		return (int) b & 0xFF;
	}

	public static byte toByte(int c) {
		return (byte) (c <= 0x7f ? c : ((c % 0x80) - 0x80));
	}

	public static String getIpAddress(int ipAddress) {
		return String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
	}

}