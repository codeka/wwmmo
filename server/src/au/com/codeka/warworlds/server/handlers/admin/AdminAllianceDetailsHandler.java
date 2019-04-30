package au.com.codeka.warworlds.server.handlers.admin;

import java.sql.SQLException;
import java.util.ArrayList;

import com.google.gson.Gson;

import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;

/**
 * This handler is used by the "alliance search" page for display details about an alliance. We use
 * the default handlers as much as possible, but some things (such as audit history) is not
 * available to non-Admin players, so we have to do it here.
 */
public class AdminAllianceDetailsHandler extends AdminHandler {
  @Override
  protected void get() throws RequestException {
    if (getRequest().getParameter("section").equals("audit")) {
      getAudit();
    } else if (getRequest().getParameter("section").equals("requests")) {
      getRequests();
    } else {
      throw new RequestException(501);
    }
  }

  private void getAudit() throws RequestException {
    String sql = "SELECT alliance_request_id, empire_id, date, amount_before, amount_after," +
        "   message" +
        " FROM alliance_bank_balance_audit" +
        " INNER JOIN alliance_requests" +
        "   ON alliance_bank_balance_audit.alliance_request_id = alliance_requests.id" +
        " WHERE alliance_requests.alliance_id = ?" +
        " ORDER BY date DESC";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setInt(1, Integer.parseInt(getUrlParameter("allianceid")));
      SqlResult result = stmt.select();

      ArrayList<BankBalanceAudit> audit = new ArrayList<>();
      while (result.next()) {
        audit.add(new BankBalanceAudit(result));
      }
      Gson gson = new Gson();
      writeJson(gson.toJsonTree(audit));
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  private void getRequests() throws RequestException {
    String sql = "SELECT id, alliance_id, request_empire_id, request_date, request_type, message," +
        "   state, votes, target_empire_id, amount, png_image, new_name, new_description" +
        " FROM alliance_requests" +
        " WHERE alliance_id = ?" +
        " ORDER BY request_date DESC";
    try (SqlStmt stmt = DB.prepare(sql)) {
      stmt.setInt(1, Integer.parseInt(getUrlParameter("allianceid")));
      SqlResult result = stmt.select();

      ArrayList<RequestAudit> audit = new ArrayList<>();
      while (result.next()) {
        audit.add(new RequestAudit(result));
      }
      Gson gson = new Gson();
      writeJson(gson.toJsonTree(audit));
    } catch (Exception e) {
      throw new RequestException(e);
    }
  }

  @SuppressWarnings("unused") // the fields ARE used, by Gson, reflectively
  private static class BankBalanceAudit {
    public long alliance_request_id;
    public long empire_id;
    public long date;
    public double amount_before;
    public double amount_after;
    public String message;

    public BankBalanceAudit(SqlResult result) throws SQLException {
      alliance_request_id = result.getLong("alliance_request_id");
      empire_id = result.getLong("empire_id");
      date = result.getDateTime("date").getMillis();
      amount_before = result.getDouble("amount_before");
      amount_after = result.getDouble("amount_after");
      message = result.getString("message");
    }
  }

  @SuppressWarnings("unused") // the fields ARE used, by Gson, reflectively
  private static class RequestAudit {
    public long id;
    public long alliance_id;
    public long request_empire_id;
    public long request_date;
    public int request_type;
    public String message;
    public int state;
    public int votes;
    public Long target_empire_id;
    public Double amount;
    public byte[] png_image;
    public String new_name;
    public String new_description;


    public RequestAudit(SqlResult result) throws SQLException {
      id = result.getLong("id");
      alliance_id = result.getLong("alliance_id");
      request_empire_id = result.getLong("request_empire_id");
      request_date = result.getDateTime("request_date").getMillis();
      request_type = result.getInt("request_type");
      message = result.getString("message");
      state = result.getInt("state");
      votes = result.getInt("votes");
      target_empire_id = result.getLong("target_empire_id");
      amount = result.getDouble("amount");
      png_image = result.getBytes("png_image");
      new_name = result.getString("new_name");
      new_description = result.getString("new_description");
    }
  }
}
