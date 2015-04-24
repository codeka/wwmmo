package au.com.codeka.warworlds.server.handlers.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.wire.Message;
import com.squareup.wire.WireTypeAdapterFactory;

import java.util.ArrayList;
import java.util.TreeMap;

import au.com.codeka.common.Wire;
import au.com.codeka.common.protobuf.EmpireChangeShieldRequest;
import au.com.codeka.common.protobuf.EmpireRenameRequest;
import au.com.codeka.common.protobuf.StarRenameRequest;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

public class AdminDebugPurchasesHandler extends AdminHandler {
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
            SqlResult res = stmt.select();
            ArrayList<TreeMap<String, Object>> results = new ArrayList<TreeMap<String, Object>>();
            while (res.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("time", res.getDateTime("time"));
                result.put("sku", res.getString("sku"));
                result.put("empireName", res.getString("name"));
                result.put("price", res.getString("price"));
                result.put("skuExtra", getSkuExtra(res));
                results.add(result);
            }
            data.put("purchases", results);
        } catch(Exception e) {
            // TODO: handle errors
        }

        render("admin/debug/purchases.html", data);
    }

    private static String getSkuExtra(SqlResult res) throws Exception {
        String skuName = res.getString("sku");
        if (skuName.equals("rename_empire")) {
            return toJson(res.getBytes("sku_extra"), EmpireRenameRequest.class);
        } else if (skuName.equals("decorate_empire")) {
            return toJson(res.getBytes("sku_extra"), EmpireChangeShieldRequest.class);
        } else if (skuName.equals("star_rename")) {
            return toJson(res.getBytes("sku_extra"), StarRenameRequest.class);
        }
        return "{}";
    }

    private static String toJson(byte[] data, Class<? extends Message> clazz) throws Exception {
        Gson gson = new GsonBuilder()
            .registerTypeAdapterFactory(new WireTypeAdapterFactory(Wire.i))
            .setPrettyPrinting()
            .create();
        return gson.toJson(Wire.i.parseFrom(data, clazz));
    }
}
