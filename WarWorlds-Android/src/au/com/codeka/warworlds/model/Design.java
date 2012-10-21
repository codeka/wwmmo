package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

import au.com.codeka.XmlIterator;

/**
 * This is the base "design" class which both \c ShipDesign and \c BuildingDesign inherit from.
 */
public class Design {
    protected String mID;
    protected String mName;
    protected String mDescription;
    protected int mBuildTimeSeconds;
    protected float mBuildCostMinerals;
    protected String mSpriteName;
    protected DesignKind mDesignKind;

    public String getID() {
        return mID;
    }
    public String getDisplayName() {
        return mName;
    }
    public String getDescription() {
        return mDescription;
    }
    public int getBuildTimeSeconds() {
        return mBuildTimeSeconds;
    }
    public float getBuildCostMinerals() {
        return mBuildCostMinerals;
    }
    public DesignKind getDesignKind() {
        return mDesignKind;
    }
    public Sprite getSprite() {
        return SpriteManager.getInstance().getSprite(mSpriteName);
    }
    public boolean canBuildMultiple() {
        return true;
    }

    /**
     * The values here should be kept in sync with with the BuildRequest.BUILD_KIND protocol buffer.
     */
    public enum DesignKind {
        BUILDING(1),
        SHIP(2);

        private int mValue;
        DesignKind(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static DesignKind fromInt(int value) {
            for (DesignKind dk : DesignKind.values()) {
                if (dk.getValue() == value) {
                    return dk;
                }
            }

            return DesignKind.BUILDING; //??
        }
    }

    public abstract static class Factory {
        protected Element mDesignElement;

        public Factory(Element buildingElement) {
            mDesignElement = buildingElement;
        }

        protected void populateDesign(Design design) {
            design.mID = mDesignElement.getAttribute("id");

            for(Element elem : XmlIterator.childElements(mDesignElement)) {
                if (elem.getNodeName().equals("name")) {
                    design.mName = elem.getTextContent();
                } else if (elem.getNodeName().equals("description")) {
                    design.mDescription = elem.getTextContent();
                } else if (elem.getNodeName().equals("cost")) {
                    String value = elem.getAttribute("time");
                    if (!value.equals("")) {
                        double timeInHours = Double.parseDouble(value);
                        design.mBuildTimeSeconds = (int)(timeInHours * 3600);
                    }

                    value = elem.getAttribute("minerals");
                    if (!value.equals("")) {
                        design.mBuildCostMinerals = Float.parseFloat(value);
                    }
                } else if (elem.getNodeName().equals("sprite")) {
                    design.mSpriteName = elem.getTextContent();
                } else {
                    parseElement(elem, design);
                }
            }
        }

        /**
         * This is called when we get an "unknown" element. That means, it's a custom element
         * for this particular design.
         */
        protected void parseElement(Element elem, Design design) {
        }
    }

}
