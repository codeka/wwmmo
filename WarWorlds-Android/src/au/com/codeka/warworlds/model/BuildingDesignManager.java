package au.com.codeka.warworlds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import android.graphics.Bitmap;
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

    private List<DesignsChangedListener> mDesignsChangedListeners;
    private SortedMap<String, BuildingDesign> mDesigns;
    private SortedMap<String, Bitmap> mDesignIcons;

    /**
     * This should be called at the beginning of the game to initialize the building
     * design manager. We download the list of buildings, parse them and set up the
     * list.
     */
    public void setup() {
        mDesignIcons = new TreeMap<String, Bitmap>();
        mDesignsChangedListeners = new ArrayList<DesignsChangedListener>();

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

    public void addDesignsChangedListener(DesignsChangedListener listener) {
        mDesignsChangedListeners.add(listener);
    }
    public void removeDesignsChangedListener(DesignsChangedListener listener) {
        mDesignsChangedListeners.remove(listener);
    }
    protected void fireDesignsChanged() {
        for (DesignsChangedListener listener : mDesignsChangedListeners) {
            listener.onDesignsChanged();
        }
    }

    /**
     * Gets the collection of building designs.
     */
    public SortedMap<String, BuildingDesign> getDesigns() {
        return mDesigns;
    }

    /**
     * Gets a \c Bitmap that represents the icon for the given design.
     * 
     * If we haven't fetched the icon from the server yet, this can return \c null in which
     * case you you listen for the "designs updated" event (via \c addDesignsUpdatedListener).
     */
    public Bitmap getDesignIcon(final BuildingDesign design) {
        synchronized(mDesignIcons) {
            if (mDesignIcons.containsKey(design.getIconUrl())) {
                return mDesignIcons.get(design.getIconUrl());
            }

            // add a null value to indicate that we're still in the process of fetching it...
            mDesignIcons.put(design.getIconUrl(), null);

            // and actually make a request to fetch
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... arg0) {
                    try {
                        return ApiClient.getImage(design.getIconUrl());
                    } catch (ApiException e) {
                        log.error(String.format("Could not fetch design \"%s\" icon [%s]",
                                design.getName(), design.getIconUrl()), e);
                        return null;
                    }
                }
                @Override
                protected void onPostExecute(Bitmap result) {
                    synchronized(mDesignIcons) {
                        mDesignIcons.put(design.getIconUrl(), result);
                    }

                    fireDesignsChanged();
                }
            }.execute();

            return null;
        }
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

    public interface DesignsChangedListener {
        void onDesignsChanged();
    }
}
