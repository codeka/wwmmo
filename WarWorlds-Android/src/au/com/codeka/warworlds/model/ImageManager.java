package au.com.codeka.warworlds.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.util.LruCache;
import au.com.codeka.planetrender.Colour;
import au.com.codeka.planetrender.Image;
import au.com.codeka.planetrender.PlanetRenderer;
import au.com.codeka.planetrender.Template;
import au.com.codeka.planetrender.TemplateException;
import au.com.codeka.planetrender.Vector3;

/**
 * This is the base class for the \c StarImageManagaer and \c PlanetImageManager.
 */
@SuppressLint("NewApi") // for LruCache, we grab it from android.support.v4 anyway
public abstract class ImageManager {
    private static Logger log = LoggerFactory.getLogger(ImageManager.class);

    private Queue<QueuedGenerate> mGenerateQueue = new ArrayBlockingQueue<QueuedGenerate>(50);
    private Thread mGenerateThread;
    private Handler mHandler = new Handler();
    private List<BitmapGeneratedListener> mBitmapGeneratedListeners =
            new ArrayList<BitmapGeneratedListener>();
    private LruCache<String, Bitmap> mLoadedBitmaps = new LruCache<String, Bitmap>(100);
    private Map<String, Template> mTemplates = new HashMap<String, Template>();
    private double mPixelScale;

    /**
     * Gets the \c Bitmap for the given object at the given size.. If no image has been generated
     * yet, \c null is returned and you should wait for the ImageGenerated event.
     * 
     * To facilitate adding new images more easily, we check the asset directory each
     * time for the template. If the template would cause a different image to be
     * generated, then we don't return the cached image.
     * 
     * @param context The \c Context that you're running in.
     * @param key The key of the object (planet or star) that you want to generate an image for.
     * @param size The size, in device pixels, of the image you're after.
     * @param extra An object that the subclass passes in, which we'll pass back when getting
     *         things like the sun direction and planet size, etc.
     * @return A \c Bitmap with an image of the planet or star, or \c null if the image has
     *          not been generated yet.
     */
    protected Bitmap getBitmap(Context context, final String key, int size, Object extra) {
        final String cacheKey = String.format("%s_%d", key, size);
        Bitmap loadedBitmap = mLoadedBitmaps.get(cacheKey);
        if (loadedBitmap != null) {
            return loadedBitmap;
        }

        if (isInGenerateQueue(cacheKey)) {
            // if we've already queued up this planet/star, just give up now
            return null;
        }

        if (mPixelScale == 0) {
            mPixelScale = context.getResources().getDisplayMetrics().density;
        }

        long startTime = System.nanoTime();
        Template tmpl = getTemplate(extra);
        if (tmpl == null) {
            return null;
        }

        final File cacheFile = new File(getCachePath(context, tmpl, cacheKey));
        if (cacheFile.exists()) {
            log.debug("Loading cached image: "+cacheFile.getAbsolutePath());

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    Bitmap bmp = BitmapFactory.decodeFile(cacheFile.getAbsolutePath(), opts);

                    mLoadedBitmaps.put(cacheKey, bmp);
                    return null;
                }

                @Override
                protected void onPostExecute(Void v) {
                    fireBitmapGeneratedListeners(key, mLoadedBitmaps.get(cacheKey));
                }
            }.execute();

