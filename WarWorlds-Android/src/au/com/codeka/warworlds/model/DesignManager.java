package au.com.codeka.warworlds.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;
import android.os.AsyncTask;
import au.com.codeka.XmlIterator;

/**
 * This is the base "manager" class that manages designs for ships and buildings.
 */
public abstract class DesignManager {
    private static Logger log = LoggerFactory.getLogger(DesignManager.class);

    private SortedMap<String, Design> mDesigns;

    public static DesignManager getInstance(BuildRequest.BuildKind buildKind) {
        if (buildKind == BuildRequest.BuildKind.BUILDING)
            return BuildingDesignManager.getInstance();
        else
            return ShipDesignManager.getInstance();
    }

    /**
     * This should be called at the beginning of the game to initialize the
     * design manager. We download the list of designs, parse them and set up the
     * list.
     */
    public void setup(final Context context) {
        Document xmldoc;
        try {
            InputStream ins = context.getAssets().open(getDesignPath());
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setValidating(false);

            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            xmldoc = builder.parse(ins);
        } catch (Exception e) {
            log.error("Error loading "+getDesignPath(), e);
            return;
        }

        List<Design> designs = null;
        try {
            designs = parseDesigns(xmldoc);
        } catch (ParseException e) {
            log.error("Error loading "+getDesignPath(), e);
            return;
        }

        mDesigns = new TreeMap<String, Design>();
        for(Design design : designs) {
            mDesigns.put(design.getID(), design);
        }
    }

    protected abstract String getDesignPath();
    protected abstract Design parseDesign(Element designElement);

    /**
     * Gets the collection of designs.
     */
    public SortedMap<String, Design> getDesigns() {
        return mDesigns;
    }

    /**
     * Gets the design with the given identifier.
     */
    public Design getDesign(String designID) {
        return mDesigns.get(designID);
    }

    /**
     * Parses the buildings.xml file, generating a list of \c BuildingDesign objects.
     */
    private List<Design> parseDesigns(Document xmldoc) throws ParseException {
        Element designsElement = xmldoc.getDocumentElement();
        if (!designsElement.getTagName().equals("designs")) {
            throw new ParseException("Expected root <designs> element.");
        }

        List<Design> designs = new ArrayList<Design>();
        for (Element designElement : XmlIterator.childElements(designsElement, "design")) {
            designs.add(parseDesign(designElement));
        }
        return designs;
    }

    private static class ParseException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParseException(String msg) {
            super(msg);
        }
    }

    public interface DesignsChangedListener {
        void onDesignsChanged();
    }
}
