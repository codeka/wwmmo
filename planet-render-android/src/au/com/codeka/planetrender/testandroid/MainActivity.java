package au.com.codeka.planetrender.testandroid;

import java.io.IOException;
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

        AssetManager assetManager = getAssets();
        String[] templateFiles = null;
        try {
            templateFiles = assetManager.list("planets");
        } catch (IOException e1) {
        }
        if (templateFiles != null) {
            templateView.setAdapter(new ArrayAdapter<String>(this, R.layout.template_entry, templateFiles));
        }

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String templateName = (String) templateView.getSelectedItem();
                try {
                    for (String fileName : getAssets().list("planets/"+templateName)) {
                        templateName = "planets/" + templateName + "/" + fileName;
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

                long renderStartTime = System.nanoTime();
                PlanetRenderer renderer = new PlanetRenderer(
                        (Template.PlanetTemplate) tmpl.getTemplate(), new Random());
                Image img = new Image(200, 200);
                renderer.render(img);
                long renderEndTime = System.nanoTime();

                long convertStartTime = System.nanoTime();
                Bitmap bmp = Bitmap.createBitmap(img.getArgb(), 200, 200, Bitmap.Config.ARGB_8888);
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