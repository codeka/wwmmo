package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import au.com.codeka.common.XmlIterator;

public class ShipDesign extends Design {
    private float mSpeedParsecPerHour;
    private float mFuelCostPerParsec;
    private float mBaseAttack;
    private float mBaseDefence;
    private int mCombatPriority;
    private List<Upgrade> mUpgrades;

    public float getSpeedInParsecPerHour() {
        return mSpeedParsecPerHour;
    }
    public float getFuelCostPerParsec() {
        return mFuelCostPerParsec;
    }
    public float getFuelCost(float parsecs, float numShips) {
        return mFuelCostPerParsec * parsecs * numShips;
    }
    public float getBaseAttack() {
        return mBaseAttack;
    }
    public float getBaseDefence() {
        return mBaseDefence;
    }
    public int getCombatPriority() {
        return mCombatPriority;
    }
    public List<Upgrade> getUpgrades() {
        return mUpgrades;
    }
    public Upgrade getUpgrade(String id) {
        for (Upgrade upgrade : mUpgrades) {
            if (upgrade.getID().equals(id)) {
                return upgrade;
            }
        }
        return null;
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
            design.mUpgrades = new ArrayList<Upgrade>();

            if (elem.getNodeName().equals("stats")) {
                design.mSpeedParsecPerHour = Float.parseFloat(elem.getAttribute("speed"));
                design.mBaseAttack = Float.parseFloat(elem.getAttribute("baseAttack"));
                design.mBaseDefence = Float.parseFloat(elem.getAttribute("baseDefence"));
                if (elem.getAttribute("combatPriority") != null) {
                    design.mCombatPriority = Integer.parseInt(elem.getAttribute("combatPriority"));
                }
            }

            if (elem.getNodeName().equals("fuel")) {
                String s = elem.getAttribute("costPerParsec");
                if (s != null) {
                    design.mFuelCostPerParsec = Float.parseFloat(s);
                }
            }

            if (elem.getNodeName().equals("upgrades")) {
                for (Element upgradeElem : XmlIterator.childElements(elem, "upgrade")) {
                    Upgrade upgrade = new Upgrade(upgradeElem);
                    design.mUpgrades.add(upgrade);
                }
            }
        }
    }

    /**
     * Ship upgrades are slightly different to building upgrades in that ship upgrades give
     * specific bonuses to the ships. For example, one upgrade might increase shields, one
     * might increase firepower, etc.
     */
    public static class Upgrade {
        private String mID;
        private String mDisplayName;
        private String mDescription;
        private String mSpriteName;
        private BuildCost mBuildCost;

        public Upgrade(Element upgradeElem) {
            mID = upgradeElem.getAttribute("id");
            for(Element elem : XmlIterator.childElements(upgradeElem)) {
                if (elem.getNodeName().equals("sprite")) {
                    mSpriteName = elem.getTextContent();
                } else if (elem.getNodeName().equals("name")) {
                    mDisplayName = elem.getTextContent();
                } else if (elem.getNodeName().equals("description")) {
                    mDescription = elem.getTextContent();
                } else if (elem.getNodeName().equals("cost")) {
                    mBuildCost = new BuildCost(elem);
                }
            }
        }

        public String getID() {
            return mID;
        }
        public String getDisplayName() {
            return mDisplayName;
        }
        public String getDescription() {
            return mDescription;
        }
        public String getSpriteName() {
            return mSpriteName;
        }
        public BuildCost getBuildCost() {
            return mBuildCost;
        }
    }
}
