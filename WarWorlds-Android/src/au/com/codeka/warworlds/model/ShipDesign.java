package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

public class ShipDesign extends Design {
    private double mSpeedParsecPerHour;

    public double getSpeedInParsecPerHour() {
        return mSpeedParsecPerHour;
    }

    public static class Factory extends Design.Factory {
        public Factory(Element shipElement) {
            super(shipElement);
        }

        public ShipDesign get() {
            ShipDesign design = new ShipDesign();
            design.mDesignKind = Design.DesignKind.SHIP;
            this.populateDesign(design);
            return design;
        }

        @Override
        protected void parseElement(Element elem, Design baseDesign) {
            ShipDesign design = (ShipDesign) baseDesign;

            if (elem.getNodeName().equals("stats")) {
                String s = elem.getAttribute("speed");
                if (s != null) {
                    design.mSpeedParsecPerHour = Double.parseDouble(s);
                }
            }
        }
    }
}
