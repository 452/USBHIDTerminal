package com.appspot.usbhidterminal;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

public class MainActivity extends Activity implements View.OnClickListener {

	private static final String ACTION_USB_PERMISSION = "com.google.android.HID.action.USB_PERMISSION";
	private static final String MESSAGE_SELECT_YOUR_USB_HID_DEVICE = "Please select your USB HID device";
	private static final String MESSAGE_CONNECT_YOUR_USB_HID_DEVICE = "Please connect your USB HID device";
	private static final String RECEIVE_DATA_FORMAT = "receiveDataFormat";
	private static final String BINARY = "binary";
	private static final String INTEGER = "integer";
	private static final String HEXADECIMAL = "hexadecimal";
	private static final String TEXT = "text";
	private static final String DELIMITER = "delimiter";
	private static final String DELIMITER_NONE = "none";
	private static final String DELIMITER_NEW_LINE = "newLine";
	private static final String DELIMITER_SPACE = "space";
	private static final String NEW_LINE = "\n";
	private static final String SPACE = " ";

	private PendingIntent mPermissionIntent;

	private SharedPreferences sharedPreferences;

	private UsbDevice device;
	private UsbManager mUsbManager;

	private UsbInterface intf;
	private UsbEndpoint endPointRead;
	private UsbEndpoint endPointWrite;
	private UsbDeviceConnection connection;
	private int packetSize;

	private EditText log_txt;
	private EditText edtxtHidInput;
	private Button btnSend;
	private Button btnSelectHIDDevice;
	private Button btnClear;
	private RadioButton radioButton;
	private Timer myTimer = new Timer();
	private final Handler uiHandler = new Handler();
	private String settingsDelimiter;
	private String delimiter;

