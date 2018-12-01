package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.TreeMap;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.common.protoformat.PbFormatter;
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
    TreeMap<String, Object> data = new TreeMap<>();

    int pageNo = 0;
    try {
      pageNo = Integer.parseInt(getRequest().getParameter("page")) - 1;
    } catch (NumberFormatException e) {
    }
    data.put("page_no", pageNo + 1);

    String sql = "SELECT COUNT(*) as total_purchases from purchases";
    long totalPurchases = 0;
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      if (res.next()) {
        totalPurchases = res.getLong("total_purchases");
      }
    } catch (Exception e) {
      // Ignore.
    }
    data.put("total_purchases", totalPurchases);
    data.put("total_pages", (totalPurchases / 100) + 1);

    sql = "SELECT * FROM purchases" +
        " INNER JOIN empires ON empires.id = purchases.empire_id" +
        " ORDER BY time DESC" +
        " LIMIT 100 OFFSET " + (100 * pageNo);
    try (SqlStmt stmt = DB.prepare(sql)) {
      SqlResult res = stmt.select();
      ArrayList<TreeMap<String, Object>> results = new ArrayList<>();
      while (res.next()) {
        TreeMap<String, Object> result = new TreeMap<>();
        result.put("time", res.getDateTime("time"));
        result.put("sku", res.getString("sku"));
        result.put("empireName", res.getString("name"));
        result.put("price", res.getString("price"));
        result.put("skuExtra", getSkuExtra(res));
        results.add(result);
      }
      data.put("purchases", results);
    } catch (Exception e) {
      // TODO: handle errors
    }

    render("admin/debug/purchases.html", data);
  }

  private static String getSkuExtra(SqlResult res) throws Exception {
    String skuName = res.getString("sku");
    if (skuName.equals("rename_empire")) {
      return getRenameEmpireSkuExtra(res.getBytes("sku_extra"));
    } else if (skuName.equals("decorate_empire")) {
      return getDecorateEmpireSkuExtra(res.getBytes("sku_extra"));
    } else if (skuName.equals("star_rename")) {
      return getStarRenameSkuExtra(res.getBytes("sku_extra"));
    }
    return "{}";
  }

  private static String getRenameEmpireSkuExtra(byte[] data) throws Exception {
    Messages.EmpireRenameRequest pb = Messages.EmpireRenameRequest.parseFrom(data);
    return PbFormatter.toJson(pb);
  }

  private static String getDecorateEmpireSkuExtra(byte[] data) throws Exception {
    Messages.EmpireChangeShieldRequest pb = Messages.EmpireChangeShieldRequest.parseFrom(data);
    return PbFormatter.toJson(pb);
  }

  private static String getStarRenameSkuExtra(byte[] data) throws Exception {
    Messages.StarRenameRequest pb = Messages.StarRenameRequest.parseFrom(data);
    return PbFormatter.toJson(pb);
  }
}
