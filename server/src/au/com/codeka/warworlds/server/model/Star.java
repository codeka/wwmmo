package au.com.codeka.warworlds.server.model;

import au.com.codeka.common.model.BaseBuildRequest;
import au.com.codeka.common.model.BaseBuilding;
import au.com.codeka.common.model.BaseColony;
import au.com.codeka.common.model.BaseEmpirePresence;
import au.com.codeka.common.model.BaseFleet;
import au.com.codeka.common.model.BasePlanet;
import au.com.codeka.common.model.BaseStar;
import au.com.codeka.common.protobuf.Messages;

public class Star extends BaseStar {
    @Override
    protected BasePlanet createPlanet(Messages.Planet pb) {
        Planet p = new Planet();
        if (pb != null) {
            p.fromProtocolBuffer(this, pb);
        }
        return p;
    }

    @Override
    protected BaseColony createColony(Messages.Colony pb) {
        Colony c = new Colony();
        if (pb != null) {
            c.fromProtocolBuffer( pb);
        }
        return c;
    }

    @Override
    protected BaseBuilding createBuilding(Messages.Building pb) {
        Building b = new Building();
        if (pb != null) {
            b.fromProtocolBuffer( pb);
        }
        return b;
    }

    @Override
    protected BaseEmpirePresence createEmpirePresence(Messages.EmpirePresence pb) {
        EmpirePresence ep = new EmpirePresence();
        if (pb != null) {
            ep.fromProtocolBuffer( pb);
        }
        return ep;
    }

    @Override
    protected BaseFleet createFleet(Messages.Fleet pb) {
        Fleet f = new Fleet();
        if (pb != null) {
            f.fromProtocolBuffer( pb);
        }
        return f;
    }

    @Override
    protected BaseBuildRequest createBuildRequest(Messages.BuildRequest pb) {
        BuildRequest br = new BuildRequest();
        if (pb != null) {
            br.fromProtocolBuffer( pb);
        }
        return br;
    }

    @Override
    public BaseStar clone() {
        return null; //TODO
    }
}
