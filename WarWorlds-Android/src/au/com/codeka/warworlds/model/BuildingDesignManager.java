package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import android.os.AsyncTask;
import au.com.codeka.XmlIterator;
import au.com.codeka.warworlds.api.ApiClient;
import au.com.codeka.warworlds.api.ApiException;

public class BuildingDesignManager {
    private static Logger log = LoggerFactory.getLogger(BuildingDesignManager.class);

    private static BuildingDesignManager sInstance = new BuildingDesignManager();
    public static BuildingDesignManager getInstance() {
        return sInstance;
    }

    private SortedMap<String, BuildingDesign> mDesigns;

    /**
     * This should be called at the beginning of the game to initialize the building
     * design manager. We download the list of buildings, parse them and set up the
     * list.
     */
    public void setup() {
        new AsyncTask<Void, Void, List<BuildingDesign>>() {
            @Override
            protected List<BuildingDesign> doInBackground(Void... arg0) {
                Document xmldoc;
                try {
                    xmldoc = ApiClient.getXml("/data/buildings.xml");
                } catch (ApiException e) {
                    log.error("Error loading buildings.xml", e);
                    return null;
                }

                try {
                    return parseBuildings(xmldoc);
                } catch (ParseException e) {
                    log.error("Error loading buildings.xml", e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<BuildingDesign> result) {
                mDesigns = new TreeMap<String, BuildingDesign>();
                for(BuildingDesign design : result) {
                    mDesigns.put(design.getID(), design);
                }
            }
        }.execute();
    }

    /**
     * Gets the collection of building designs.
     */
    public SortedMap<String, BuildingDesign> getDesigns() {
        return mDesigns;
    }

    /**
     * Parses the buildings.xml file, generating a list of \c BuildingDesign objects.
     */
    private static List<BuildingDesign> parseBuildings(Document xmldoc) throws ParseException {
        Element buildingsElement = xmldoc.getDocumentElement();
        if (!buildingsElement.getTagName().equals("buildings")) {
            throw new ParseException("Expected root <buildings> element.");
        }

        List<BuildingDesign> designs = new ArrayList<BuildingDesign>();
        for (Element buildingElement : XmlIterator.childElements(buildingsElement, "building")) {
            designs.add(new BuildingDesign.Factory(buildingElement).get());
        }
        return designs;
    }

    private static class ParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParseException(String msg) {
            super(msg);
        }
    }
}
