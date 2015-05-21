package com.appspot.usbhidterminal.core;

public abstract class Consts {
	public static final String BINARY = "binary";
	public static final String INTEGER = "integer";
	public static final String HEXADECIMAL = "hexadecimal";
	public static final String TEXT = "text";

	public static final String ACTION_USB_PERMISSION = "com.google.android.HID.action.USB_PERMISSION";
	public static final String MESSAGE_SELECT_YOUR_USB_HID_DEVICE = "Please select your USB HID device";
	public static final String MESSAGE_CONNECT_YOUR_USB_HID_DEVICE = "Please connect your USB HID device";
	public static final String RECEIVE_DATA_FORMAT = "receiveDataFormat";
	public static final String DELIMITER = "delimiter";
	public static final String DELIMITER_NONE = "none";
	public static final String DELIMITER_NEW_LINE = "newLine";
	public static final String DELIMITER_SPACE = "space";
	public static final String NEW_LINE = "\n";
	public static final String SPACE = " ";

	public static final String ACTION_USB_SHOW_DEVICES_LIST = "ACTION_USB_SHOW_DEVICES_LIST";
	public static final String ACTION_USB_DATA_TYPE = "ACTION_USB_DATA_TYPE";
	public static final int RESULT_SETTINGS = 7;
	public static final String USB_HID_TERMINAL_CLOSE_ACTION = "USB_HID_TERMINAL_EXIT";
	public static final String WEB_SERVER_CLOSE_ACTION = "WEB_SERVER_EXIT";
	public static final String SOCKET_SERVER_CLOSE_ACTION = "SOCKET_SERVER_EXIT";
	public static final int USB_HID_TERMINAL_NOTIFICATION = 45277991;
	public static final int WEB_SERVER_NOTIFICATION = 45277992;
	public static final int SOCKET_SERVER_NOTIFICATION = 45277993;

	private Consts() {
	}
}