package com.appspot.usbhidterminal.core.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.appspot.usbhidterminal.core.events.LogMessageEvent;
import com.appspot.usbhidterminal.core.webserver.WebServer;

import java.io.IOException;

import de.greenrobot.event.EventBus;

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
                EventBus.getDefault().post(new LogMessageEvent("Web service launched"));
            } catch (IOException e) {
                Log.e(tag, "Starting Web Server error", e);
                EventBus.getDefault().post(new LogMessageEvent("Web service problem: " + e.getMessage()));
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        webServer.stop();
    }
}