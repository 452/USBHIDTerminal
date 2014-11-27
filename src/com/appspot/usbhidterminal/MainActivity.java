package com.appspot.usbhidterminal;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.appspot.usbhidterminal.USBHIDService.LocalBinder;

public class MainActivity extends Activity implements View.OnClickListener {

	private Activity activity;
	private SharedPreferences sharedPreferences;

	private USBHIDService usbService;
	private USBServiceResultReceiver usbServiceResultReceiver;

	private EditText log_txt;
	private EditText edtxtHidInput;
	private Button btnSend;
	private Button btnSelectHIDDevice;
	private Button btnClear;
	private RadioButton radioButton;
	private String settingsDelimiter;

	private String receiveDataFormat;
	private String delimiter;
	boolean mBound;

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			usbService = binder.getService();
			mBound = true;
			Toast.makeText(getApplicationContext(), "onServiceConnected ...", Toast.LENGTH_SHORT).show();
			usbService.setActivity(activity);
			setDelimiter();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Toast.makeText(getApplicationContext(), "onServiceDisconnected ...", Toast.LENGTH_SHORT).show();
			mBound = false;
		}

	};

	class USBServiceResultReceiver extends ResultReceiver {

		public USBServiceResultReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			if (resultCode == Consts.ACTION_USB_LOG) {
				mLog(resultData.getString("log"));
			}
			if (resultCode == Consts.ACTION_USB_LOG_C) {
				mLogC(resultData.getString("log"));
			}
			if (resultCode == Consts.ACTION_USB_DEVICE_ATTACHED) {
				btnSend.setEnabled(true);
			}
			if (resultCode == Consts.ACTION_USB_DEVICE_DETACHED) {
				btnSend.setEnabled(false);
			}
		}

	}

	private void prepareUSBHIDService() {
		Intent intent = new Intent(activity, USBHIDService.class);
		usbServiceResultReceiver = new USBServiceResultReceiver(null);
		intent.putExtra("receiver", usbServiceResultReceiver);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setVersionToTitle();
		activity = this;
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
		edtxtHidInput.setText("129");
		// btnSend.setEnabled(true);
	}

	public void onClick(View v) {
		if (v == btnSend) {
			usbService.sendData(edtxtHidInput.getText().toString(), radioButton.isChecked());
		}
		if (v == btnClear) {
			log_txt.setText("");
		}
		if (v == btnSelectHIDDevice) {
			usbService.showListOfDevices();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		receiveDataFormat = sharedPreferences.getString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT);
		setDelimiter();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		prepareUSBHIDService();
	}

	@Override
	protected void onPause() {
		super.onPause();
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
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.BINARY);
			editor.commit();
			break;
		case R.id.menuSettingsReceiveInteger:
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.INTEGER);
			editor.commit();
			break;
		case R.id.menuSettingsReceiveHexadecimal:
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.HEXADECIMAL);
			editor.commit();
			break;
		case R.id.menuSettingsReceiveText:
			editor.putString(Consts.RECEIVE_DATA_FORMAT, Consts.TEXT);
			editor.commit();
			break;
		case R.id.menuSettingsDelimiterNone:
			editor.putString(Consts.DELIMITER, Consts.DELIMITER_NONE);
			editor.commit();
			break;
		case R.id.menuSettingsDelimiterNewLine:
			editor.putString(Consts.DELIMITER, Consts.DELIMITER_NEW_LINE);
			editor.commit();
			break;
		case R.id.menuSettingsDelimiterSpace:
			editor.putString(Consts.DELIMITER, Consts.DELIMITER_SPACE);
			editor.commit();
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
		if (usbService != null) {
			usbService.setReceiveDataFormat(receiveDataFormat);
			usbService.setDelimiter(delimiter);
		}
	}

	private void mLog(String log) {
		log_txt.append(Consts.NEW_LINE);
		log_txt.append(log);
		log_txt.setSelection(log_txt.getText().length());
	}

	private void mLogC(String log) {
		log_txt.append(log);
		log_txt.setSelection(log_txt.getText().length());
	}

	private void setVersionToTitle() {
		try {
			this.setTitle(Consts.SPACE + this.getTitle() + Consts.SPACE + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
}