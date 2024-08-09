package com.appspot.usbhidterminal.core.webserver;

import android.util.Log;

import com.appspot.usbhidterminal.core.events.USBDataReceiveEvent;
import com.appspot.usbhidterminal.core.events.USBDataSendEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;

public class Ws extends WebSocket {

    private static final String TAG = Ws.class.getCanonicalName();

    private EventBus eventBus = EventBus.getDefault();
    private NanoHTTPD.IHTTPSession httpSession;
    private TimerTask pingTimer;

    public Ws(NanoHTTPD.IHTTPSession handshakeRequest) {
        super(handshakeRequest);
        this.httpSession = handshakeRequest;
        eventBus.register(this);

        // prevent connection from being closed due to inactivity:
        // send ping messages every 2 seconds, and the client will respond with pong
        pingTimer = new TimerTask() {
            @Override
            public void run(){
                try {
                    ping(new byte[0]);
                } catch (IOException e) {
                    pingTimer.cancel();
                }
            }
        };
        new Timer().schedule(pingTimer, 1000, 2000);
    }

    @Override
    protected void onPong(WebSocketFrame pongFrame) {
    }

    @Override
    protected void onMessage(WebSocketFrame messageFrame) {
        EventBus.getDefault().post(new USBDataSendEvent(messageFrame.getTextPayload()));
    }

    @Override
    protected void onClose(WebSocketFrame.CloseCode code, String reason,
                           boolean initiatedByRemote) {
        pingTimer.cancel();
        eventBus.unregister(this);
    }

    @Override
    protected void onException(IOException e) {
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(USBDataReceiveEvent event) {
        wsSend(event.getData());
    }

    private void wsSend(String data){
        try {
            this.send(data);
        } catch (IOException e) {
            Log.e(TAG, "WebSocket event send error", e);
        }
    }

}