            return null;
        } else {
            long endTime = System.nanoTime();
            log.debug(String.format("No cached image (after %.4fms), generating: %s",
                    (endTime - startTime) / 1000000.0,
                    cacheFile.getAbsolutePath()));

            addToGenerateQueue(new QueuedGenerate(tmpl, key, size, cacheKey,
                                                  cacheFile.getAbsolutePath(), extra));
            ensureGenerateThread();

            return null;
        }
    }


    public void addBitmapGeneratedListener(BitmapGeneratedListener listener) {
        mBitmapGeneratedListeners.add(listener);
    }

    public void removeBitmapGeneratedListener(BitmapGeneratedListener listener) {
        mBitmapGeneratedListeners.remove(listener);
    }

    protected void fireBitmapGeneratedListeners(final String key, final Bitmap bmp) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // make a copy, because sometimes they'll remove themselves in their own callback
                ArrayList<BitmapGeneratedListener> listeners =
                        new ArrayList<BitmapGeneratedListener>(mBitmapGeneratedListeners);

                for (BitmapGeneratedListener listener : listeners) {
                    listener.onBitmapGenerated(key, bmp);
                }
            }
        });
    }

    /**
     * Loads the \c Template for the given \c Planet.
     */
    protected Template loadTemplate(Context context, String basePath, String key) {
        String[] fileNames = null;
        try {
            fileNames = context.getAssets().list(basePath);
        } catch(IOException e) {
            return null; // should never happen!
        }

        // TODO: better seed
        long seed = key.hashCode();
        Random rand = new Random(seed);

        String fullPath = basePath + "/";
        if (fileNames.length == 0) {
            return null;
        } else if (fileNames.length == 1) {
            fullPath += fileNames[0];
        } else {
            fullPath += fileNames[rand.nextInt(fileNames.length - 1)];
        }

        Template tmpl = mTemplates.get(fullPath);
        if (tmpl == null) {
            InputStream ins = null;
            try {
                ins = context.getAssets().open(fullPath);
                tmpl = Template.parse(ins);
            } catch (IOException e) {
                log.error("Error loading object definition: "+fullPath, e);
            } catch (TemplateException e) {
                log.error("Error parsing object definition: "+fullPath, e);
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                    }
                }
            }

            if (tmpl != null) {
                tmpl.setName(fullPath.replace(File.separatorChar, '-').replace(".xml", ""));
                mTemplates.put(fullPath, tmpl);
            }
        }

        return tmpl;
    }

    /**
     * Gets the path to the cached version of the image generated by the given
     * \c Template.
     */
    private static String getCachePath(Context context, Template tmpl, String cacheKey) {
        File cacheDir = context.getCacheDir();

        String fullPath = cacheDir.getAbsolutePath() + File.separator + "planets" + File.separator;
        fullPath += String.format("%s-%d-%s.png", tmpl.getName(),
                tmpl.getTemplateVersion(), cacheKey);

        return fullPath;
    }

    protected abstract Template getTemplate(Object extra);

    protected abstract Vector3 getSunDirection(Object extra);

    protected abstract double getPlanetSize(Object extra);

    /**
     * This is called in a background to actually generate the bitmap.
     * @param tmpl
     * @param outputPath
     */
    private void generateBitmap(QueuedGenerate item) {
        Vector3 sunDirection = getSunDirection(item.extra);

        Template.PlanetTemplate planetTemplate = (Template.PlanetTemplate) item.tmpl.getTemplate();
        planetTemplate.setSunLocation(sunDirection);

        // planet size ranges from 10 to 50, we convert that to 5..10 which is what we apply to
        // the planet renderer itself
        double size = getPlanetSize(item.extra);
        planetTemplate.setPlanetSize(size);

        // TODO: better seed
        long seed = item.key.hashCode();
        Random rand = new Random(seed);

        PlanetRenderer renderer = new PlanetRenderer(
                (Template.PlanetTemplate) item.tmpl.getTemplate(), rand);

        int imgSize = (int)(item.size * mPixelScale);

        long startTime = System.nanoTime();
        Image img = new Image(imgSize, imgSize, Colour.TRANSPARENT);
        renderer.render(img);
        Bitmap bmp = Bitmap.createBitmap(img.getArgb(), imgSize, imgSize, Bitmap.Config.ARGB_8888);
        long endTime = System.nanoTime();

        Vector3.pool.release(sunDirection);
        log.debug(String.format("Rendered %dx%d image in %.4fms.",
                imgSize, imgSize, (endTime - startTime) / 1000000.0));

        File outputFile = new File(item.outputPath);
        File outputDirectory = new File(outputFile.getParent());
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(item.outputPath);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
        } catch (FileNotFoundException e) {
            log.error("Error writing to cache file.", e);
        }

        fireBitmapGeneratedListeners(item.key, bmp);
    }

    /**
     * Makes sure the generate thread is running, and spins one up if it's not.
     */
    private void ensureGenerateThread() {
        synchronized(mGenerateQueue) {
            if (mGenerateThread == null) {
                mGenerateThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        generateThreadProc();

                        // when it finishes, make sure we set the thread to null so
                        // we know to start it up again.
                        synchronized(mGenerateQueue) {
                            mGenerateThread = null;
                        }
                    }
                });

                // make it low priority -- UI must stay responsive!
                mGenerateThread.setPriority(Thread.MIN_PRIORITY);

                mGenerateThread.start();
            }
        }
    }

    private void generateThreadProc() {
        QueuedGenerate item;
        synchronized(mGenerateQueue) {
            item = mGenerateQueue.poll();
        }

        while (item != null) {
            generateBitmap(item);

            synchronized(mGenerateQueue) {
                item = mGenerateQueue.poll();
            }
        }
    }

    private void addToGenerateQueue(QueuedGenerate item) {
        synchronized(mGenerateQueue) {
            // only add if we're not already generating this item
            if (!isInGenerateQueue(item.cacheKey)) {
                mGenerateQueue.add(item);
            }
        }
    }

    /**
     * Checks whether we've already requests the given planet be generated (no need to
     * go through all the bother a second time).
     */
    private boolean isInGenerateQueue(String cacheKey) {
        synchronized(mGenerateQueue) {
            boolean found = false;
            for (QueuedGenerate qg : mGenerateQueue) {
                if (qg.cacheKey.equals(cacheKey)) {
                    found = true;
                    break;
                }
            }

            return found;
        }
    }

    class QueuedGenerate {
        public Template tmpl;
        public String outputPath;
        public String key;
        public int size;
        public String cacheKey;
        public Object extra;

        public QueuedGenerate(Template tmpl, String key, int size, String cacheKey,
                               String outputPath, Object extra) {
            this.tmpl = tmpl;
            this.outputPath = outputPath;
            this.key = key;
            this.size = size;
            this.cacheKey = cacheKey;
            this.extra = extra;
        }
    }

    public interface BitmapGeneratedListener {
        /**
         * This is called when the bitmap for the given planet/star has been generated.
         */
        void onBitmapGenerated(String key, Bitmap bmp);
    }
}
