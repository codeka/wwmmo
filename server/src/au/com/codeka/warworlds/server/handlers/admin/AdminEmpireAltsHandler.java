package au.com.codeka.warworlds.server.handlers.admin;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.protoformat.PbFormatter;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminEmpireAltsHandler extends AdminHandler {
    private static final MultipartConfigElement MULTI_PART_CONFIG =
            new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

    @Override
    protected void get() throws RequestException {
        if (!isAdmin()) {
            return;
        }
        TreeMap<String, Object> data = new TreeMap<String, Object>();

        if (getRequest().getParameter("empire_id") != null) {
            int empireID = Integer.parseInt(getRequest().getParameter("empire_id"));
            data.put("empire_id", empireID);

            String sql = "SELECT alt_blob FROM empire_alts WHERE empire_id = ?";
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.setInt(1, empireID);
                ResultSet rs = stmt.select();
                if (rs.next()) {
                    byte[] blob = rs.getBytes(1);
                    Messages.EmpireAltAccounts pb = Messages.EmpireAltAccounts.parseFrom(blob);
                    data.put("alts", PbFormatter.toJson(pb));
                }
            } catch(Exception e) {
                // TODO: handle errors
            }
        }

        render("admin/empire/alts.html", data);
    }
}
