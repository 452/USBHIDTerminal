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
    private static final String MIME_JAVASCRIPT = "text/javascript";
    private static final String MIME_CSS = "text/css";
    private static final String MIME_JPEG = "image/jpeg";
    private static final String MIME_PNG = "image/png";
    private static final String MIME_SVG = "image/svg+xml";
    private static final String MIME_JSON = "application/json";
    private AssetManager assetManager;
    private WebSocketResponseHandler responseHandler;

    public WebServer(AssetManager assetManager, int port) {
        super(port);
        this.assetManager = assetManager;
        responseHandler = new WebSocketResponseHandler(webSocketFactory);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String mimeType = NanoHTTPD.MIME_HTML;
        String uri = session.getUri();
        Response response = new Response("Sample");
        if (uri.equals("/websocket")) {
            response = responseHandler.serve(session);
        } else {
            switch (uri) {
                case "/":
                    uri = "/index.html";
                    break;
            }
            if (uri.endsWith(".js")) {
                mimeType = MIME_JAVASCRIPT;
            } else if (uri.endsWith(".css")) {
                mimeType = MIME_CSS;
            } else if (uri.endsWith(".html")) {
                mimeType = MIME_HTML;
            } else if (uri.endsWith(".jpeg")) {
                mimeType = MIME_JPEG;
            } else if (uri.endsWith(".png")) {
                mimeType = MIME_PNG;
            } else if (uri.endsWith(".jpg")) {
                mimeType = MIME_JPEG;
            } else if (uri.endsWith(".svg")) {
                mimeType = MIME_SVG;
            } else if (uri.endsWith(".json")) {
                mimeType = MIME_JSON;
            }
            response.setMimeType(mimeType);
            response.setData(openPage(uri));
        }
        return response;
    }

    public void stop(){
        super.stop();
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