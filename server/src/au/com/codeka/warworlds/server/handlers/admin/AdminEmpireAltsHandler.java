package au.com.codeka.warworlds.server.handlers.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.WireTypeAdapterFactory;

import java.util.TreeMap;

import au.com.codeka.common.Wire;
import au.com.codeka.common.protobuf.EmpireAltAccounts;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminEmpireAltsHandler extends AdminHandler {
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
                SqlResult res = stmt.select();
                if (res.next()) {
                    byte[] blob = res.getBytes(1);
                    EmpireAltAccounts pb = Wire.i.parseFrom(blob, EmpireAltAccounts.class);
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapterFactory(new WireTypeAdapterFactory(Wire.i))
                            .setPrettyPrinting()
                            .create();
                    data.put("alts", gson.toJson(pb));
                }
            } catch(Exception e) {
                // TODO: handle errors
            }
        }

        render("admin/empire/alts.html", data);
    }
}
