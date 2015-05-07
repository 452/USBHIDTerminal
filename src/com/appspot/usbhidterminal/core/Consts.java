package com.appspot.usbhidterminal.core;

public abstract class Consts {
	public static final String CONNECTION = "CONNECTION";
	public static final String END_POINT_READ = "END_POINT_READ";
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

	public static final int ACTION_USB_DEVICE_ATTACHED = 1;
	public static final int ACTION_USB_DEVICE_DETACHED = 2;
	public static final String ACTION_USB_SHOW_DEVICES_LIST = "ACTION_USB_SHOW_DEVICES_LIST";
	public static final String ACTION_USB_SEND_DATA = "ACTION_USB_SEND_DATA";
	public static final String ACTION_USB_DATA_TYPE = "ACTION_USB_DATA_TYPE";
	public static final String ACTION_USB_SELECT_DEVICE = "ACTION_USB_SELECT_DEVICE";
	public static final int ACTION_USB_LOG = 4;
	public static final int ACTION_USB_LOG_C = 5;
	public static final int ACTION_USB_SHOW_DEVICES_LIST_RESULT = 6;

	private Consts() {
	}
}