package au.com.codeka.warworlds.server.handlers.pages;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.protoformat.PbFormatter;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class DebugPurchasesPageHandler extends BasePageHandler {
    @Override
    protected void get() throws RequestException {
        if (!isAdmin()) {
            return;
        }
        TreeMap<String, Object> data = new TreeMap<String, Object>();

        String sql = "SELECT * FROM purchases" +
                    " INNER JOIN empires ON empires.id = purchases.empire_id" +
                    " ORDER BY time DESC";
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            ArrayList<TreeMap<String, Object>> results = new ArrayList<TreeMap<String, Object>>();
            while (rs.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("time", new DateTime(rs.getTimestamp("time").getTime()));
                result.put("sku", rs.getString("sku"));
                result.put("empireName", rs.getString("name"));
                result.put("price", rs.getString("price"));
                result.put("skuExtra", getSkuExtra(rs));
                results.add(result);
            }
            data.put("purchases", results);
        } catch(Exception e) {
            // TODO: handle errors
        }

        render("admin/debug/purchases.html", data);
    }

    private static String getSkuExtra(ResultSet rs) throws Exception {
        String skuName = rs.getString("sku");
        if (skuName.equals("rename_empire")) {
            return getRenameEmpireSkuExtra(rs.getBytes("sku_extra"));
        } else if (skuName.equals("decorate_empire")) {
            return getDecorateEmpireSkuExtra(rs.getBytes("sku_extra"));
        }
        return null;
    }

    private static String getRenameEmpireSkuExtra(byte[] data) throws Exception {
        Messages.EmpireRenameRequest pb = Messages.EmpireRenameRequest.parseFrom(data);
        return PbFormatter.toJson(pb);
    }

    private static String getDecorateEmpireSkuExtra(byte[] data) throws Exception {
        Messages.EmpireChangeShieldRequest pb = Messages.EmpireChangeShieldRequest.parseFrom(data);
        return PbFormatter.toJson(pb);
    }
}
