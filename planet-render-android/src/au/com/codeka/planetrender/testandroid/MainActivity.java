package au.com.codeka.planetrender.testandroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import au.com.codeka.common.Image;
import au.com.codeka.planetrender.PlanetRenderer;
import au.com.codeka.planetrender.Template;
import au.com.codeka.planetrender.TemplateException;

public class MainActivity extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final ImageView planetView = (ImageView) findViewById(R.id.planet);
        final Button refreshButton = (Button) findViewById(R.id.refresh);
        final TextView statusView = (TextView) findViewById(R.id.status);
        final Spinner templateView = (Spinner) findViewById(R.id.template);
        final Spinner sizeView = (Spinner) findViewById(R.id.size);

        AssetManager assetManager = getAssets();
        ArrayList<String> templateFiles = new ArrayList<String>();
        try {
            String[] allFiles = assetManager.list("planets");
            // remove the planets.png file...
            for (int i = 0; i < allFiles.length; i++) {
                if (allFiles[i].endsWith(".png")) {
                    continue;
                }
                templateFiles.add("planets/"+allFiles[i]);
            }
            allFiles = assetManager.list("stars");
            // remove the planets.png file...
            for (int i = 0; i < allFiles.length; i++) {
                if (allFiles[i].endsWith(".png")) {
                    continue;
                }
                templateFiles.add("stars/"+allFiles[i]);
            }
        } catch (IOException e1) {
        }
        if (templateFiles.size() > 0) {
            String[] array = new String[templateFiles.size()];
            templateFiles.toArray(array);
            templateView.setAdapter(new ArrayAdapter<String>(this, R.layout.template_entry, array));
        }

        String[] sizes = new String[] { "250x250", "500x500", "1000x1000", "2000x2000" };
        sizeView.setAdapter(new ArrayAdapter<String>(this, R.layout.template_entry, sizes));

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String templateName = (String) templateView.getSelectedItem();
                try {
                    for (String fileName : getAssets().list(templateName)) {
                        templateName = templateName + "/" + fileName;
                        break;
                    }
                } catch (IOException e) {
                    statusView.setText(e.getMessage());
                    return;
                }

                long parseStartTime = System.nanoTime();
                Template tmpl;
                try {
                    tmpl = Template.parse(getAssets().open(templateName));
                } catch (TemplateException e) {
                    statusView.setText(e.getMessage());
                    return;
                } catch (IOException e) {
                    statusView.setText(e.getMessage());
                    return;
                }
                long parseEndTime = System.nanoTime();

                String sizeString = (String) sizeView.getSelectedItem();
                int size = Integer.parseInt(sizeString.split("x")[0]);

                long renderStartTime = System.nanoTime();
                PlanetRenderer renderer = new PlanetRenderer(
                        (Template.PlanetTemplate) tmpl.getTemplate(), new Random());
                Image img = new Image(size, size);
                renderer.render(img);
                long renderEndTime = System.nanoTime();

                long convertStartTime = System.nanoTime();
                Bitmap bmp = Bitmap.createBitmap(img.getArgb(), size, size, Bitmap.Config.ARGB_8888);
                planetView.setImageBitmap(bmp);
                long convertEndTime = System.nanoTime();

                statusView.setText(String.format("Parse time: %.2fms, Render time: %.2fms, Convert time: %.2fms",
                        (parseEndTime - parseStartTime) / 1000000.0,
                        (renderEndTime - renderStartTime) / 1000000.0,
                        (convertEndTime - convertStartTime) / 1000000.0));
            }
        });
    }
}