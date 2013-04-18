package au.com.codeka.common.model;

import org.w3c.dom.Element;

public class ShipDesign extends Design {
    private float mSpeedParsecPerHour;
    private float mFuelCostPerParsec;
    private float mBaseAttack;
    private float mBaseDefence;

    public float getSpeedInParsecPerHour() {
        return mSpeedParsecPerHour;
    }
    public float getFuelCostPerParsec() {
        return mFuelCostPerParsec;
    }
    public float getFuelCost(float parsecs, int numShips) {
        return mFuelCostPerParsec * parsecs * numShips;
    }
    public float getBaseAttack() {
        return mBaseAttack;
    }
    public float getBaseDefence() {
        return mBaseDefence;
    }

    public static class Factory extends Design.Factory {
        public Factory(Element shipElement) {
            super(shipElement);
        }

        public ShipDesign get() {
            ShipDesign design = new ShipDesign();
            design.mDesignKind = DesignKind.SHIP;
            this.populateDesign(design);
            return design;
        }

        @Override
        protected void parseElement(Element elem, Design baseDesign) {
            ShipDesign design = (ShipDesign) baseDesign;

            if (elem.getNodeName().equals("stats")) {
                design.mSpeedParsecPerHour = Float.parseFloat(elem.getAttribute("speed"));
                design.mBaseAttack = Float.parseFloat(elem.getAttribute("baseAttack"));
                design.mBaseDefence = Float.parseFloat(elem.getAttribute("baseDefence"));
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
