package com.appspot.usbhidterminal.core.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.appspot.usbhidterminal.R;
import com.appspot.usbhidterminal.USBHIDTerminal;
import com.appspot.usbhidterminal.core.Consts;
import com.appspot.usbhidterminal.core.events.USBDataReceiveEvent;
import com.appspot.usbhidterminal.core.events.USBDataSendEvent;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

import de.greenrobot.event.EventBus;

public class SocketService extends Service {

    private static final String TAG = SocketService.class.getCanonicalName();
    private static final int DEFAULT_SOCKET_PORT = 7899;
    private EventBus eventBus = EventBus.getDefault();
    private int socketPort;
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
        Log.v(">>>>>>>>>>>>>>>", "Exit serv");
        if (socketThreadDataReceiver != null) {
            socketThreadDataReceiver.stopThis();
        }
        if (socket != null) {
            try {
                socket.shutdownInput();
                socket.shutdownOutput();
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Close streams", e);
            }
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
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
            new SocketServerTask().execute();
        }
        return START_REDELIVER_INTENT;
    }

    private void setup() {
        try {
            serverSocket = new ServerSocket(socketPort);
            serverSocket.setReuseAddress(true);
            Log.v("Socket", "Waiting connection! Port: " + socketPort);
            waitingForConnection();
            socketThreadDataReceiver = new SocketThreadDataReceiver();
            socketThreadDataReceiver.start();
        } catch (BindException e) {
        } catch (IOException e) {
            //Log.w(TAG, e);
        }
    }

    private void waitingForConnection() {
        try {
            socket = serverSocket.accept();
            out = new DataOutputStream(socket.getOutputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.writeChars("Hello from USBHIDTerminal\n");
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

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

    class SocketServerTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                setup();
                return "";
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(String feed) {
        }
    }

    private void setupNotifications() { //called in onCreate()
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, USBHIDTerminal.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
        PendingIntent pendingCloseIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, USBHIDTerminal.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .setAction(Consts.SOCKET_SERVER_CLOSE_ACTION),
                0);
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