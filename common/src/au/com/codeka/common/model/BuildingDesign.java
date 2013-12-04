package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import au.com.codeka.common.XmlIterator;

public class BuildingDesign extends Design {
    private int mMaxPerColony;
    private int mMaxPerEmpire;
    private ArrayList<Upgrade> mUpgrades;

    public int getMaxPerColony() {
        return mMaxPerColony;
    }
    public int getMaxPerEmpire() {
        return mMaxPerEmpire;
    }

    public List<Upgrade> getUpgrades() {
        return mUpgrades;
    }

    public List<Effect> getEffects(int level) {
        if (level <= 1) {
            return mEffects;
        } else {
            if (level - 2 >= mUpgrades.size()) {
                return mUpgrades.get(mUpgrades.size() - 1) .getEffects();
            }
            return mUpgrades.get(level - 2).getEffects();
        }
    }

    @Override
    public ArrayList<Dependency> getDependencies(int level) {
        if (level <= 1) {
            return mDependencies;
        } else {
            return mUpgrades.get(level - 2).getDependencies();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> getEffects(int level, Class<T> effectClass) {
        List<Effect> allEffects = getEffects(level);

        ArrayList<T> effects = new ArrayList<T>();
        for (Effect e : allEffects) {
            if (effectClass.isInstance(e)) {
                effects.add((T) e);
            }
        }
        return effects;
    }
    public boolean hasEffect(int level, String kind) {
        List<Effect> allEffects = getEffects(level);

        for (Effect e : allEffects) {
            if (e.getKind().equals(kind)) {
                return true;
            }
        }
        return false;
    }
    public boolean hasEffect(int level, Class<?> effectClass) {
        List<Effect> allEffects = getEffects(level);

        for (Effect e : allEffects) {
            if (effectClass.isInstance(e)) {
                return true;
            }
        }
        return false;
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
            design.mDesignKind = DesignKind.BUILDING;
            design.mMaxPerColony = 0;
            design.mMaxPerEmpire = 0;
            design.mUpgrades = new ArrayList<Upgrade>();

            this.populateDesign(design);
            return design;
        }

        @Override
        public void parseElement(Element elem, Design design) {
            BuildingDesign bd = (BuildingDesign) design;

            if (elem.getTagName().equals("limits")) {
                String s = elem.getAttribute("maxPerColony");
                if (s != null && s.length() > 0) {
                    bd.mMaxPerColony = Integer.parseInt(s);
                }
                s = elem.getAttribute("maxPerEmpire");
                if (s != null && s.length() > 0) {
                    bd.mMaxPerEmpire = Integer.parseInt(s);
                }
            }

            if (elem.getTagName().equals("upgrades")) {
                for (Element upgradeElem : XmlIterator.childElements(elem, "upgrade")) {
                    Upgrade upgrade = new Upgrade(upgradeElem);
                    bd.mUpgrades.add(upgrade);
                }
            }
        }
    }

    /**
     * An upgrade is used to increase the capacity or effects of a building. We don't care so
     * much about the actual effects here (that's handled on the server) but we want to describe
     * the cost and so on.
     */
    public static class Upgrade {
        private BuildCost mBuildCost;
        protected ArrayList<Dependency> mDependencies;
        protected ArrayList<Effect> mEffects;

        public Upgrade(Element upgradeElem) {
            for(Element elem : XmlIterator.childElements(upgradeElem)) {
                if (elem.getNodeName().equals("cost")) {
                    mBuildCost = new BuildCost(elem);
                } else if (elem.getNodeName().equals("dependencies")) {
                    mDependencies = Dependency.parse(elem);
                } else if (elem.getNodeName().equals("effects")) {
                    mEffects = Effect.parse(DesignKind.BUILDING, elem);
                }
            }

            if (mDependencies == null) {
                mDependencies = new ArrayList<Dependency>();
            }
            if (mEffects == null) {
                mEffects = new ArrayList<Effect>();
            }
        }

        public BuildCost getBuildCost() {
            return mBuildCost;
        }
        public ArrayList<Dependency> getDependencies() {
            return mDependencies;
        }
        public List<Effect> getEffects() {
            return mEffects;
        }
    }
}