	private String receiveDataFormat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.activity_main);
			setVersionToTitle();

			btnSend = (Button) findViewById(R.id.btnSend);
			btnSend.setOnClickListener(this);

			btnSelectHIDDevice = (Button) findViewById(R.id.btnSelectHIDDevice);
			btnSelectHIDDevice.setOnClickListener(this);

			btnClear = (Button) findViewById(R.id.btnClear);
			btnClear.setOnClickListener(this);

			edtxtHidInput = (EditText) findViewById(R.id.edtxtHidInput);
			log_txt = (EditText) findViewById(R.id.log_txt);

			radioButton = (RadioButton) findViewById(R.id.rbSendData);

			mLog("Initialized\nPlease select your USB HID device");
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
			mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
			IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
			filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
			filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
			registerReceiver(mUsbReceiver, filter);
			edtxtHidInput.setText("129");
			// btnSend.setEnabled(true);
			setupReceiver();
		} catch (Exception e) {
			Log.e("Init", "Initialization error", e);
		}
	}

	private void setupReceiver() {
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				{
					try {
						if (connection != null && endPointRead != null) {
							final byte[] buffer = new byte[packetSize];
							final int status = connection.bulkTransfer(endPointRead, buffer, packetSize, 300);
							uiHandler.post(new Runnable() {
								@Override
								public void run() {
									if (status >= 0) {
										StringBuilder stringBuilder = new StringBuilder();
										if (receiveDataFormat.equals(INTEGER)) {
											for (int i = 0; i < packetSize; i++) {
												if (buffer[i] != 0) {
													stringBuilder.append(delimiter).append(String.valueOf(toInt(buffer[i])));
												} else {
													break;
												}
											}
										} else if (receiveDataFormat.equals(HEXADECIMAL)) {
											for (int i = 0; i < packetSize; i++) {
												if (buffer[i] != 0) {
													stringBuilder.append(delimiter).append(Integer.toHexString(buffer[i]));
												} else {
													break;
												}
											}
										} else if (receiveDataFormat.equals(TEXT)) {
											for (int i = 0; i < packetSize; i++) {
												if (buffer[i] != 0) {
													stringBuilder.append(String.valueOf((char) buffer[i]));
												} else {
													break;
												}
											}
										} else if (receiveDataFormat.equals(BINARY)) {
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
					} catch (Exception e) {
						mLog("Exception: " + e.getLocalizedMessage());
						Log.w("setupReceiver", e);
					}
				}
			};
		}, 0L, 1);
	}

	public void onClick(View v) {
		if (v == btnSend) {
			if (device != null && endPointWrite != null && mUsbManager.hasPermission(device) && !edtxtHidInput.getText().toString().isEmpty()) {
				// mLog(connection +"\n"+ device +"\n"+ request +"\n"+
				// packetSize);
				byte[] out = edtxtHidInput.getText().toString().getBytes();// UTF-16LE
																			// Charset.forName("UTF-16")
				mLog("Sending: " + edtxtHidInput.getText().toString());
				if (radioButton.isChecked()) {
					try {
						String str[] = edtxtHidInput.getText().toString().split("[\\s]");
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
						mLogC(SPACE + toInt(out[i]));
					} else {
						break;
					}
				}
			}
		}
		if (v == btnClear) {
			log_txt.setText("");
		}
		if (v == btnSelectHIDDevice) {
			showListOfDevices();
		}
	}

	void showListOfDevices() {
		btnSend.setEnabled(false);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		if (mUsbManager.getDeviceList().size() == 0) {
			builder.setTitle(MESSAGE_CONNECT_YOUR_USB_HID_DEVICE);
		} else {
			builder.setTitle(MESSAGE_SELECT_YOUR_USB_HID_DEVICE);
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

	/**
	 * receives the permission request to connect usb devices
	 */
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					setDevice(intent);
				}
			}
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				synchronized (this) {
					setDevice(intent);
				}
				if (device == null) {
					mLog("device connected");
				}
			}
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				if (device != null) {
					device = null;
					btnSend.setEnabled(false);
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
				btnSend.setEnabled(true);
			}
		}
	};

	@Override
	protected void onStart() {
		super.onStart();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		receiveDataFormat = sharedPreferences.getString(RECEIVE_DATA_FORMAT, TEXT);
		setDelimiter();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		setSelectedMenuItemsFromSettings(menu);
		return true;
	}

	private void setSelectedMenuItemsFromSettings(Menu menu) {
		receiveDataFormat = sharedPreferences.getString(RECEIVE_DATA_FORMAT, TEXT);
		if (receiveDataFormat.equals(BINARY)) {
			menu.findItem(R.id.menuSettingsReceiveBinary).setChecked(true);
		} else if (receiveDataFormat.equals(INTEGER)) {
			menu.findItem(R.id.menuSettingsReceiveInteger).setChecked(true);
		} else if (receiveDataFormat.equals(HEXADECIMAL)) {
			menu.findItem(R.id.menuSettingsReceiveHexadecimal).setChecked(true);
		} else if (receiveDataFormat.equals(TEXT)) {
			menu.findItem(R.id.menuSettingsReceiveText).setChecked(true);
		}

		setDelimiter();
		if (settingsDelimiter.equals(DELIMITER_NONE)) {
			menu.findItem(R.id.menuSettingsDelimiterNone).setChecked(true);
		} else if (settingsDelimiter.equals(DELIMITER_NEW_LINE)) {
			menu.findItem(R.id.menuSettingsDelimiterNewLine).setChecked(true);
		} else if (settingsDelimiter.equals(DELIMITER_SPACE)) {
			menu.findItem(R.id.menuSettingsDelimiterSpace).setChecked(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		item.setChecked(true);
		switch (item.getItemId()) {
		case R.id.menuSettingsReceiveBinary:
			editor.putString(RECEIVE_DATA_FORMAT, BINARY);
			editor.commit();
			break;
		case R.id.menuSettingsReceiveInteger:
			editor.putString(RECEIVE_DATA_FORMAT, INTEGER);
			editor.commit();
			break;
		case R.id.menuSettingsReceiveHexadecimal:
			editor.putString(RECEIVE_DATA_FORMAT, HEXADECIMAL);
			editor.commit();
			break;
		case R.id.menuSettingsReceiveText:
			editor.putString(RECEIVE_DATA_FORMAT, TEXT);
			editor.commit();
			break;
		case R.id.menuSettingsDelimiterNone:
			editor.putString(DELIMITER, DELIMITER_NONE);
			editor.commit();
			break;
		case R.id.menuSettingsDelimiterNewLine:
			editor.putString(DELIMITER, DELIMITER_NEW_LINE);
			editor.commit();
			break;
		case R.id.menuSettingsDelimiterSpace:
			editor.putString(DELIMITER, DELIMITER_SPACE);
			editor.commit();
			break;
		}

		receiveDataFormat = sharedPreferences.getString(RECEIVE_DATA_FORMAT, TEXT);
		setDelimiter();
		return true;
	}

	private static int toInt(byte b) {
		return (int) b & 0xFF;
	}

	private static byte toByte(int c) {
		return (byte) (c <= 0x7f ? c : ((c % 0x80) - 0x80));
	}

	private void setDelimiter() {
		settingsDelimiter = sharedPreferences.getString(DELIMITER, DELIMITER_NEW_LINE);
		if (settingsDelimiter.equals(DELIMITER_NONE)) {
			delimiter = "";
		} else if (settingsDelimiter.equals(DELIMITER_NEW_LINE)) {
			delimiter = NEW_LINE;
		} else if (settingsDelimiter.equals(DELIMITER_SPACE)) {
			delimiter = SPACE;
		}
	}

	private void mLog(String log) {
		log_txt.append(NEW_LINE);
		log_txt.append(log);
		log_txt.setSelection(log_txt.getText().length());
	}

	private void mLogC(String log) {
		log_txt.append(log);
		log_txt.setSelection(log_txt.getText().length());
	}

	private void setVersionToTitle() {
		try {
			this.setTitle(SPACE + this.getTitle() + SPACE + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}
