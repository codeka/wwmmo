package au.com.codeka.common.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import au.com.codeka.common.Log;
import au.com.codeka.common.XmlIterator;

/**
 * This is the base "manager" class that manages designs for ships and buildings.
 */
public abstract class BaseDesignManager {
    private static final Log log = new Log("BaseDesignManager");
    public static BaseDesignManager i;

    private SortedMap<DesignKind, SortedMap<String, Design>> mDesigns;

    /**
     * This should be called at the beginning of the game to initialize the
     * design manager. We download the list of designs, parse them and set up the
     * list.
     */
    public void setup() {
        mDesigns = new TreeMap<DesignKind, SortedMap<String, Design>>();
        for (DesignKind designKind : DesignKind.values()) {
            Document xmldoc;
            try {
                InputStream ins = open(designKind);
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                builderFactory.setValidating(false);

                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                xmldoc = builder.parse(ins);
            } catch (Exception e) {
                log.error("Error loading %s", designKind, e);
                return;
            }

            List<Design> designs = null;
            try {
                designs = parseDesigns(designKind, xmldoc);
            } catch (ParseException e) {
                log.error("Error loading %s", designKind, e);
                return;
            }

            TreeMap<String, Design> designMap = new TreeMap<String, Design>();
            for(Design design : designs) {
                designMap.put(design.getID(), design);
            }
            mDesigns.put(designKind, designMap);
        }
    }

    protected abstract InputStream open(DesignKind designKind) throws IOException;
    public abstract Design.Effect createEffect(DesignKind designKind, Element effectElement);

    /**
     * Gets the collection of designs.
     */
    public SortedMap<String, Design> getDesigns(DesignKind kind) {
        return mDesigns.get(kind);
    }

    /**
     * Gets the design with the given identifier.
     */
    public Design getDesign(DesignKind kind, String designID) {
        return mDesigns.get(kind).get(designID);
    }

    /**
     * Parses the buildings.xml file, generating a list of \c BuildingDesign objects.
     */
    private List<Design> parseDesigns(DesignKind kind, Document xmldoc) throws ParseException {
        Element designsElement = xmldoc.getDocumentElement();
        if (!designsElement.getTagName().equals("designs")) {
            throw new ParseException("Expected root <designs> element.");
        }

        List<Design> designs = new ArrayList<Design>();
        for (Element designElement : XmlIterator.childElements(designsElement, "design")) {
            designs.add(parseDesign(kind, designElement));
        }
        return designs;
    }

    private Design parseDesign(DesignKind kind, Element designElement) {
        if (kind == DesignKind.BUILDING) {
            return new BuildingDesign.Factory(designElement).get();
        } else if (kind == DesignKind.SHIP) {
            return new ShipDesign.Factory(designElement).get();
        }

        return null;
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
