package com.appspot.usbhidterminal.core.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.appspot.usbhidterminal.R;
import com.appspot.usbhidterminal.USBHIDTerminal;
import com.appspot.usbhidterminal.core.Consts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketService extends Service {

    private static final int SOCKET_PORT = 7899;
    private ServerSocket server;
    private static DataInputStream in;
    private static DataOutputStream out;
    private static Socket s;

    private final IBinder socketServiceBinder = new LocalBinder();

    public SocketService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupNotifications();
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
        //if (action.equals("start")) {

        //new SocketServerTask().execute();
        //}
        System.out.println("Hello>> " + action);
        return 0;
    }

    private void waitingForConnnection() {
        try {
            try {
                //ServerSocket serverSocket = new ServerSocket();
                ServerSocket serverSocket = new ServerSocket(SOCKET_PORT);
                System.out.println("Wating connection!"); //SOCKET_PORT, 0, "192.168.1.45".getBytes()
                s = serverSocket.accept();
                out = new DataOutputStream(s.getOutputStream());
                in = new DataInputStream(s.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                //out.close();
                //s.close();
                //waitingForConnnection();
            } finally {
                //out.close();
                //s.close();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    class SocketServerTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        protected String doInBackground(String... urls) {
            try {
                waitingForConnnection();
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