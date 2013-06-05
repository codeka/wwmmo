package au.com.codeka.warworlds.driver;

import java.awt.EventQueue;

public class Runner {
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new MainWindow().show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
