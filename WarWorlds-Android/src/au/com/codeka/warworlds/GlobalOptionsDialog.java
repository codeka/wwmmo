package au.com.codeka.warworlds;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.SeekBar;


public class GlobalOptionsDialog extends Dialog {
    private Context mContext;
    private SeekBar mGraphicsDetail;

    public GlobalOptionsDialog(Context context) {
        super(context);
        mContext = context;

        this.setTitle("Options");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove the title bar
        setContentView(R.layout.global_options);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = LayoutParams.MATCH_PARENT;
        params.width = LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        mGraphicsDetail = (SeekBar) findViewById(R.id.globalopts_graphics_detail_level);
        mGraphicsDetail.setMax(2);
        mGraphicsDetail.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    GlobalOptions globalOptions = new GlobalOptions(mContext);
                    globalOptions.setGraphicsDetail(GlobalOptions.GraphicsDetail.fromValue(progress));
                }
            }
        });

        refreshSettings();

        Button okButton = (Button) findViewById(R.id.ok_btn);
        okButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void refreshSettings() {
        GlobalOptions globalOptions = new GlobalOptions(mContext);
        mGraphicsDetail.setProgress(globalOptions.getGraphicsDetail().getValue());
    }
}
