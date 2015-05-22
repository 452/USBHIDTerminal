package com.appspot.usbhidterminal.core.webserver;

import android.util.Log;

import com.appspot.usbhidterminal.core.events.USBDataReceiveEvent;
import com.appspot.usbhidterminal.core.events.USBDataSendEvent;

import java.io.IOException;

import de.greenrobot.event.EventBus;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;

public class Ws extends WebSocket {

    private static final String TAG = Ws.class.getCanonicalName();

    private EventBus eventBus = EventBus.getDefault();
    private NanoHTTPD.IHTTPSession httpSession;

    public Ws(NanoHTTPD.IHTTPSession handshakeRequest) {
        super(handshakeRequest);
        this.httpSession = handshakeRequest;
        eventBus.register(this);
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
        eventBus.unregister(this);
    }

    @Override
    protected void onException(IOException e) {
    }

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