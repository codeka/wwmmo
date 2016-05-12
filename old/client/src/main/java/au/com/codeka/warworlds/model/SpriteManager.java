package au.com.codeka.warworlds.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import android.content.Context;
import android.graphics.Rect;
import au.com.codeka.common.Log;
import au.com.codeka.common.XmlIterator;
import au.com.codeka.warworlds.model.Sprite.SpriteFrame;

/**
 * This class manages sprites as described in the assets/sprites.xml file.
 */
public class SpriteManager {
    public static SpriteManager i = new SpriteManager();
    private static final Log log = new Log("SpriteManager");

    private TreeMap<String, Sprite> mSprites;
    private SpriteImageManager mSpriteImageManager = new SpriteImageManager();

    private SpriteManager() {
    }

    public void setup(final Context context) {
        Document xmldoc;
        try {
            xmldoc = loadXml(context.getAssets().open("sprites.xml"));
        } catch (IOException e) {
            log.error("Error loading sprites.xml", e);
            return;
        }

        if (xmldoc == null) {
            log.error("Error loading sprites.xml");
            return;
        }

        List<Sprite> sprites = parseSpritesXml(context, xmldoc);
        mSprites = new TreeMap<String, Sprite>();
        for(Sprite sprite : sprites) {
            mSprites.put(sprite.getName(), sprite);
        }
    }

    public Sprite getSprite(String name) {
        return mSprites.get(name);
    }

    public Sprite getSimpleSprite(String fileName, boolean isAsset) {
        Sprite.SpriteImage img = mSpriteImageManager.getSpriteImage(fileName, isAsset);
        Sprite sprite = new Sprite(img, "simple");
        sprite.addFrame(new SpriteFrame(new Rect(0, 0, img.getWidth(), img.getHeight())));
        return sprite;
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
            Sprite.SpriteImage img = new Sprite.SpriteImage(fileName, true);

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

                Rect r = new Rect();
                r.left = Integer.parseInt(locationStr[0]);
                r.top = Integer.parseInt(locationStr[1]);
                r.right = r.left + Integer.parseInt(sizeStr[0]);
                r.bottom = r.top + Integer.parseInt(sizeStr[1]);

                Sprite.SpriteFrame frame = new Sprite.SpriteFrame(r);
                sprite.addFrame(frame);
            } else if (elem.getTagName().equals("direction")) {
                String[] up = elem.getTextContent().split(",");

                sprite.getUp().x = Float.parseFloat(up[0]);
                sprite.getUp().y = -Float.parseFloat(up[1]);
            } else {
                log.warning("Unknown element inside <sprite>: %s", elem.getTagName());
            }
        }

        return sprite;
    }

    /**
     * This class manages SpriteImages that are backed by files on disk.
     */
    private static class SpriteImageManager {
        private TreeMap<String, Sprite.SpriteImage> mSpriteImages =
                new TreeMap<String, Sprite.SpriteImage>();

        public Sprite.SpriteImage getSpriteImage(String fileName, boolean isAsset) {
            synchronized(mSpriteImages) {
                if (mSpriteImages.containsKey(fileName)) {
                    return mSpriteImages.get(fileName);
                }
            }

            Sprite.SpriteImage img = new Sprite.SpriteImage(fileName, isAsset);
            mSpriteImages.put(fileName, img);
            return img;
        }
    }
}
