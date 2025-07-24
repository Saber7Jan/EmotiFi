package com.stevenmhernandez.csi_labelling_app.server;

import android.content.Context;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.InputStream;
import java.util.Map;

public class WebDashboardServer extends NanoHTTPD {
    private Context context;
    private String latestCsi = "";

    public WebDashboardServer(Context ctx, int port) {
        super(port);
        this.context = ctx;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            if (uri.startsWith("/api/")) {
                if (uri.equals("/api/csi")) {
                    if (Method.POST.equals(method)) {
                        session.parseBody(new java.util.HashMap<>());
                        Map<String, String> parms = session.getParms();
                        latestCsi = parms.get("csi");
                        Log.d("WebDashboardServer", "Received CSI: " + latestCsi);
                        return newFixedLengthResponse(Response.Status.OK, "text/plain", "OK");
                    } else if (Method.GET.equals(method)) {
                        return newFixedLengthResponse(Response.Status.OK, "text/plain", latestCsi);
                    }
                }
                // ...existing code for other /api/ endpoints...
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
            }

            // Serve static files from assets/www
            String filePath = "www" + (uri.equals("/") ? "/index.html" : uri);
            InputStream is;
            String mime = resolveMimeType(filePath);
            try {
                is = context.getAssets().open(filePath);
            } catch (Exception e) {
                // Fallback to index.html for SPA routes if file not found
                is = context.getAssets().open("www/index.html");
                mime = "text/html";
            }
            return newChunkedResponse(Response.Status.OK, mime, is);

        } catch (Exception e) {
            Log.e("WebDashboardServer", "Error serving " + uri, e);
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }
    }

    // Improved MIME type handling
    private String resolveMimeType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".js")) return "application/javascript";
        if (filePath.endsWith(".css")) return "text/css";
        if (filePath.endsWith(".json")) return "application/json";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        if (filePath.endsWith(".ico")) return "image/x-icon";
        if (filePath.endsWith(".woff")) return "font/woff";
        if (filePath.endsWith(".woff2")) return "font/woff2";
        if (filePath.endsWith(".ttf")) return "font/ttf";
        if (filePath.endsWith(".eot")) return "application/vnd.ms-fontobject";
        // Fallback to NanoHTTPD's default
        return NanoHTTPD.getMimeTypeForFile(filePath);
    }
}
