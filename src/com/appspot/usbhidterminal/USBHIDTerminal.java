package com.appspot.usbhidterminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.appspot.usbhidterminal.core.Consts;
import com.appspot.usbhidterminal.core.USBHIDService;

public class USBHIDTerminal extends Activity implements View.OnClickListener {

	private SharedPreferences sharedPreferences;

	private Intent usbService;
	private USBServiceResultReceiver usbServiceResultReceiver;

	private EditText edtlogText;
	private EditText edtxtHidInput;
	private Button btnSend;
	private Button btnSelectHIDDevice;
	private Button btnClear;
	private RadioButton rbSendText;
	private RadioButton rbSendDataType;
	private String settingsDelimiter;

	private String receiveDataFormat;
	private String delimiter;

	class USBServiceResultReceiver extends ResultReceiver {

		public USBServiceResultReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			if (resultCode == Consts.ACTION_USB_LOG) {
				mLog(resultData.getString("log"), false);
			} else if (resultCode == Consts.ACTION_USB_LOG_C) {
				mLog(resultData.getString("log"), true);
			} else if (resultCode == Consts.ACTION_USB_DEVICE_ATTACHED) {
				btnSend.setEnabled(true);
			} else if (resultCode == Consts.ACTION_USB_DEVICE_DETACHED) {
				btnSend.setEnabled(false);
			} else if (resultCode == Consts.ACTION_USB_SHOW_DEVICES_LIST_RESULT) {
				showListOfDevices(resultData.getCharSequenceArray(Consts.ACTION_USB_SHOW_DEVICES_LIST));
			}
		}

	}

	private void prepareUSBHIDService() {
		usbService = new Intent(this, USBHIDService.class);
		usbServiceResultReceiver = new USBServiceResultReceiver(null);
		usbService.putExtra("receiver", usbServiceResultReceiver);
		startService(usbService);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initUI();
	}

	private void initUI() {
		setVersionToTitle();
		btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(this);

		btnSelectHIDDevice = (Button) findViewById(R.id.btnSelectHIDDevice);
		btnSelectHIDDevice.setOnClickListener(this);

		btnClear = (Button) findViewById(R.id.btnClear);
		btnClear.setOnClickListener(this);

		edtxtHidInput = (EditText) findViewById(R.id.edtxtHidInput);
		edtlogText = (EditText) findViewById(R.id.edtlogText);

		rbSendDataType = (RadioButton) findViewById(R.id.rbSendData);
		rbSendText = (RadioButton) findViewById(R.id.rbSendText);
		rbSendDataType.setOnClickListener(this);
		rbSendText.setOnClickListener(this);

		mLog("Initialized\nPlease select your USB HID device\n", false);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		edtxtHidInput.setText("test");
		// btnSend.setEnabled(true);
	}

	public void onClick(View v) {
		if (v == btnSend) {
			sendToUSBService(Consts.ACTION_USB_SEND_DATA, edtxtHidInput.getText().toString());
		} else if (v == rbSendText || v == rbSendDataType) {
			sendToUSBService(Consts.ACTION_USB_DATA_TYPE, rbSendDataType.isChecked());
		} else if (v == btnClear) {
			edtlogText.setText("");
		} else if (v == btnSelectHIDDevice) {
			sendToUSBService(Consts.ACTION_USB_SHOW_DEVICES_LIST);
		}
	}

	void showListOfDevices(CharSequence devicesName[]) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if (devicesName.length == 0) {
			builder.setTitle(Consts.MESSAGE_CONNECT_YOUR_USB_HID_DEVICE);
		} else {
			builder.setTitle(Consts.MESSAGE_SELECT_YOUR_USB_HID_DEVICE);
		}

		builder.setItems(devicesName, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				sendToUSBService(Consts.ACTION_USB_SELECT_DEVICE, which);
			}
		});
		builder.setCancelable(true);
		builder.show();
	}

	@Override
	protected void onStart() {
		super.onStart();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		receiveDataFormat = sharedPreferences.getString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT);
		prepareUSBHIDService();
		setDelimiter();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		setSelectedMenuItemsFromSettings(menu);
		return true;
	}

	private void setSelectedMenuItemsFromSettings(Menu menu) {
		receiveDataFormat = sharedPreferences.getString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT);
		if (receiveDataFormat.equals(Consts.BINARY)) {
			menu.findItem(R.id.menuSettingsReceiveBinary).setChecked(true);
		} else if (receiveDataFormat.equals(Consts.INTEGER)) {
			menu.findItem(R.id.menuSettingsReceiveInteger).setChecked(true);
		} else if (receiveDataFormat.equals(Consts.HEXADECIMAL)) {
			menu.findItem(R.id.menuSettingsReceiveHexadecimal).setChecked(true);
		} else if (receiveDataFormat.equals(Consts.TEXT)) {
			menu.findItem(R.id.menuSettingsReceiveText).setChecked(true);
		}

		setDelimiter();
		if (settingsDelimiter.equals(Consts.DELIMITER_NONE)) {
			menu.findItem(R.id.menuSettingsDelimiterNone).setChecked(true);
		} else if (settingsDelimiter.equals(Consts.DELIMITER_NEW_LINE)) {
			menu.findItem(R.id.menuSettingsDelimiterNewLine).setChecked(true);
		} else if (settingsDelimiter.equals(Consts.DELIMITER_SPACE)) {
			menu.findItem(R.id.menuSettingsDelimiterSpace).setChecked(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences.Editor editor = sharedPreferences.edit();
		item.setChecked(true);
		switch (item.getItemId()) {
		case R.id.menuSettingsReceiveBinary:
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.BINARY).apply();
			break;
		case R.id.menuSettingsReceiveInteger:
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.INTEGER).apply();
			break;
		case R.id.menuSettingsReceiveHexadecimal:
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.HEXADECIMAL).apply();
			break;
		case R.id.menuSettingsReceiveText:
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT).apply();
			break;
		case R.id.menuSettingsDelimiterNone:
			editor.putString(Consts.DELIMITER, Consts.DELIMITER_NONE).apply();
			break;
		case R.id.menuSettingsDelimiterNewLine:
			editor.putString(Consts.DELIMITER, Consts.DELIMITER_NEW_LINE).apply();
			break;
		case R.id.menuSettingsDelimiterSpace:
			editor.putString(Consts.DELIMITER, Consts.DELIMITER_SPACE).apply();
			break;
		}

		receiveDataFormat = sharedPreferences.getString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT);
		setDelimiter();
		return true;
	}

	private void setDelimiter() {
		settingsDelimiter = sharedPreferences.getString(Consts.DELIMITER, Consts.DELIMITER_NEW_LINE);
		if (settingsDelimiter.equals(Consts.DELIMITER_NONE)) {
			delimiter = "";
		} else if (settingsDelimiter.equals(Consts.DELIMITER_NEW_LINE)) {
			delimiter = Consts.NEW_LINE;
		} else if (settingsDelimiter.equals(Consts.DELIMITER_SPACE)) {
			delimiter = Consts.SPACE;
		}
		usbService.setAction(Consts.RECEIVE_DATA_FORMAT);
		usbService.putExtra(Consts.RECEIVE_DATA_FORMAT, receiveDataFormat);
		usbService.putExtra(Consts.DELIMITER, delimiter);
		startService(usbService);
	}

	void sendToUSBService(String action) {
		usbService.setAction(action);
		startService(usbService);
	}

	void sendToUSBService(String action, String data) {
		usbService.putExtra(action, data);
		sendToUSBService(action);
	}

	void sendToUSBService(String action, boolean data) {
		usbService.putExtra(action, data);
		sendToUSBService(action);
	}

	void sendToUSBService(String action, int data) {
		usbService.putExtra(action, data);
		sendToUSBService(action);
	}

	private void mLog(String log, boolean newLine) {
		if (newLine) {
			edtlogText.append(Consts.NEW_LINE);
		}
		edtlogText.append(log);
		edtlogText.setSelection(edtlogText.getText().length());
	}

	private void setVersionToTitle() {
		try {
			this.setTitle(Consts.SPACE + this.getTitle() + Consts.SPACE + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}