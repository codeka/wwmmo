package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

public class ShipDesign extends Design {
    private float mSpeedParsecPerHour;
    private float mFuelCostPerParsec;

    public float getSpeedInParsecPerHour() {
        return mSpeedParsecPerHour;
    }
    public float getFuelCostPerParsec() {
        return mFuelCostPerParsec;
    }
    public float getFuelCost(float parsecs, int numShips) {
        return mFuelCostPerParsec * parsecs * numShips;
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
                    design.mSpeedParsecPerHour = Float.parseFloat(s);
                }
            }

            if (elem.getNodeName().equals("fuel")) {
                String s = elem.getAttribute("costPerParsec");
                if (s != null) {
                    design.mFuelCostPerParsec = Float.parseFloat(s);
                }
            }
        }
    }
}
