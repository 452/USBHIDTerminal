package com.appspot.usbhidterminal.core.services;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

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

}