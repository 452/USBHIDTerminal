package com.appspot.usbhidterminal.core.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.support.v4.app.NotificationCompat;

import com.appspot.usbhidterminal.R;
import com.appspot.usbhidterminal.USBHIDTerminal;
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
		setupNotifications();
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
		eventBus.post(new USBDataReceiveEvent(stringBuilder.toString(), i));
	}

	private void mLog(String log) {
		eventBus.post(new LogMessageEvent(log));
	}

	private void setupNotifications() { //called in onCreate()
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, USBHIDTerminal.class)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
				0);
		PendingIntent pendingCloseIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, USBHIDTerminal.class)
						.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
						.setAction(Consts.USB_HID_TERMINAL_CLOSE_ACTION),
				0);
		mNotificationBuilder
				.setSmallIcon(R.drawable.ic_launcher)
				.setCategory(NotificationCompat.CATEGORY_SERVICE)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setContentTitle(getText(R.string.app_name))
				.setWhen(System.currentTimeMillis())
				.setContentIntent(pendingIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel,
						getString(R.string.action_exit), pendingCloseIntent)
				.setOngoing(true);
		mNotificationBuilder
				.setTicker(getText(R.string.app_name))
				.setContentText(getText(R.string.app_name));
		if (mNotificationManager != null) {
			mNotificationManager.notify(Consts.USB_HID_TERMINAL_NOTIFICATION, mNotificationBuilder.build());
		}
	}

}