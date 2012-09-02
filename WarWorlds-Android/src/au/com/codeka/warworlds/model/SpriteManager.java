package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import android.content.Context;
import android.os.AsyncTask;
import au.com.codeka.XmlIterator;

/**
 * This class manages sprites as described in the assets/sprites.xml file.
 */
public class SpriteManager {
    private static Logger log = LoggerFactory.getLogger(SpriteManager.class);

    private static SpriteManager sInstance = new SpriteManager();
    public static SpriteManager getInstance() {
        return sInstance;
    }

    private TreeMap<String, Sprite> mSprites;

    private SpriteManager() {
    }

    public void setup(final Context context) {
        new AsyncTask<Void, Void, List<Sprite>>() {
            @Override
            protected List<Sprite> doInBackground(Void... arg0) {
                Document xmldoc;
                try {
                    xmldoc = loadXml(context.getAssets().open("sprites.xml"));
                } catch (IOException e) {
                    log.error("Error loading sprites.xml", e);
                    return null;
                }

                if (xmldoc == null) {
                    log.error("Error loading sprites.xml");
                    return null;
                }

                return parseSpritesXml(context, xmldoc);
            }

            @Override
            protected void onPostExecute(List<Sprite> result) {
                mSprites = new TreeMap<String, Sprite>();
                if (result == null)
                    return;

                for(Sprite sprite : result) {
                    mSprites.put(sprite.getName(), sprite);
                }
            }
        }.execute();
    }

    public Sprite getSprite(String name) {
        return mSprites.get(name);
    }

    private static Document loadXml(InputStream inp) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setValidating(false);

        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            return builder.parse(inp);
        } catch (ParserConfigurationException e) {
            return null;
        } catch (IllegalStateException e) {
            return null;
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private List<Sprite> parseSpritesXml(final Context context, Document xmldoc) {
        List<Sprite> sprites = new ArrayList<Sprite>();

        Element spritesElement = xmldoc.getDocumentElement();
        if (!spritesElement.getTagName().equals("sprites")) {
            log.error("<sprites> is expected as the root element.");
            return null;
        }

        for (Element sheetElement : XmlIterator.childElements(spritesElement, "sheet")) {
            String fileName = sheetElement.getAttribute("filename");
            Sprite.SpriteImage img = new Sprite.SpriteImage(context, fileName);

            for (Element spriteElement : XmlIterator.childElements(sheetElement, "sprite")) {
                Sprite sprite = parseSpriteXml(img, spriteElement);
                if (sprite != null) {
                    sprites.add(sprite);
                }
            }
        }

        return sprites;
    }

    private Sprite parseSpriteXml(Sprite.SpriteImage img, Element spriteElement) {
        String name = spriteElement.getAttribute("name");
        Sprite sprite = new Sprite(img, name);

        for (Element elem : XmlIterator.childElements(spriteElement)) {
            if (elem.getTagName().equals("rect")) {
                String[] locationStr = elem.getAttribute("location").split(",");
                String[] sizeStr = elem.getAttribute("size").split("x");

                sprite.getRect().left = Integer.parseInt(locationStr[0]);
                sprite.getRect().top = Integer.parseInt(locationStr[1]);
                sprite.getRect().right = sprite.getRect().left + Integer.parseInt(sizeStr[0]);
                sprite.getRect().bottom = sprite.getRect().top + Integer.parseInt(sizeStr[1]);
            } else if (elem.getTagName().equals("direction")) {
                String[] up = elem.getTextContent().split(",");

                sprite.getUp().x = Float.parseFloat(up[0]);
                sprite.getUp().y = -Float.parseFloat(up[1]);
            } else {
                log.warn("Unknown element inside <sprite>: "+elem.getTagName());
            }
        }

        return sprite;
    }
}
