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
import android.widget.Toast;

public class USBHIDService extends Service {

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

	private String delimiter;
	private String receiveDataFormat;
	private int packetSize;
	private boolean sendedDataType;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this, "Service onCreate ...", Toast.LENGTH_SHORT).show();
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
		Toast.makeText(this, "Service onStartCommand ...", Toast.LENGTH_SHORT).show();
		if (resultReceiver == null) {
			resultReceiver = intent.getParcelableExtra("receiver");
		}
		if (Consts.ACTION_USB_DATA_TYPE.equals(action)) {
			sendedDataType = intent.getBooleanExtra(Consts.ACTION_USB_DATA_TYPE, false);
		}
		if (Consts.ACTION_USB_SEND_DATA.equals(action)) {
			sendData(intent.getStringExtra(Consts.ACTION_USB_SEND_DATA), sendedDataType);
		}
		if (Consts.RECEIVE_DATA_FORMAT.equals(action)) {
			receiveDataFormat = intent.getStringExtra(Consts.RECEIVE_DATA_FORMAT);
			delimiter = intent.getStringExtra(Consts.DELIMITER);
		}
		if (Consts.ACTION_USB_SHOW_DEVICES_LIST.equals(action)) {
			mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
			List<CharSequence> list = new LinkedList<CharSequence>();
			for (UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
				list.add("devID:" + usbDevice.getDeviceId() + " VID:" + Integer.toHexString(usbDevice.getVendorId()) + " PID:" + Integer.toHexString(usbDevice.getProductId()) + " " + usbDevice.getDeviceName());
			}
			final CharSequence devicesName[] = new CharSequence[mUsbManager.getDeviceList().size()];
			list.toArray(devicesName);
			Bundle bundle = new Bundle();
			bundle.putCharSequenceArray(Consts.ACTION_USB_SHOW_DEVICES_LIST, devicesName);
			resultReceiver.send(Consts.ACTION_USB_SHOW_DEVICES_LIST_RESULT, bundle);
		}
		if (Consts.ACTION_USB_SELECT_DEVICE.equals(action)) {
			device = (UsbDevice) mUsbManager.getDeviceList().values().toArray()[intent.getIntExtra(Consts.ACTION_USB_SELECT_DEVICE, 0)];
			mUsbManager.requestPermission(device, mPermissionIntent);
		}
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "Service destroyed ...", Toast.LENGTH_SHORT).show();
		if (usbThreadDataReceiver != null) {
			usbThreadDataReceiver.stopThis();
		}
		unregisterReceiver(mUsbReceiver);
	}

	class USBThreadDataReceiver extends Thread {

		private volatile boolean isStopped;

		public USBThreadDataReceiver() {
		}

		@Override
		public void run() {
			try {
				if (connection != null && endPointRead != null) {
					final byte[] buffer = new byte[packetSize];

					while (!isStopped) {
						final int status = connection.bulkTransfer(endPointRead, buffer, packetSize, 300);
						uiHandler.post(new Runnable() {
							@Override
							public void run() {
								if (status >= 0) {
									StringBuilder stringBuilder = new StringBuilder();
									if (receiveDataFormat.equals(Consts.INTEGER)) {
										for (int i = 0; i < packetSize; i++) {
											if (buffer[i] != 0) {
												stringBuilder.append(delimiter).append(String.valueOf(toInt(buffer[i])));
											} else {
												break;
											}
										}
									} else if (receiveDataFormat.equals(Consts.HEXADECIMAL)) {
										for (int i = 0; i < packetSize; i++) {
											if (buffer[i] != 0) {
												stringBuilder.append(delimiter).append(Integer.toHexString(buffer[i]));
											} else {
												break;
											}
										}
									} else if (receiveDataFormat.equals(Consts.TEXT)) {
										for (int i = 0; i < packetSize; i++) {
											if (buffer[i] != 0) {
												stringBuilder.append(String.valueOf((char) buffer[i]));
											} else {
												break;
											}
										}
									} else if (receiveDataFormat.equals(Consts.BINARY)) {
										for (int i = 0; i < packetSize; i++) {
											if (buffer[i] != 0) {
												stringBuilder.append(delimiter).append("0b").append(Integer.toBinaryString(Integer.valueOf(buffer[i])));
											} else {
												break;
											}
										}
									}
									stringBuilder.append("\nreceived ").append(status).append(" bytes");
									mLog(stringBuilder.toString());
								}
							}
						});
					}
				}
			} catch (Exception e) {
				// mLog("Exception: " + e.getLocalizedMessage());
				Log.e("setupReceiver", e.getMessage(), e);
			}
			if (isInterrupted()) {
				return;
			}

		}

		public void stopThis() {
			isStopped = true;
			this.interrupt();
		}
	}

	private void sendData(String data, boolean sendAsString) {
		if (device != null && endPointWrite != null && mUsbManager.hasPermission(device) && !data.isEmpty()) {
			// mLog(connection +"\n"+ device +"\n"+ request +"\n"+
			// packetSize);
			byte[] out = data.getBytes();// UTF-16LE
											// Charset.forName("UTF-16")
			mLog("Sending: " + data);
			if (sendAsString) {
				try {
					String str[] = data.split("[\\s]");
					out = new byte[str.length];
					for (int i = 0; i < str.length; i++) {
						out[i] = toByte(Integer.decode(str[i]));
					}
				} catch (Exception e) {
					mLog("Please check your bytes, sent as text");
				}
			}
			int status = connection.bulkTransfer(endPointWrite, out, out.length, 250);
			mLog("sended " + status + " bytes");
			for (int i = 0; i < out.length; i++) {
				if (out[i] != 0) {
					mLogC(Consts.SPACE + toInt(out[i]));
				} else {
					break;
				}
			}
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
					mLog("device connected");
				}
			}
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				if (device != null) {
					device = null;
					usbThreadDataReceiver.stopThis();
					resultReceiver.send(Consts.ACTION_USB_DEVICE_DETACHED, null);
				}
				mLog("device disconnected");
			}
		}

		private void setDevice(Intent intent) {
			device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			if (device != null && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
				mLog("Selected device VID:" + Integer.toHexString(device.getVendorId()) + " PID:" + Integer.toHexString(device.getProductId()));
				connection = mUsbManager.openDevice(device);
				intf = device.getInterface(0);
				if (null == connection) {
					mLog("(unable to establish connection)\n");
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
				resultReceiver.send(Consts.ACTION_USB_DEVICE_ATTACHED, null);
			}
		}
	};

	private void mLog(String value) {
		Bundle bundle = new Bundle();
		bundle.putString("log", value);
		resultReceiver.send(Consts.ACTION_USB_LOG, bundle);
	}

	private void mLogC(String value) {
		Bundle bundle = new Bundle();
		bundle.putString("log", value);
		resultReceiver.send(Consts.ACTION_USB_LOG_C, bundle);
	}

	private static int toInt(byte b) {
		return (int) b & 0xFF;
	}

	private static byte toByte(int c) {
		return (byte) (c <= 0x7f ? c : ((c % 0x80) - 0x80));
	}

}