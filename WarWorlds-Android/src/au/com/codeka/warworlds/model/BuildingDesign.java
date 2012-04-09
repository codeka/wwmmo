package au.com.codeka.warworlds.model;

import org.w3c.dom.Element;

import au.com.codeka.XmlIterator;

public class BuildingDesign {
    private String mID;
    private String mName;
    private String mDescription;
    private int mBuildCost;
    private int mBuildTimeSeconds;
    private String mIconUrl;

    public String getID() {
        return mID;
    }
    public String getName() {
        return mName;
    }
    public String getDescription() {
        return mDescription;
    }
    public int getBuildCost() {
        return mBuildCost;
    }
    public int getBuildTimeSeconds() {
        return mBuildTimeSeconds;
    }
    public String getIconUrl() {
        return mIconUrl;
    }

    public static class Factory {
        private Element mBuildingElement;

        public Factory(Element buildingElement) {
            mBuildingElement = buildingElement;
        }

        public BuildingDesign get() {
            BuildingDesign design = new BuildingDesign();
            design.mID = mBuildingElement.getAttribute("id");

            for(Element elem : XmlIterator.childElements(mBuildingElement)) {
                if (elem.getNodeName().equals("name")) {
                    design.mName = elem.getTextContent();
                } else if (elem.getNodeName().equals("description")) {
                    design.mDescription = elem.getTextContent();
                } else if (elem.getNodeName().equals("cost")) {
                    String value = elem.getAttribute("credits");
                    if (!value.isEmpty()) {
                        design.mBuildCost = Integer.parseInt(value);
                    }

                    value = elem.getAttribute("time");
                    if (!value.isEmpty()) {
                        double timeInHours = Double.parseDouble(value);
                        design.mBuildTimeSeconds = (int)(timeInHours * 3600);
                    }
                } else if (elem.getNodeName().equals("icon")) {
                    design.mIconUrl = elem.getTextContent();
                } else {
                    // ?? unknown element... ignore
                }
            }
            return design;
        }
    }
}
