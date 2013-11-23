package au.com.codeka.warworlds.server.handlers.pages;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;


public class DebugErrorReportsPageHandler extends BasePageHandler {
    private final Logger log = LoggerFactory.getLogger(DebugErrorReportsPageHandler.class);

    @Override
    protected void get() throws RequestException {
        if (!isAdmin()) {
            return;
        }
        TreeMap<String, Object> data = new TreeMap<String, Object>();

        long cursor = 0;
        if (getRequest().getParameter("cursor") != null) {
            cursor = Long.parseLong(getRequest().getParameter("cursor"));
        }
        data.put("curr_cursor", cursor);

        ArrayList<String> parameters = new ArrayList<String>();

        String sql = "SELECT error_reports.*, empires.name AS empire_name, empires.user_email AS empire_email" +
                    " FROM error_reports" +
                    " INNER JOIN empires ON error_reports.empire_id = empires.id" +
                    " WHERE 1 = 1";
        if (cursor != 0) {
            sql += " AND report_date < FROM_UNIXTIME("+cursor+")";
        }
        if (getRequest().getParameter("empire") != null && !getRequest().getParameter("empire").equals("")) {
            String empire = getRequest().getParameter("empire");
            data.put("curr_empire", empire);
            if (empire.matches("^[0-9]+$")) {
                sql += " AND error_reports.empire_id = " + Integer.parseInt(empire);
            } else if (empire.contains("@")) {
                sql += " AND empires.user_email = ?";
                parameters.add(empire);
            } else {
                sql += " AND empires.name = ?";
                parameters.add(empire);
            }
        }
        if (getRequest().getParameter("q") != null && !getRequest().getParameter("q").equals("")) {
            String q = getRequest().getParameter("q");
            data.put("curr_q", q);
            sql += " AND (" +
                     " error_reports.message LIKE ? OR " +
                     " error_reports.context LIKE ? OR " +
                     " error_reports.exception_class LIKE ?)";
            parameters.add("%"+q+"%");
            parameters.add("%"+q+"%");
            parameters.add("%"+q+"%");
        }
        sql += " ORDER BY report_date DESC" +
               " LIMIT 50";
        // TODO: filtering
        try (SqlStmt stmt = DB.prepare(sql)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i));
            }
            ResultSet rs = stmt.select();
            ArrayList<TreeMap<String, Object>> results = new ArrayList<TreeMap<String, Object>>();
            while (rs.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("empire_name", rs.getString("empire_name"));
                result.put("empire_email", rs.getString("empire_email"));

                Messages.ErrorReport error_report_pb = Messages.ErrorReport.parseFrom(rs.getBytes("data"));
                result.put("android_version", error_report_pb.getAndroidVersion());
                result.put("phone_model", error_report_pb.getPhoneModel());
                result.put("app_version", error_report_pb.getAppVersion());
                result.put("stack_trace", error_report_pb.getStackTrace());
                result.put("message", error_report_pb.getMessage());
                result.put("report_time", new DateTime(error_report_pb.getReportTime()));
                result.put("empire_id", error_report_pb.getEmpireId());
                result.put("context", error_report_pb.getContext());
                result.put("exception_class", error_report_pb.getExceptionClass());
                results.add(result);

                cursor = error_report_pb.getReportTime() / 1000;
            }
            data.put("error_reports", results);
        } catch(Exception e) {
            log.error("Exception occurred fetching error details.", e);
        }

        data.put("cursor", cursor);
        render("admin/debug/error-reports.html", data);
    }

    @Override
    protected void post() throws RequestException {
        if (!isAdmin()) {
            return;
        }

        if (getRequest().getParameter("action").equals("delete")) {
            long ms = Long.parseLong(getRequest().getParameter("ts"));
            int empireID = Integer.parseInt(getRequest().getParameter("e"));

            String sql = "DELETE FROM error_reports WHERE report_date = FROM_UNIXTIME("+(ms / 1000)+")"+
                         " AND empire_id = "+empireID;
            try (SqlStmt stmt = DB.prepare(sql)) {
                stmt.update();
            } catch (Exception e) {
                throw new RequestException(e);
            }
        }

        write("done");
    }
}
