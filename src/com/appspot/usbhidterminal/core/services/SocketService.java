package com.appspot.usbhidterminal.core.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.appspot.usbhidterminal.R;
import com.appspot.usbhidterminal.USBHIDTerminal;
import com.appspot.usbhidterminal.core.Consts;
import com.appspot.usbhidterminal.core.USBUtils;
import com.appspot.usbhidterminal.core.events.LogMessageEvent;
import com.appspot.usbhidterminal.core.events.USBDataReceiveEvent;
import com.appspot.usbhidterminal.core.events.USBDataSendEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class SocketService extends Service {

    private static final String TAG = SocketService.class.getCanonicalName();
    private static final int DEFAULT_SOCKET_PORT = 7899;
    private EventBus eventBus = EventBus.getDefault();
    private int socketPort = DEFAULT_SOCKET_PORT;
    private SocketThreadDataReceiver socketThreadDataReceiver;
    private BufferedReader in;
    private DataOutputStream out;
    private ServerSocket serverSocket;
    private Socket socket;

    private final IBinder socketServiceBinder = new LocalBinder();

    public SocketService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotifications();
        eventBus.register(this);
    }

    @Override
    public void onDestroy() {
        try {
            if (socketThreadDataReceiver != null) {
                socketThreadDataReceiver.stopThis();
            }
            if (socket != null) {
                socket.shutdownInput();
                socket.shutdownOutput();
                in.close();
                out.close();
                socket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(Consts.SOCKET_SERVER_NOTIFICATION);
        } catch (IOException e) {
            Log.e(TAG, "Close streams", e);
        }
        eventBus.unregister(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return socketServiceBinder;
    }


    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals("start")) {
            socketPort = intent.getIntExtra("SOCKET_PORT", DEFAULT_SOCKET_PORT);
            if (socketThreadDataReceiver == null) {
                socketThreadDataReceiver = new SocketThreadDataReceiver();
                socketThreadDataReceiver.start();
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                if (wm.isWifiEnabled()) {
                    String ip = USBUtils.getIpAddress(wm.getConnectionInfo().getIpAddress());
                    EventBus.getDefault().post(new LogMessageEvent("Socket service launched\n" +
                            "telnet " + ip + " " + socketPort));
                }
            }
        }
        return START_REDELIVER_INTENT;
    }

    private void setup() {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(socketPort));
            waitingForConnection();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    private void waitingForConnection() {
        try {
            socket = serverSocket.accept();
            out = new DataOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.writeChars("Hello from USBHIDTerminal\n");
        } catch (SocketException e) {
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(USBDataReceiveEvent event) {
        if (socket != null && socket.isConnected()) {
            try {
                out.writeBytes(event.getData());
            } catch (IOException e) {
            }
        }
    }

    private class SocketThreadDataReceiver extends Thread {

        private volatile boolean isStopped;

        public SocketThreadDataReceiver() {
        }

        @Override
        public void run() {
            try {
                setup();
                if (socket != null && socket.isConnected()) {
                    while (!isStopped) {
                        String data = in.readLine();
                        if (data == null) {
                            waitingForConnection();
                        } else {
                            eventBus.post(new USBDataSendEvent(data));
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
                        .setAction(Consts.SOCKET_SERVER_CLOSE_ACTION),
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
                .setContentText(getText(R.string.socket_server));
        if (mNotificationManager != null) {
            mNotificationManager.notify(Consts.SOCKET_SERVER_NOTIFICATION, mNotificationBuilder.build());
        }
    }
}