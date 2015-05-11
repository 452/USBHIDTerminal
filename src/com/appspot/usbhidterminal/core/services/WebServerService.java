package com.appspot.usbhidterminal.core.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.appspot.usbhidterminal.core.webserver.WebServer;

import java.io.IOException;

public class WebServerService extends Service {

    private static final String tag = WebServerService.class.getCanonicalName();
    private final IBinder webServerServiceBinder = new LocalBinder();
    private WebServer webServer;

    public WebServerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return webServerServiceBinder;
    }


    public class LocalBinder extends Binder {
        public WebServerService getService() {
            return WebServerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals("start")) {
            try {
                if (webServer == null) {
                    webServer = new WebServer(this.getAssets());
                }
                webServer.start();
            } catch (IOException e) {
                Log.e(tag, "Starting Web Server error", e);
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        webServer.stop();
    }
}