package au.com.codeka.controlfieldtest;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class MainWindow {
    private JFrame mFrame;

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow();
                    window.mFrame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public MainWindow() {
        initialize();
    }

    private void initialize() {
        mFrame = new JFrame();
        mFrame.setTitle("Control Field Test");
        mFrame.setBounds(100, 100, 1200, 700);
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.getContentPane().add(new AppContent());
    }
  
}
