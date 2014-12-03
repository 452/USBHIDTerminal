package com.appspot.usbhidterminal.core;

import java.util.LinkedList;
import java.util.List;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

public abstract class AbstractUSBHIDService extends Service {

	private USBThreadDataReceiver usbThreadDataReceiver;
	private ResultReceiver resultReceiver;

	private final Handler uiHandler = new Handler();

	private UsbManager mUsbManager;
	private UsbInterface intf;
	private UsbEndpoint endPointRead;
	private UsbEndpoint endPointWrite;
	private UsbDeviceConnection connection;
	private UsbDevice device;

	private IntentFilter filter;
	private PendingIntent mPermissionIntent;

	private int packetSize;
	private boolean sendedDataType;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(Consts.ACTION_USB_PERMISSION), 0);
		filter = new IntentFilter(Consts.ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		filter.addAction(Consts.ACTION_USB_SHOW_DEVICES_LIST);
		filter.addAction(Consts.ACTION_USB_SELECT_DEVICE);
		filter.addAction(Consts.ACTION_USB_SEND_DATA);
		filter.addAction(Consts.ACTION_USB_DATA_TYPE);
		registerReceiver(mUsbReceiver, filter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (resultReceiver == null) {
			resultReceiver = intent.getParcelableExtra("receiver");
		}
		if (Consts.ACTION_USB_DATA_TYPE.equals(action)) {
			sendedDataType = intent.getBooleanExtra(Consts.ACTION_USB_DATA_TYPE, false);
		} else if (Consts.ACTION_USB_SEND_DATA.equals(action)) {
			sendData(intent.getStringExtra(Consts.ACTION_USB_SEND_DATA), sendedDataType);
		} else if (Consts.ACTION_USB_SHOW_DEVICES_LIST.equals(action)) {
			mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
			List<CharSequence> list = new LinkedList<CharSequence>();
			for (UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
				list.add(onBuildingDevicesList(usbDevice));
			}
			final CharSequence devicesName[] = new CharSequence[mUsbManager.getDeviceList().size()];
			list.toArray(devicesName);
			Bundle bundle = new Bundle();
			bundle.putCharSequenceArray(Consts.ACTION_USB_SHOW_DEVICES_LIST, devicesName);
			sendResultToUI(Consts.ACTION_USB_SHOW_DEVICES_LIST_RESULT, bundle);
		} else if (Consts.ACTION_USB_SELECT_DEVICE.equals(action)) {
			device = (UsbDevice) mUsbManager.getDeviceList().values().toArray()[intent.getIntExtra(Consts.ACTION_USB_SELECT_DEVICE, 0)];
			mUsbManager.requestPermission(device, mPermissionIntent);
		}
		onCommand(intent, action, flags, startId);
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (usbThreadDataReceiver != null) {
			usbThreadDataReceiver.stopThis();
		}
		unregisterReceiver(mUsbReceiver);
	}

	private class USBThreadDataReceiver extends Thread {

		private volatile boolean isStopped;

		public USBThreadDataReceiver() {
		}

		@Override
		public void run() {
			if (connection != null && endPointRead != null) {
				final byte[] buffer = new byte[packetSize];
				while (!isStopped) {
					final int status = connection.bulkTransfer(endPointRead, buffer, packetSize, 300);
					if (status >= 0) {
						uiHandler.post(new Runnable() {
							@Override
							public void run() {
								onUSBDataReceive(buffer);
							}
						});
					}
				}
			}
		}

		public void stopThis() {
			isStopped = true;
		}
	}

	private void sendData(String data, boolean sendAsString) {
		if (device != null && endPointWrite != null && mUsbManager.hasPermission(device) && !data.isEmpty()) {
			// mLog(connection +"\n"+ device +"\n"+ request +"\n"+
			// packetSize);
			byte[] out = data.getBytes();// UTF-16LE
											// Charset.forName("UTF-16")
			onUSBDataSending(data);
			if (sendAsString) {
				try {
					String str[] = data.split("[\\s]");
					out = new byte[str.length];
					for (int i = 0; i < str.length; i++) {
						out[i] = USBUtils.toByte(Integer.decode(str[i]));
					}
				} catch (Exception e) {
					onSendingError(e);
				}
			}
			int status = connection.bulkTransfer(endPointWrite, out, out.length, 250);
			onUSBDataSended(status, out);
		}
	}

	/**
	 * receives the permission request to connect usb devices
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Consts.ACTION_USB_PERMISSION.equals(action)) {
				setDevice(intent);
			}
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				setDevice(intent);
				if (device == null) {
					onDeviceConnected();
				}
			}
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				if (device != null) {
					device = null;
					usbThreadDataReceiver.stopThis();
					sendResultToUI(Consts.ACTION_USB_DEVICE_DETACHED, null);
					onDeviceDisconnected();
				}
			}
		}

		private void setDevice(Intent intent) {
			device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
				onDeviceSelected(device);
				connection = mUsbManager.openDevice(device);
				intf = device.getInterface(0);
				if (null == connection) {
					// mLog("(unable to establish connection)\n");
				} else {
					connection.claimInterface(intf, true);
				}
				try {
					if (UsbConstants.USB_DIR_OUT == intf.getEndpoint(1).getDirection()) {
						endPointWrite = intf.getEndpoint(1);
					}
				} catch (Exception e) {
					Log.e("endPointWrite", "Device have no endPointWrite", e);
				}
				try {
					if (UsbConstants.USB_DIR_IN == intf.getEndpoint(0).getDirection()) {
						endPointRead = intf.getEndpoint(0);
						packetSize = endPointRead.getMaxPacketSize();
					}
				} catch (Exception e) {
					Log.e("endPointWrite", "Device have no endPointRead", e);
				}
				usbThreadDataReceiver = new USBThreadDataReceiver();
				usbThreadDataReceiver.start();
				sendResultToUI(Consts.ACTION_USB_DEVICE_ATTACHED, null);
			}
		}
	};

	public void sendResultToUI(int resultCode, Bundle resultData) {
		resultReceiver.send(resultCode, resultData);
	}

	public void onCommand(Intent intent, String action, int flags, int startId) {
	}

	public void onUSBDataReceive(byte[] buffer) {
	}

	public void onDeviceConnected() {
	}

	public void onDeviceDisconnected() {
	}

	public void onDeviceSelected(UsbDevice device) {
	}

	public CharSequence onBuildingDevicesList(UsbDevice usbDevice) {
		return null;
	}

	public void onUSBDataSending(String data) {
	}

	public void onUSBDataSended(int status, byte[] out) {
	}

	public void onSendingError(Exception e) {
	}

}