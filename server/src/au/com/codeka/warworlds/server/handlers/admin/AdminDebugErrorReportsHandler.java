package au.com.codeka.warworlds.server.handlers.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.data.DB;
import au.com.codeka.warworlds.server.data.SqlResult;
import au.com.codeka.warworlds.server.data.SqlStmt;


public class AdminDebugErrorReportsHandler extends AdminHandler {
    private final Logger log = LoggerFactory.getLogger(AdminDebugErrorReportsHandler.class);

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
                    " LEFT JOIN empires ON error_reports.empire_id = empires.id" +
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
        if (getRequest().getParameter("source") != null && getRequest().getParameter("source").equals("client")) {
            data.put("curr_source", "client");
            sql += " AND error_reports.empire_id IS NOT NULL";
        }
        if (getRequest().getParameter("source") != null && getRequest().getParameter("source").equals("server")) {
            data.put("curr_source", "server");
            sql += " AND error_reports.empire_id IS NULL";
        }
        sql += " ORDER BY report_date DESC" +
               " LIMIT 50";
        try (SqlStmt stmt = DB.prepare(sql)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setString(i + 1, parameters.get(i));
            }
            SqlResult res = stmt.select();
            ArrayList<TreeMap<String, Object>> results = new ArrayList<TreeMap<String, Object>>();
            while (res.next()) {
                TreeMap<String, Object> result = new TreeMap<String, Object>();
                result.put("empire_name", res.getString("empire_name"));
                result.put("empire_email", res.getString("empire_email"));

                Messages.ErrorReport error_report_pb = Messages.ErrorReport.parseFrom(
                        res.getBytes("data"));
                result.put("android_version", error_report_pb.getAndroidVersion());
                result.put("phone_model", error_report_pb.getPhoneModel());
                result.put("app_version", error_report_pb.getAppVersion());
                result.put("log_buffer", error_report_pb.getLogOutput());
                result.put("stack_trace", error_report_pb.getStackTrace());
                result.put("message", error_report_pb.getMessage());
                result.put("report_time", new DateTime(error_report_pb.getReportTime()));
                result.put("empire_id", error_report_pb.getEmpireId());
                result.put("context", error_report_pb.getContext());
                result.put("exception_class", error_report_pb.getExceptionClass());
                result.put("heap_size", error_report_pb.getHeapSize());
                result.put("heap_allocated", error_report_pb.getHeapAllocated());
                result.put("heap_free", error_report_pb.getHeapFree());
                result.put("total_run_time", error_report_pb.getTotalRunTime());
                result.put("foreground_run_time", error_report_pb.getForegroundRunTime());
                result.put("server_request_qs", error_report_pb.getServerRequestQs());
                result.put("server_request_user_agent", error_report_pb.getServerRequestUserAgent());
                results.add(result);

                cursor = error_report_pb.getReportTime() / 1000;
            }
            data.put("error_reports", results);
        } catch(Exception e) {
            log.error("Exception occurred fetching error details.", e);
        }

        data.put("cursor", cursor);

        // add some data so we can display a histogram of the number of errors we're getting
        ArrayList<Integer> maxValues = new ArrayList<Integer>();
        ArrayList<TreeMap<String, Object>> histogram = new ArrayList<TreeMap<String, Object>>();
        sql = "SELECT DATE(report_date) AS date," +
                    " SUM(CASE WHEN empire_id IS NULL THEN 0 ELSE 1 END) AS num_client_errors," +
                    " SUM(CASE WHEN empire_id IS NULL THEN 1 ELSE 0 END) AS num_server_errors," +
                    " COUNT(DISTINCT empire_id) AS num_empires_reporting" +
              " FROM error_reports" +
              " GROUP BY DATE(report_date)" +
              " ORDER BY DATE(report_date) DESC" +
              " LIMIT 60";
        try (SqlStmt stmt = DB.prepare(sql)) {
            SqlResult res = stmt.select();
            while (res.next()) {
                DateTime dt = res.getDateTime(1);
                int numClientErrors = res.getInt(2);
                int numServerErrors = res.getInt(3);
                int numEmpiresReporting = res.getInt(4);

                TreeMap<String, Object> entry = new TreeMap<String, Object>();
                entry.put("year", dt.getYear());
                entry.put("month", dt.getMonthOfYear());
                entry.put("day", dt.getDayOfMonth());
                entry.put("num_client_errors", numClientErrors);
                entry.put("num_server_errors", numServerErrors);
                entry.put("num_empires_reporting", numEmpiresReporting);
                histogram.add(entry);
                maxValues.add(Math.max(numClientErrors, numServerErrors));
            }
        } catch(Exception e) {
            throw new RequestException(e);
        }
        Collections.reverse(histogram);
        data.put("error_histogram", histogram);

        // calculate the 90th percentile of maxValues, which will be the max value of the graph. We
        // don't want one or two bad days to make the whole graph unsuable for a month...
        Collections.sort(maxValues);
        int index = (int)(0.9 * maxValues.size()); // not exactly 90th percentile, but close enough..
        data.put("error_histogram_max", maxValues.get(index) + 10);

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
