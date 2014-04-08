package au.com.codeka.warworlds.game.alliance;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.R;
import au.com.codeka.warworlds.ServerGreeter;
import au.com.codeka.warworlds.ServerGreeter.ServerGreeting;
import au.com.codeka.warworlds.StyledDialog;
import au.com.codeka.warworlds.WarWorldsActivity;
import au.com.codeka.warworlds.model.Alliance;
import au.com.codeka.warworlds.model.AllianceManager;
import au.com.codeka.warworlds.model.EmpireManager;
import au.com.codeka.warworlds.model.MyEmpire;

public class AllianceChangeDetailsActivity extends BaseActivity {
    private static final Logger log = LoggerFactory.getLogger(AllianceChangeDetailsActivity.class);

    private static final int CHOOSE_IMAGE_RESULT_ID = 7406;
    private InputStream mImageStream;
    private Bitmap mNewBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.alliance_change_details);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CHOOSE_IMAGE_RESULT_ID && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try {
                mImageStream = getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();

        ServerGreeter.waitForHello(this, new ServerGreeter.HelloCompleteHandler() {
            @Override
            public void onHelloComplete(boolean success, ServerGreeting greeting) {
                if (!success) {
                    startActivity(new Intent(AllianceChangeDetailsActivity.this, WarWorldsActivity.class));
                } else {
                    fullRefresh();
                }
            }
        });
    }

    private void fullRefresh() {
        MyEmpire myEmpire = EmpireManager.i.getEmpire();
        Alliance myAlliance = (Alliance) myEmpire.getAlliance();

        final EditText newNameEdit = (EditText) findViewById(R.id.new_name);
        newNameEdit.setText(myAlliance.getName());

        final Button changeNameBtn = (Button) findViewById(R.id.change_name_btn);
        changeNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChangeNameClick();
            }
        });

        TextView txt = (TextView) findViewById(R.id.change_image_info);
        txt.setText(Html.fromHtml(txt.getText().toString()));

        ImageView currentImage = (ImageView) findViewById(R.id.current_image);
        //currentShield.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), EmpireManager.i.getEmpire()));
        ImageView currentImageSmall = (ImageView) findViewById(R.id.current_image_small);
        //currentShieldSmall.setImageBitmap(EmpireShieldManager.i.getShield(getActivity(), EmpireManager.i.getEmpire()));

        final Button chooseImageBtn = (Button) findViewById(R.id.choose_image_btn);
        chooseImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChooseImageClick();
            }
        });

        final Button changeImageBtn = (Button) findViewById(R.id.change_image_btn);
        changeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChangeImageClick();
            }
        });

        if (mImageStream != null) {
            loadImage(mImageStream);
        }
    }

    private void onChangeNameClick() {
        final EditText newNameEdit = (EditText) findViewById(R.id.new_name);

        final String newName = newNameEdit.getText().toString().trim();
        if (newName.equals(EmpireManager.i.getEmpire().getAlliance().getName())) {
            new StyledDialog.Builder(this)
                .setMessage("Please enter the new name you want before clicking 'Change'.")
                .setTitle("Rename Alliance")
                .setPositiveButton("OK", null)
                .create().show();
            return;
        }

        int allianceID = ((Alliance) EmpireManager.i.getEmpire().getAlliance()).getID();
        AllianceManager.i.requestChangeName(allianceID, "", newName);
    }

    private void onChooseImageClick() {
        // launch a new intent to find an image
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose Image"),
                CHOOSE_IMAGE_RESULT_ID);
    }

    private void onChangeImageClick() {
        if (mNewBitmap == null) {
            return;
        }

        int allianceID = ((Alliance) EmpireManager.i.getEmpire().getAlliance()).getID();

        ByteArrayOutputStream outs = new ByteArrayOutputStream();
        mNewBitmap.compress(CompressFormat.PNG, 90, outs);

        AllianceManager.i.requestChangeImage(allianceID, "", outs.toByteArray());
    }

    private void loadImage(InputStream ins) {
        if (ins.markSupported()) {
            mNewBitmap = loadImageWithMark(ins);
        } else {
            mNewBitmap = loadImageNoMark(ins);
        }

        ImageView currentImage = (ImageView) findViewById(R.id.current_image);
        currentImage.setImageBitmap(mNewBitmap);
        ImageView currentImageSmall = (ImageView) findViewById(R.id.current_image_small);
        currentImageSmall.setImageBitmap(mNewBitmap);

        // and now we can enable the 'Change' button
        ((Button) findViewById(R.id.change_image_btn)).setEnabled(true);;        
    }

    private Bitmap loadImageWithMark(InputStream ins) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        ins.mark(10 * 1024 * 1024);
        BitmapFactory.decodeStream(ins, null, opts);

        int scale = 1;
        while (opts.outWidth / scale / 2 >= 100 && opts.outHeight / scale / 2 >= 100) {
            scale *= 2;
        }
        log.info("Scale set to "+scale+" for image size "+opts.outWidth+"x"+opts.outHeight);

        opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        opts.inSampleSize = scale;
        return BitmapFactory.decodeStream(ins, null, opts);
    }

    private Bitmap loadImageNoMark(InputStream ins) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        return BitmapFactory.decodeStream(ins, null, opts);
    }
}
