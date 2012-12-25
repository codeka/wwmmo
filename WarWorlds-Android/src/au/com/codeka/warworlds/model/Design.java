package au.com.codeka.warworlds.model;

import java.util.ArrayList;

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
    protected ArrayList<Dependency> mDependencies;
    protected ArrayList<Effect> mEffects;

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
    public ArrayList<Dependency> getDependencies() {
        return mDependencies;
    }
    public ArrayList<Effect> getEffects() {
        return mEffects;
    }
    public boolean hasEffect(String kind) {
        for (Effect e : mEffects) {
            if (e.getKind().equals(kind)) {
                return true;
            }
        }
        return false;
    }
    public boolean hasEffect(String kind, int level) {
        for (Effect e : mEffects) {
            if (e.getKind().equals(kind) && (e.getLevel() == 0 || e.getLevel() == level)) {
                return true;
            }
        }
        return false;
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
            design.mDependencies = new ArrayList<Dependency>();
            design.mEffects = new ArrayList<Effect>();

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
                } else if (elem.getNodeName().equals("dependencies")) {
                    for (Element requiresElem : XmlIterator.childElements(elem, "requires")) {
                        design.mDependencies.add(new Dependency(
                                requiresElem.getAttribute("building"),
                                Integer.parseInt(requiresElem.getAttribute("level"))));
                    }
                } else if (elem.getNodeName().equals("effects")) {
                    for (Element effectElem : XmlIterator.childElements(elem, "effect")) {
                        Effect effect = createEffect();
                        populateEffect(effectElem, effect);
                        design.mEffects.add(effect);
                    }
                } else {
                    parseElement(elem, design);
                }
            }
        }

        protected Effect createEffect() {
            return new Effect();
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
    }

    public static class Effect {
        private String mKind;
        private int mLevel;

        public int getLevel() {
            return mLevel;
        }
        public String getKind() {
            return mKind;
        }
    }
}
