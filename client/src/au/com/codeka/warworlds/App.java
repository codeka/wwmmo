package au.com.codeka.warworlds;

import android.app.Application;

public class App extends Application {
    public static App i;

    public App() {
        i = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // this will force the BackgroundRunner's handler to initialize on the main thread.
        try {
            Class.forName("au.com.codeka.BackgroundRunner");
        } catch (ClassNotFoundException e) {
        }
    }
}
