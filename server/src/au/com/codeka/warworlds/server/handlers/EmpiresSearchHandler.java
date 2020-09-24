package au.com.codeka.warworlds.server.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import au.com.codeka.common.Log;
import au.com.codeka.common.protobuf.Messages;
import au.com.codeka.warworlds.server.RequestException;
import au.com.codeka.warworlds.server.RequestHandler;
import au.com.codeka.warworlds.server.ctrl.EmpireController;
import au.com.codeka.warworlds.server.model.Alliance;
import au.com.codeka.warworlds.server.model.Empire;

public class EmpiresSearchHandler extends RequestHandler {
  private final Log log = new Log("EmpiresSearchHandler");

  @Override
  protected void get() throws RequestException {
    ArrayList<Empire> empires = new ArrayList<>();
    EmpireController ctrl = new EmpireController();

    EmpireController.Order order = EmpireController.Order.UNSPECIFIED;
    if (getRequest().getParameter("sort") != null) {
      switch (getRequest().getParameter("sort")) {
        case "rank":
          order = EmpireController.Order.RANK;
          break;
        case "signup_date_asc":
          order = EmpireController.Order.OLDEST_FIRST;
          break;
        case "signup_date_desc":
          order = EmpireController.Order.NEWEST_FIRST;
          break;
      }
    }

    String str = getRequest().getParameter("email");
    if (str != null) {
      Empire empire = ctrl.getEmpireByEmail(str);
      if (empire != null) {
        empires.add(empire);
      }
    }

    str = getRequest().getParameter("name");
    if (str != null) {
      empires.addAll(ctrl.getEmpiresByName(str, order, 50));
    }

    str = getRequest().getParameter("ids");
    if (str != null) {
      String[] parts = str.split(",");
      int[] ids = new int[parts.length];
      for (int i = 0; i < parts.length; i++) {
        ids[i] = Integer.parseInt(parts[i]);
      }

      empires.addAll(ctrl.getEmpires(ids));
    }

    str = getRequest().getParameter("minRank");
    if (str != null) {
      int minRank = Integer.parseInt(str);
      int maxRank = minRank + 5;
      if (minRank <= 3) {
        minRank = 1;
      }
      str = getRequest().getParameter("maxRank");
      if (str != null) {
        maxRank = Integer.parseInt(str);
      }
      if (maxRank <= minRank) {
        maxRank = minRank + 5;
      }

      empires.addAll(ctrl.getEmpiresByRank(minRank, maxRank));

      str = getRequest().getParameter("noLeader");
      if (minRank > 3 && (str == null || !str.equals("1"))) {
        empires.addAll(ctrl.getEmpiresByRank(1, 3));
      }
    }

    str = getRequest().getParameter("self");
    if (str != null && str.equals("1")) {
      empires.add(ctrl.getEmpire(getSession().getEmpireID()));
    }

    int myAllianceID = 0;
    if (getSessionNoError() != null) {
      myAllianceID = getSession().getAllianceID();
    }

    ArrayList<Integer> empiresToFetchTaxRateFor = new ArrayList<>();
    for (Empire empire : empires) {
      if (myAllianceID > 0 && empire.getAlliance() != null &&
          ((Alliance) empire.getAlliance()).getID() == myAllianceID) {
        empiresToFetchTaxRateFor.add(empire.getID());
      }
    }
    Map<Integer, Double> taxRates;
    if (!empiresToFetchTaxRateFor.isEmpty()) {
      taxRates = new EmpireController().getTaxCollectedPerHour(empiresToFetchTaxRateFor);
    } else {
      taxRates = new HashMap<>();
    }

    StringBuilder etag = new StringBuilder();
    Messages.Empires.Builder pb = Messages.Empires.newBuilder();
    for (Empire empire : empires) {
      Messages.Empire.Builder empire_pb = Messages.Empire.newBuilder();
      boolean isTrusted = false;
      if (getSessionNoError() != null) {
        if (getSession().isAdmin() || getSession().getEmpireID() == empire.getID()) {
          isTrusted = true;
        }
      }
      empire.toProtocolBuffer(empire_pb, isTrusted);
      if (getSessionNoError() == null
          || empire.getID() != getSession().getEmpireID() && !getSession().isAdmin()) {
        // if it's not our empire....
        empire_pb.setCash(0);
      }
      Double taxRate = taxRates.get(empire.getID());
      if (taxRate != null) {
        empire_pb.setTaxesCollectedPerHour(taxRate);
      }
      pb.addEmpires(empire_pb);
      etag.append(":");
      etag.append(empire_pb.getKey());
    }
    etag.append(":");

    setCacheTime(24, etag.toString());
    setResponseBody(pb.build());
  }
}
