package au.com.codeka.warworlds.server.handlers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import au.com.codeka.common.Log;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;

public class StaticFileHandler extends RequestHandler {
    private final Log log = new Log("AdminGenericHandler");

    private static String sBasePath;
    static {
        sBasePath = System.getProperty("au.com.codeka.warworlds.server.basePath");
        if (sBasePath == null) {
            sBasePath = StaticFileHandler.class.getClassLoader().getResource("").getPath();
        }
        sBasePath += "../data/static/";
    }

    @Override
    protected void get() throws RequestException {
        String path = getExtraOption() + getUrlParameter("path");

        String contentType = "text/plain";
        if (path.endsWith(".css")) {
            contentType = "text/css";
        } else if (path.endsWith(".js")) {
            contentType = "text/javascript";
        } else if (path.endsWith(".png")) {
            contentType = "image/png";
        } else if (path.endsWith(".ico")) {
            contentType = "image/x-icon";
        } else {
            contentType = "text/plain";
        }
        getResponse().setContentType(contentType);
        getResponse().setHeader("Content-Type", contentType);

        try {
            OutputStream outs = getResponse().getOutputStream();
            InputStream ins = new FileInputStream(sBasePath + path);

            byte[] buffer = new byte[1024];
            int bytes;
            while ((bytes = ins.read(buffer, 0, 1024)) > 0) {
                outs.write(buffer, 0, bytes);
            }

            ins.close();
        } catch (IOException e) {
            log.error("Error sending static file!", e);
        }
    }
}
