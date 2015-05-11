package com.appspot.usbhidterminal.core.services;

import android.content.Intent;
import android.hardware.usb.UsbDevice;

import com.appspot.usbhidterminal.core.Consts;
import com.appspot.usbhidterminal.core.USBUtils;
import com.appspot.usbhidterminal.core.events.LogMessageEvent;
import com.appspot.usbhidterminal.core.events.USBDataReceiveEvent;

public class USBHIDService extends AbstractUSBHIDService {

	private String delimiter;
	private String receiveDataFormat;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onCommand(Intent intent, String action, int flags, int startId) {
		if (Consts.RECEIVE_DATA_FORMAT.equals(action)) {
			receiveDataFormat = intent.getStringExtra(Consts.RECEIVE_DATA_FORMAT);
			delimiter = intent.getStringExtra(Consts.DELIMITER);
		}
		super.onCommand(intent, action, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onDeviceConnected(UsbDevice device) {
		mLog("device connected");
	}

	@Override
	public void onDeviceDisconnected(UsbDevice device) {
		mLog("device disconnected");
	}

	@Override
	public void onDeviceSelected(UsbDevice device) {
		mLog("Selected device VID:" + Integer.toHexString(device.getVendorId()) + " PID:" + Integer.toHexString(device.getProductId()));
	}

	@Override
	public CharSequence onBuildingDevicesList(UsbDevice usbDevice) {
		return "devID:" + usbDevice.getDeviceId() + " VID:" + Integer.toHexString(usbDevice.getVendorId()) + " PID:" + Integer.toHexString(usbDevice.getProductId()) + " " + usbDevice.getDeviceName();
	}

	@Override
	public void onUSBDataSending(String data) {
		mLog("Sending: " + data);
	}

	@Override
	public void onUSBDataSended(int status, byte[] out) {
		mLog("Sended " + status + " bytes");
		for (int i = 0; i < out.length && out[i] != 0; i++) {
			mLog(Consts.SPACE + USBUtils.toInt(out[i]));
		}
	}

	@Override
	public void onSendingError(Exception e) {
		mLog("Please check your bytes, sent as text");
	}

	@Override
	public void onUSBDataReceive(byte[] buffer) {

		StringBuilder stringBuilder = new StringBuilder();
		int i = 0;
		if (receiveDataFormat.equals(Consts.INTEGER)) {
			for (; i < buffer.length && buffer[i] != 0; i++) {
				stringBuilder.append(delimiter).append(String.valueOf(USBUtils.toInt(buffer[i])));
			}
		} else if (receiveDataFormat.equals(Consts.HEXADECIMAL)) {
			for (; i < buffer.length && buffer[i] != 0; i++) {
				stringBuilder.append(delimiter).append(Integer.toHexString(buffer[i]));
			}
		} else if (receiveDataFormat.equals(Consts.TEXT)) {
			for (; i < buffer.length && buffer[i] != 0; i++) {
				stringBuilder.append(String.valueOf((char) buffer[i]));
			}
		} else if (receiveDataFormat.equals(Consts.BINARY)) {
			for (; i < buffer.length && buffer[i] != 0; i++) {
				stringBuilder.append(delimiter).append("0b").append(Integer.toBinaryString(Integer.valueOf(buffer[i])));
			}
		}
		stringBuilder.append("\nReceived ").append(i).append(" bytes");
		eventBus.post(new USBDataReceiveEvent(stringBuilder.toString()));
	}

	private void mLog(String log) {
		eventBus.post(new LogMessageEvent(log));
	}

}