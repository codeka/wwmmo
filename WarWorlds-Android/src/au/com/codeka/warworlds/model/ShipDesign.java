package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

public class ShipDesign extends Design {
    public static class Factory extends Design.Factory {
        public Factory(Element shipElement) {
            super(shipElement);
        }

        public ShipDesign get() {
            ShipDesign design = new ShipDesign();
            this.populateDesign(design);
            return design;
        }
    }
}
