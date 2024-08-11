package com.appspot.usbhidterminal.core.services;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import java.util.HashMap;
import java.util.Iterator;
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
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.appspot.usbhidterminal.core.Consts;
import com.appspot.usbhidterminal.core.USBUtils;
import com.appspot.usbhidterminal.core.events.PrepareDevicesListEvent;
import com.appspot.usbhidterminal.core.events.SelectDeviceEvent;
import com.appspot.usbhidterminal.core.events.USBDataSendEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class AbstractUSBHIDService extends Service {

	private static final String TAG = AbstractUSBHIDService.class.getCanonicalName();

	public static final int REQUEST_GET_REPORT = 0x01;
	public static final int REQUEST_SET_REPORT = 0x09;
	public static final int REPORT_TYPE_INPUT = 0x0100;
	public static final int REPORT_TYPE_OUTPUT = 0x0200;
	public static final int REPORT_TYPE_FEATURE = 0x0300;

	private USBThreadDataReceiver usbThreadDataReceiver;

	private final Handler uiHandler = new Handler();

	private List<UsbInterface> interfacesList = null;

	private UsbManager mUsbManager;
	private UsbDeviceConnection connection;
	private UsbDevice device;

	private IntentFilter filter;
	private PendingIntent mPermissionIntent;

	private boolean sendedDataType;

	protected EventBus eventBus = EventBus.getDefault();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(Consts.ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT : 0));
		filter = new IntentFilter(Consts.ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		filter.addAction(Consts.ACTION_USB_SHOW_DEVICES_LIST);
		filter.addAction(Consts.ACTION_USB_DATA_TYPE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			registerReceiver(mUsbReceiver, filter, Context.RECEIVER_EXPORTED);
		} else {
			registerReceiver(mUsbReceiver, filter);
		}
		eventBus.register(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if (Consts.ACTION_USB_DATA_TYPE.equals(action)) {
			sendedDataType = intent.getBooleanExtra(Consts.ACTION_USB_DATA_TYPE, false);
		}
		onCommand(intent, action, flags, startId);
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onDestroy() {
		eventBus.unregister(this);
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
			try {
				if (connection != null) {
					while (!isStopped) {
						for (UsbInterface intf: interfacesList) {
							for (int i = 0; i < intf.getEndpointCount(); i++) {
								UsbEndpoint endPointRead = intf.getEndpoint(i);
								if (UsbConstants.USB_DIR_IN == endPointRead.getDirection()) {
									final byte[] buffer = new byte[endPointRead.getMaxPacketSize()];
									final int status = connection.bulkTransfer(endPointRead, buffer, buffer.length, 100);
									if (status > 0) {
										uiHandler.post(new Runnable() {
											@Override
											public void run() {
												onUSBDataReceive(buffer);
											}
										});
									} else {
										int transfer = connection.controlTransfer(0xA0, REQUEST_GET_REPORT, REPORT_TYPE_OUTPUT, 0x00, buffer, buffer.length, 100);
										if (transfer > 0) {
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
						}
					}
				}
			} catch (Exception e) {
				Log.e(TAG, "Error in receive thread", e);
			}
		}

		public void stopThis() {
			isStopped = true;
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEventMainThread(USBDataSendEvent event){
		sendData(event.getData(), sendedDataType);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEvent(SelectDeviceEvent event) {
		device = (UsbDevice) mUsbManager.getDeviceList().values().toArray()[event.getDevice()];
		mUsbManager.requestPermission(device, mPermissionIntent);
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEventMainThread(PrepareDevicesListEvent event) {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<CharSequence> list = new LinkedList<CharSequence>();
        for (UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {
            list.add(onBuildingDevicesList(usbDevice));
        }
        final CharSequence devicesName[] = new CharSequence[mUsbManager.getDeviceList().size()];
        list.toArray(devicesName);
		onShowDevicesList(devicesName);
    }

	private void sendData(String data, boolean sendAsString) {
		if (device != null && mUsbManager.hasPermission(device) && !data.isEmpty()) {
			// mLog(connection +"\n"+ device +"\n"+ request +"\n"+
			// packetSize);
			for (UsbInterface intf: interfacesList) {
				for (int i = 0; i < intf.getEndpointCount(); i++) {
					UsbEndpoint endPointWrite = intf.getEndpoint(i);
					if (UsbConstants.USB_DIR_OUT == endPointWrite.getDirection()) {
						byte[] out = data.getBytes();// UTF-16LE
						// Charset.forName("UTF-16")
						onUSBDataSending(data);
						if (sendAsString) {
							try {
								String str[] = data.split("[\\s]");
								out = new byte[str.length];
								for (int s = 0; s < str.length; s++) {
									out[s] = USBUtils.toByte(Integer.decode(str[s]));
								}
							} catch (Exception e) {
								onSendingError(e);
							}
						}
						int status = connection.bulkTransfer(endPointWrite, out, out.length, 250);
						onUSBDataSended(status, out);
						status = connection.controlTransfer(0x21, REQUEST_SET_REPORT, REPORT_TYPE_OUTPUT, 0x02, out, out.length, 250);
						onUSBDataSended(status, out);
					}
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
				if (device != null) {
					onDeviceConnected(device);
				}
			}
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				if (device != null) {
					device = null;
					if (usbThreadDataReceiver != null) {
						usbThreadDataReceiver.stopThis();
					}
					onDeviceDisconnected(device);
				}
			}
		}

		private void setDevice(Intent intent) {
			synchronized (this) {
				device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				Log.d(TAG, "Broadcast received. Device: " + device);
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
					Log.d(TAG, "Permission granted for device: " + device.getDeviceName());
					onDeviceSelected(device);
					connection = mUsbManager.openDevice(device);
					if (connection == null) {
						return;
					}
					interfacesList = new LinkedList<>();
					for (int i = 0; i < device.getInterfaceCount(); i++) {
						UsbInterface intf = device.getInterface(i);
						connection.claimInterface(intf, true);
						interfacesList.add(intf);
					}
					usbThreadDataReceiver = new USBThreadDataReceiver();
					usbThreadDataReceiver.start();
					onDeviceAttached(device);
				} else {
					Log.e(TAG, "Permission denied for the USB HID device");
				}
			}
		}
	};

	public void onCommand(Intent intent, String action, int flags, int startId) {
	}

	public void onUSBDataReceive(byte[] buffer) {
	}

	public void onDeviceConnected(UsbDevice device) {
	}

	public void onDeviceDisconnected(UsbDevice device) {
	}

	public void onDeviceSelected(UsbDevice device) {
	}

	public void onDeviceAttached(UsbDevice device) {
	}

	public void onShowDevicesList(CharSequence[] deviceName) {
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