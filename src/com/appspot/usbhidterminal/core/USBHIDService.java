package com.appspot.usbhidterminal.core;

import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.widget.Toast;

public class USBHIDService extends AstractUSBHIDService {

	private String delimiter;
	private String receiveDataFormat;

	@Override
	public void onCreate() {
		Toast.makeText(this, "Service onCreate ...", Toast.LENGTH_SHORT).show();
		super.onCreate();
	}

	@Override
	public void onCommand(Intent intent, String action, int flags, int startId) {
		super.onCommand(intent, action, flags, startId);
		Toast.makeText(this, "Service onCommand ...", Toast.LENGTH_SHORT).show();
		if (Consts.RECEIVE_DATA_FORMAT.equals(action)) {
			receiveDataFormat = intent.getStringExtra(Consts.RECEIVE_DATA_FORMAT);
			delimiter = intent.getStringExtra(Consts.DELIMITER);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "Service destroyed ...", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onDeviceConnected() {
		mLog("device connected");
	}

	@Override
	public void onDeviceDisconnected() {
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
		mLog("sended " + status + " bytes");
		for (int i = 0; i < out.length; i++) {
			if (out[i] != 0) {
				mLogC(Consts.SPACE + USBUtils.toInt(out[i]));
			} else {
				break;
			}
		}
	}

	@Override
	public void onSendingError(Exception e) {
		mLog("Please check your bytes, sent as text");
	}

	@Override
	public void onUSBDataReceive(byte[] buffer) {

		StringBuilder stringBuilder = new StringBuilder();
		if (receiveDataFormat.equals(Consts.INTEGER)) {
			for (int i = 0; i < getPacketSize(); i++) {
				if (buffer[i] != 0) {
					stringBuilder.append(delimiter).append(String.valueOf(USBUtils.toInt(buffer[i])));
				} else {
					break;
				}
			}
		} else if (receiveDataFormat.equals(Consts.HEXADECIMAL)) {
			for (int i = 0; i < getPacketSize(); i++) {
				if (buffer[i] != 0) {
					stringBuilder.append(delimiter).append(Integer.toHexString(buffer[i]));
				} else {
					break;
				}
			}
		} else if (receiveDataFormat.equals(Consts.TEXT)) {
			for (int i = 0; i < getPacketSize(); i++) {
				if (buffer[i] != 0) {
					stringBuilder.append(String.valueOf((char) buffer[i]));
				} else {
					break;
				}
			}
		} else if (receiveDataFormat.equals(Consts.BINARY)) {
			for (int i = 0; i < getPacketSize(); i++) {
				if (buffer[i] != 0) {
					stringBuilder.append(delimiter).append("0b").append(Integer.toBinaryString(Integer.valueOf(buffer[i])));
				} else {
					break;
				}
			}
		}
		stringBuilder.append("\nreceived ").append(buffer.length).append(" bytes");
		mLog(stringBuilder.toString());
	}

	private void mLog(String value) {
		Bundle bundle = new Bundle();
		bundle.putString("log", value);
		sendResultToUI(Consts.ACTION_USB_LOG, bundle);
	}

	private void mLogC(String value) {
		Bundle bundle = new Bundle();
		bundle.putString("log", value);
		sendResultToUI(Consts.ACTION_USB_LOG_C, bundle);
	}

}