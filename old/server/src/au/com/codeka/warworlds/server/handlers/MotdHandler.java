package au.com.codeka.warworlds.server.handlers;

import java.io.BufferedReader;
import java.io.IOException;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class MotdHandler extends RequestHandler {
    @Override
    protected void get() throws RequestException {
        String motd = "";
        try (SqlStmt stmt = DB.prepare("SELECT motd FROM motd")) {
            motd = stmt.selectFirstValue(String.class);
        } catch (Exception e) {
            throw new RequestException(e);
        }

        try {
            getResponse().setContentType("text/html");
            getResponse().getWriter().write(motd);
        } catch(Exception e) {
            throw new RequestException(e);
        }
    }

    @Override
    protected void post() throws RequestException {
        String motd;
        try {
            BufferedReader reader = getRequest().getReader();

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            motd = sb.toString();
        } catch (IOException e) {
            throw new RequestException(e);
        }

        try (SqlStmt stmt = DB.prepare("UPDATE motd SET motd = ?")) {
            stmt.setString(1, motd);
            stmt.update();
        } catch (Exception e) {
            throw new RequestException(e);
        }
    }
}
