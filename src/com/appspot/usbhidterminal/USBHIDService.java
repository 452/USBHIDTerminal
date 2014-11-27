package com.appspot.usbhidterminal;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;

public class USBHIDService extends Service {

	private final IBinder mBinder = new LocalBinder();

	private USBThreadDataReceiver usbThreadDataReceiver;
	private ResultReceiver resultReceiver;

	private final Handler uiHandler = new Handler();

	private UsbManager mUsbManager;
	private UsbInterface intf;
	private UsbEndpoint endPointRead;
	private UsbEndpoint endPointWrite;
	private UsbDeviceConnection connection;
	private UsbDevice device;

	private Activity activity;
	private IntentFilter filter;
	private PendingIntent mPermissionIntent;

	private String delimiter;
	private String receiveDataFormat;
	private int packetSize;

	@Override
	public IBinder onBind(Intent intent) {
		//Toast.makeText(this, "onBind ...", Toast.LENGTH_SHORT).show();
		resultReceiver = intent.getParcelableExtra("receiver");
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(Consts.ACTION_USB_PERMISSION), 0);
		filter = new IntentFilter(Consts.ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Toast.makeText(this, "Service destroyed ...", Toast.LENGTH_SHORT).show();
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

	public void sendData(String data, boolean sendAsString) {
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

	void showListOfDevices() {
		if (activity != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
			if (mUsbManager.getDeviceList().size() == 0) {
				builder.setTitle(Consts.MESSAGE_CONNECT_YOUR_USB_HID_DEVICE);
			} else {
				builder.setTitle(Consts.MESSAGE_SELECT_YOUR_USB_HID_DEVICE);
			}
			List<CharSequence> list = new LinkedList<CharSequence>();
			for (UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
				list.add("devID:" + usbDevice.getDeviceId() + " VID:" + Integer.toHexString(usbDevice.getVendorId()) + " PID:" + Integer.toHexString(usbDevice.getProductId()) + " " + usbDevice.getDeviceName());
			}
			final CharSequence devicesName[] = new CharSequence[mUsbManager.getDeviceList().size()];
			list.toArray(devicesName);
			builder.setItems(devicesName, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					device = (UsbDevice) mUsbManager.getDeviceList().values().toArray()[which];
					mUsbManager.requestPermission(device, mPermissionIntent);
				}
			});
			builder.setCancelable(true);
			builder.show();
		}
	}

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

	/**
	 * Class used for the client Binder. Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		USBHIDService getService() {
			// Return this instance of LocalService so clients can call public
			// methods
			return USBHIDService.this;
		}
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public void setReceiveDataFormat(String receiveDataFormat) {
		this.receiveDataFormat = receiveDataFormat;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

}