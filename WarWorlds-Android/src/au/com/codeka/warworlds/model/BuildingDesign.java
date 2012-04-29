package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

public class BuildingDesign extends Design {

    public static class Factory extends Design.Factory {
        public Factory(Element shipElement) {
            super(shipElement);
        }

        public BuildingDesign get() {
            BuildingDesign design = new BuildingDesign();
            design.mDesignKind = Design.DesignKind.BUILDING;
            this.populateDesign(design);
            return design;
        }
    }
}
