package com.appspot.usbhidterminal;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class USBService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this, "Service created...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "Service destroyed ...", Toast.LENGTH_LONG).show();
	}

	public int onStartCommand(Intent intent, int flags, int startId) {

		Toast.makeText(this, "onStartCommand...", Toast.LENGTH_LONG).show();
		return Service.START_NOT_STICKY;
	}
}