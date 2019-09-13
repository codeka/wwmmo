package au.com.codeka.warworlds.server.handlers.admin;

import java.util.TreeMap;

import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.ctrl.EmpireController;

public class AdminEmpireBonusCashHandler extends AdminGenericHandler {
  @Override
  protected void post() throws RequestException {
    int empireID = Integer.parseInt(getRequest().getParameter("empire_id"));
    long cash = Integer.parseInt(getRequest().getParameter("cash"));

    TreeMap<String, Object> data = new TreeMap<>();
    data.put("complete", true);

    try {
      new EmpireController().depositCash(empireID, cash,
          Messages.CashAuditRecord.newBuilder()
              .setReason(Messages.CashAuditRecord.Reason.BonusAward));
      data.put("success", true);
    } catch (RequestException e) {
      data.put("success", false);
      data.put("msg", e.getMessage());
    }

    render("admin/empire/bonus-cash.html", data);
  }
}
