package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

public class BuildingDesign extends Design {
    private int mMaxPerColony;

    public int getMaxPerColony() {
        return mMaxPerColony;
    }

    @Override
    public boolean canBuildMultiple() {
        return false;
    }

    public static class Factory extends Design.Factory {
        public Factory(Element designElement) {
            super(designElement);
        }

        public BuildingDesign get() {
            BuildingDesign design = new BuildingDesign();
            design.mDesignKind = Design.DesignKind.BUILDING;
            design.mMaxPerColony = 0;

            this.populateDesign(design);
            return design;
        }

        @Override
        public void parseElement(Element elem, Design design) {
            BuildingDesign bd = (BuildingDesign) design;

            if (elem.getTagName().equals("limits")) {
                String s = elem.getAttribute("maxPerColony");
                if (s != null) {
                    bd.mMaxPerColony = Integer.parseInt(s);
                }
            }
        }
    }
}
