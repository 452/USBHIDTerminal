package com.appspot.usbhidterminal.core.webserver;

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

    private static final String TAG = WebServer.class.getCanonicalName();
    private AssetManager assetManager;
    private WebSocketResponseHandler responseHandler;

    public WebServer(AssetManager assetManager) {
        super(5000);
        this.assetManager = assetManager;
        responseHandler = new WebSocketResponseHandler(webSocketFactory);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Response response = new Response("Sample");
        if (uri.equals("/websocket")) {
            response = responseHandler.serve(session);
        } else {
            switch (uri) {
                case "/":
                    uri = "index.html";
                    break;
                default:
                    response.setData(openPage(uri));
            }
        }
        return response;
    }

    public void stop(){
    }

    private InputStream openPage(String file) {
        InputStream is;
        try {
            is = open(file);
        } catch (IOException e) {
            is = openPage("/404.html");
            Log.w(TAG, e);
        }
        return is;
    }

    private InputStream open(String file) throws IOException {
        return assetManager.open("webserver"+file);
    }

    IWebSocketFactory webSocketFactory = new IWebSocketFactory() {

        @Override
        public WebSocket openWebSocket(IHTTPSession handshake) {
            return new Ws(handshake);
        }
    };

}