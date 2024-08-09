package com.appspot.usbhidterminal.core.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.appspot.usbhidterminal.R;
import com.appspot.usbhidterminal.USBHIDTerminal;
import com.appspot.usbhidterminal.core.Consts;
import com.appspot.usbhidterminal.core.USBUtils;
import com.appspot.usbhidterminal.core.events.LogMessageEvent;
import com.appspot.usbhidterminal.core.webserver.WebServer;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

public class WebServerService extends Service {

    private static final String TAG = WebServerService.class.getCanonicalName();
    private static final int DEFAULT_WEB_SERVER_PORT = 7799;
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
               int serverPort = intent.getIntExtra("WEB_SERVER_PORT", DEFAULT_WEB_SERVER_PORT);
               if (webServer == null) {
                   webServer = new WebServer(this.getAssets(), serverPort);
                   webServer.start();
                   WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                   if (wm.isWifiEnabled()) {
                       String ip = USBUtils.getIpAddress(wm.getConnectionInfo().getIpAddress());
                       EventBus.getDefault().post(new LogMessageEvent("Web service launched\n" +
                               "http://" + ip + ":" + serverPort +
                               "\nws://" + ip + ":" + serverPort + "/websocket"));
                   }
               }
            } catch (IOException e) {
                Log.e(TAG, "Starting Web Server error", e);
                EventBus.getDefault().post(new LogMessageEvent("Web service problem: " + e.getMessage()));
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotifications();
    }

    @Override
    public void onDestroy() {
        if (webServer != null) {
            webServer.stop();
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(Consts.WEB_SERVER_NOTIFICATION);
        }
    }

    private void setupNotifications() { //called in onCreate()
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, USBHIDTerminal.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingCloseIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, USBHIDTerminal.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .setAction(Consts.WEB_SERVER_CLOSE_ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_launcher)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_exit), pendingCloseIntent)
                .setOngoing(true);

        mNotificationBuilder
                .setTicker(getText(R.string.app_name))
                .setContentText(getText(R.string.web_server));
        if (mNotificationManager != null) {
            mNotificationManager.notify(Consts.WEB_SERVER_NOTIFICATION, mNotificationBuilder.build());
        }
    }
}