package com.appspot.usbhidterminal.core.webserver;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;

import com.appspot.usbhidterminal.core.events.USBDataReceiveEvent;
import com.appspot.usbhidterminal.core.events.USBDataSendEvent;

import java.io.IOException;
import java.io.InputStream;

import de.greenrobot.event.EventBus;
import fi.iki.elonen.IWebSocketFactory;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.WebSocket;
import fi.iki.elonen.WebSocketFrame;
import fi.iki.elonen.WebSocketResponseHandler;

public class WebServer extends NanoHTTPD {

    private static final String tag = WebServer.class.getCanonicalName();
    private AssetManager assetManager;
    private WebSocketResponseHandler responseHandler;
    public WebSocket webSocket;

    public WebServer(AssetManager assetManager) {
        super(5000);
        this.assetManager = assetManager;
        responseHandler = new WebSocketResponseHandler(webSocketFactory);
        EventBus.getDefault().register(this);
    }

    public void onEvent(USBDataReceiveEvent event) {
        if (webSocket != null) {
            try {
                webSocket.send(event.getData());
            } catch (IOException e) {
                Log.e(tag, "Event receive error", e);
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Response response = new Response("Sample");
        switch (uri){
            case "/":
                uri = "index.html";
                break;
            case "/websocket":
                response = responseHandler.serve(session);
                break;
            default:
                response.setData(openPage(uri));
        }
        return response;
    }

    public void stop(){
        EventBus.getDefault().unregister(this);
    }

    private InputStream openPage(String file) {
        InputStream is;
        try {
            is = open(file);
        } catch (IOException e) {
            is = openPage("404.html");
            Log.w(tag, e);
        }
        return is;
    }

    private InputStream open(String file) throws IOException {
        return assetManager.open("webserver"+file);
    }


    public class Ws extends WebSocket {

        IHTTPSession httpSession;

        public Ws(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
            this.httpSession = handshakeRequest;
            webSocket = this;
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
        }

        @Override
        protected void onException(IOException e) {
            Log.w(tag, "WebSocket exception" , e);
        }


    }

    IWebSocketFactory webSocketFactory = new IWebSocketFactory() {

        @Override
        public WebSocket openWebSocket(IHTTPSession handshake) {
            return new Ws(handshake);
        }
    };

}