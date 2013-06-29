package au.com.codeka.warworlds;

import android.app.Application;

public class App extends Application {
    public static App i;

    public App() {
        i = this;
    }
}
