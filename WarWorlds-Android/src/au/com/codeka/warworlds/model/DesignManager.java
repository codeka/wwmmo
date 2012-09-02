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
import android.graphics.Bitmap;
import android.os.AsyncTask;
import au.com.codeka.XmlIterator;

/**
 * This is the base "manager" class that manages designs for ships and buildings.
 */
public abstract class DesignManager {
    private static Logger log = LoggerFactory.getLogger(DesignManager.class);

    private SortedMap<String, Design> mDesigns;
    private SortedMap<String, Bitmap> mDesignIcons;

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
        mDesignIcons = new TreeMap<String, Bitmap>();

        new AsyncTask<Void, Void, List<Design>>() {
            @Override
            protected List<Design> doInBackground(Void... arg0) {
                Document xmldoc;
                try {
                    InputStream ins = context.getAssets().open(getDesignPath());
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    builderFactory.setValidating(false);

                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    xmldoc = builder.parse(ins);
                } catch (Exception e) {
                    log.error("Error loading "+getDesignPath(), e);
                    return null;
                }

                try {
                    return parseDesigns(xmldoc);
                } catch (ParseException e) {
                    log.error("Error loading "+getDesignPath(), e);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Design> result) {
                mDesigns = new TreeMap<String, Design>();
                if (result == null)
                    return;

                for(Design design : result) {
                    mDesigns.put(design.getID(), design);
                }
            }
        }.execute();
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
     * Gets a \c Bitmap that represents the icon for the given design.
     * 
     * If we haven't fetched the icon from the server yet, this can return \c null in which
     * case you you listen for the "designs updated" event (via \c addDesignsUpdatedListener).
     */
    public Bitmap getDesignIcon(final Design design) {
        synchronized(mDesignIcons) {
            if (mDesignIcons.containsKey(design.getID())) {
                return mDesignIcons.get(design.getID());
            }

            Sprite sprite = design.getSprite();
            if (sprite != null) {
                Bitmap icon = sprite.createIcon(150, 150);
                mDesignIcons.put(design.getID(), icon);
                return icon;
            } else {
                return null;
            }
        }
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
