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

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminEmpireShieldsHandler extends AdminHandler {
    private static final MultipartConfigElement MULTI_PART_CONFIG =
            new MultipartConfigElement(System.getProperty("java.io.tmpdir"));

    @Override
    protected void get() throws RequestException {
        if (!isAdmin()) {
            return;
        }
        TreeMap<String, Object> data = new TreeMap<String, Object>();

        String sql = "SELECT empire_shields.id, empires.id, empire_shields.create_date," +
                           " empire_shields.rejected, empires.name, empires.user_email" +
                    " FROM empire_shields" +
                    " INNER JOIN empires ON empires.id = empire_shields.empire_id" +
                    " ORDER BY create_date DESC";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            ArrayList<TreeMap<String, Object>> results = new ArrayList<TreeMap<String, Object>>();
            while (rs.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("shield_id", rs.getInt(1));
                result.put("empire_id", rs.getInt(2));
                result.put("create_date", new DateTime(rs.getTimestamp(3).getTime()));
                result.put("rejected", rs.getBoolean(4));
                result.put("empire_name", rs.getString(5));
                result.put("user_email", rs.getString(6));
                results.add(result);
            }
            data.put("shields", results);
        } catch(Exception e) {
            // TODO: handle errors
        }

        render("admin/empire/shields.html", data);
    }

    @Override
    protected void post() throws RequestException {
        if (!isAdmin()) {
            return;
        }

        // set our multi-part config
        getRequest().setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);

        int empireID;
        try {
            String s = IOUtils.toString(getRequest().getPart("empire_id").getInputStream());
            empireID = Integer.parseInt(s);
        } catch(IOException | ServletException e) {
            throw new RequestException(e);
        }

        byte[] pngImage;
        try {
            Part part = getRequest().getPart("shield_file");
            InputStream shield = part.getInputStream();
            pngImage = IOUtils.toByteArray(shield);
        } catch (IOException | ServletException e) {
            throw new RequestException(e);
        }

        // make sure pngImage actually IS png (and not, say jpg)
        try {
            BufferedImage shieldImage = Imaging.getBufferedImage(pngImage);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(shieldImage, "png", baos);
            pngImage = baos.toByteArray();
        } catch (ImageReadException | IOException e) {
            throw new RequestException(e);
        }

        new EmpireController().changeEmpireShield(empireID, pngImage);
        redirect(String.format("/realms/%s/admin/debug/shields", getRealm()));
    }
}
