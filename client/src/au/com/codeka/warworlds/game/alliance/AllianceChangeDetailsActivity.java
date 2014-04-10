package au.com.codeka.warworlds.game.alliance;

import java.io.ByteArrayOutputStream;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import au.com.codeka.warworlds.BaseActivity;
import au.com.codeka.warworlds.ImagePickerHelper;
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
    private ImagePickerHelper mImagePickerHelper = new ImagePickerHelper(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.alliance_change_details);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mImagePickerHelper.onActivityResult(requestCode, resultCode, data);

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

        TextView allianceName = (TextView) findViewById(R.id.alliance_name);
        allianceName.setText(myAlliance.getName());

        TextView allianceMembers = (TextView) findViewById(R.id.alliance_num_members);
        allianceMembers.setText(String.format("Members: %d", myAlliance.getNumMembers()));

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

        loadImage();
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
        mImagePickerHelper.chooseImage();
    }

    private void onChangeImageClick() {
        Bitmap bmp = mImagePickerHelper.getImage();
        if (bmp == null) {
            return;
        }

        int allianceID = ((Alliance) EmpireManager.i.getEmpire().getAlliance()).getID();

        ByteArrayOutputStream outs = new ByteArrayOutputStream();
        bmp.compress(CompressFormat.PNG, 90, outs);

        AllianceManager.i.requestChangeImage(allianceID, "", outs.toByteArray());
    }

    private void loadImage() {
        Bitmap bmp = mImagePickerHelper.getImage();
        if (bmp == null) {
            return;
        }

        ImageView currentImage = (ImageView) findViewById(R.id.current_image);
        currentImage.setImageBitmap(bmp);
        ImageView currentImageSmall = (ImageView) findViewById(R.id.current_image_small);
        currentImageSmall.setImageBitmap(bmp);

        // and now we can enable the 'Change' button
        ((Button) findViewById(R.id.change_image_btn)).setEnabled(true);;        
    }
}
