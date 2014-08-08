package au.com.codeka.common.model;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import au.com.codeka.common.XmlIterator;

/**
 * This is the base "design" class which both \c ShipDesign and \c BuildingDesign inherit from.
 */
public abstract class Design {
    protected String mID;
    protected String mName;
    protected String mDescription;
    protected BuildCost mBuildCost;
    protected String mSpriteName;
    protected DesignKind mDesignKind;
    protected ArrayList<Dependency> mDependencies;
    protected ArrayList<Effect> mEffects;

    public String getID() {
        return mID;
    }
    public String getDisplayName() {
        return mName;
    }
    public String getDisplayName(boolean plural) {
        return mName+(plural ? "s" : "");
    }
    public String getDescription() {
        return mDescription;
    }
    public BuildCost getBuildCost() {
        return mBuildCost;
    }
    public DesignKind getDesignKind() {
        return mDesignKind;
    }
    public String getSpriteName() {
        return mSpriteName;
    }
    public boolean canBuildMultiple() {
        return true;
    }
    public ArrayList<Dependency> getDependencies() {
        return mDependencies;
    }
    public ArrayList<Effect> getEffects() {
        return mEffects;
    }
    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> getEffects(Class<?> effectClass) {
        ArrayList<T> effects = new ArrayList<T>();
        for (Effect e : mEffects) {
            if (effectClass.isInstance(e)) {
                effects.add((T) e);
            }
        }
        return effects;
    }
    @SuppressWarnings("unchecked")
    public <T> T getEffect(Class<?> effectClass) {
        for (Effect e : mEffects) {
            if (effectClass.isInstance(e)) {
                return (T) e;
            }
        }
        return null;
    }
    public boolean hasEffect(String kind) {
        for (Effect e : mEffects) {
            if (e.getKind().equals(kind)) {
                return true;
            }
        }
        return false;
    }
    public boolean hasEffect(Class<?> effectClass) {
        for (Effect e : mEffects) {
            if (effectClass.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    public abstract ArrayList<Dependency> getDependencies(int level);

    public String getDependenciesHtml(BaseColony colony) {
        return getDependenciesHtml(colony, 0);
    }

    /**
     * Returns the dependencies of the given design a string for display to
     * the user. Dependencies that we don't meet will be coloured red.
     */
    public String getDependenciesHtml(BaseColony colony, int level) {
        String required = "Required: ";
        List<Design.Dependency> dependencies = getDependencies(level);

        if (dependencies == null || dependencies.size() == 0) {
            required += "none";
        } else {
            int n = 0;
            for (Design.Dependency dep : dependencies) {
                if (n > 0) {
                    required += ", ";
                }

                boolean dependencyMet = dep.isMet(colony);
                Design dependentDesign = BaseDesignManager.i.getDesign(DesignKind.BUILDING, dep.getDesignID());
                required += "<font color=\""+(dependencyMet ? "green" : "red")+"\">";
                required += dependentDesign.getDisplayName();
                if (dep.getLevel() > 1) {
                    required += " lvl " + dep.getLevel();
                }
                required += "</font>";
            }
        }

        return required;
    }

    public abstract static class Factory {
        protected Element mDesignElement;

        public Factory(Element buildingElement) {
            mDesignElement = buildingElement;
        }

        protected void populateDesign(Design design) {
            design.mID = mDesignElement.getAttribute("id");
            design.mDependencies = new ArrayList<Dependency>();
            design.mEffects = new ArrayList<Effect>();

            for(Element elem : XmlIterator.childElements(mDesignElement)) {
                if (elem.getNodeName().equals("name")) {
                    design.mName = elem.getTextContent();
                } else if (elem.getNodeName().equals("description")) {
                    design.mDescription = elem.getTextContent();
                } else if (elem.getNodeName().equals("cost")) {
                    design.mBuildCost = new BuildCost(elem);
                } else if (elem.getNodeName().equals("sprite")) {
                    design.mSpriteName = elem.getTextContent();
                } else if (elem.getNodeName().equals("dependencies")) {
                    design.mDependencies = Dependency.parse(elem);
                } else if (elem.getNodeName().equals("effects")) {
                    design.mEffects = Effect.parse(design.mDesignKind, elem);
                } else {
                    parseElement(elem, design);
                }
            }
        }

        protected void parseEffects(List<Effect> effects, DesignKind designKind, Element effectsElem) {
            for (Element effectElem : XmlIterator.childElements(effectsElem, "effect")) {
                Effect effect = BaseDesignManager.i.createEffect(designKind, effectElem);
                if (effect == null) {
                    continue;
                }
                effects.add(effect);
            }
        }

        protected void populateEffect(Element effectElem, Effect effect) {
            effect.mKind = effectElem.getAttribute("kind");

            String s = effectElem.getAttribute("level");
            if (s != null && s.length() > 0) {
                effect.mLevel = Integer.parseInt(s);
            } else {
                effect.mLevel = 0;
            }
        }

        /**
         * This is called when we get an "unknown" element. That means, it's a custom element
         * for this particular design.
         */
        protected void parseElement(Element elem, Design design) {
        }
    }

    public static class Dependency {
        private String mDesignID;
        private int mLevel;

        public static ArrayList<Dependency> parse(Element elem) {
            ArrayList<Dependency> dependencies = new ArrayList<Dependency>();
            for (Element requiresElem : XmlIterator.childElements(elem, "requires")) {
                dependencies.add(new Dependency(
                        requiresElem.getAttribute("building"),
                        Integer.parseInt(requiresElem.getAttribute("level"))));
            }
            return dependencies;
        }

        public Dependency(String designID, int level) {
            mDesignID = designID;
            mLevel = level;
        }

        public String getDesignID() {
            return mDesignID;
        }
        public int getLevel() {
            return mLevel;
        }

        public boolean isMet(BaseColony colony) {
            for (BaseBuilding building : colony.getBuildings()) {
                if (building.getDesignID().equals(mDesignID)) {
                    if (building.getLevel() >= mLevel) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public static class Effect {
        private String mKind;
        private int mLevel;

        public Effect() {
        }
        public Effect(String kind) {
            mKind = kind;
        }

        public static ArrayList<Effect> parse(DesignKind designKind, Element effectsElem) {
            ArrayList<Effect> effects = new ArrayList<Effect>();
            for (Element effectElem : XmlIterator.childElements(effectsElem, "effect")) {
                Effect effect = BaseDesignManager.i.createEffect(designKind, effectElem);
                if (effect == null) {
                    continue;
                }
                effects.add(effect);
            }
            return effects;
        }

        public int getLevel() {
            return mLevel;
        }
        public String getKind() {
            return mKind;
        }

        public void load(Element effectElement) {
        }
    }

    public static class BuildCost {
        private int mTimeInSeconds;
        private float mCostInMinerals;
        private int mMaxCount;

        public BuildCost(Element costElement) {
            String value = costElement.getAttribute("time");
            if (!value.equals("")) {
                double timeInHours = Double.parseDouble(value);
                mTimeInSeconds = (int)(timeInHours * 3600);
            }

            value = costElement.getAttribute("minerals");
            if (!value.equals("")) {
                mCostInMinerals = Float.parseFloat(value);
            }

            value = costElement.getAttribute("maxCount");
            if (!value.equals("")) {
                mMaxCount = Integer.parseInt(value);
            }
        }

        public int getTimeInSeconds() {
            return mTimeInSeconds;
        }
        public float getCostInMinerals() {
            return mCostInMinerals;
        }
        public int getMaxCount() {
            return mMaxCount;
        }
    }
}
