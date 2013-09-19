package au.com.codeka;

import android.app.Activity;
import android.content.Context;

/**
 * This is a helper for working with the clipboard, since the API has changed it's a bit annoying...
 */
public class Clipboard {
    @SuppressWarnings("deprecation")
    public static void copyText(Context context, String label, String text) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB){
             android.content.ClipboardManager clipboard =  (android.content.ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE); 
             android.content.ClipData clip = android.content.ClipData.newPlainText(label, text);
                clipboard.setPrimaryClip(clip); 
        } else{
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE); 
            clipboard.setText(text);
        }
    }
}
