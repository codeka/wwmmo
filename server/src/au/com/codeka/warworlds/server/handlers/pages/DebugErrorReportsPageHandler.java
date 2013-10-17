package au.com.codeka.warworlds.server.handlers.pages;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.TreeMap;

import org.joda.time.DateTime;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlStmt;


public class DebugErrorReportsPageHandler extends BasePageHandler {
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

        String sql = "SELECT error_reports.*, empires.name AS empire_name" +
                    " FROM error_reports" +
                    " INNER JOIN empires ON error_reports.empire_id = empires.id";
        if (cursor != 0) {
            sql += " WHERE report_time < FROM_UNIXTIME("+cursor+")";
        }
        sql += " ORDER BY report_date DESC" +
               " LIMIT 50";
        // TODO: filtering
        try (SqlStmt stmt = DB.prepare(sql)) {
            ResultSet rs = stmt.select();
            ArrayList<TreeMap<String, Object>> results = new ArrayList<TreeMap<String, Object>>();
            while (rs.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("empire_name", rs.getString("empire_name"));

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

                cursor = error_report_pb.getReportTime();
            }
            data.put("error_reports", results);
        } catch(Exception e) {
            // TODO: handle errors
        }

        data.put("cursor", cursor);
        render("admin/debug/error-reports.html", data);
    }
}
