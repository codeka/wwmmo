package au.com.codeka.controlfieldtest;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import au.com.codeka.common.PointCloud;
import au.com.codeka.common.Voronoi;
import au.com.codeka.common.ui.GoodFlowLayout;
import au.com.codeka.controlfield.ControlField;

public class AppContent extends JPanel {
    private static final long serialVersionUID = 1L;

    private PointCloud mPointCloud;
    private Voronoi mVoronoi;
    private ControlField mControlField;
    private ControlFieldPanel mControlFieldPanel;

    private JTextField txtEmpireName;

    public AppContent() {
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(0, 0));

        JPanel panel = new JPanel();
        this.add(panel, BorderLayout.NORTH);
        panel.setLayout(new GoodFlowLayout(FlowLayout.LEADING, 0, 0));

        JToolBar toolBar = new JToolBar();
        panel.add(toolBar);

        toolBar.add(new JLabel("Empire Name "));
        txtEmpireName = new JTextField();
        txtEmpireName.setColumns(15);
        txtEmpireName.setText("Empire A");
        toolBar.add(txtEmpireName);

        toolBar = new JToolBar();
        panel.add(toolBar);

        JButton btnRefresh = new JButton("Reset");
        btnRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                generateField();
            }
        });
        toolBar.add(btnRefresh);

        mControlFieldPanel = new ControlFieldPanel();
        this.add(mControlFieldPanel, BorderLayout.CENTER);
    }

    public void generateField() {
        mPointCloud = new PointCloud(new PointCloud.PoissonGenerator().generate(0.5, 0.9, new Random()));
        mVoronoi = new Voronoi(mPointCloud);
        mControlField = new ControlField(mPointCloud, mVoronoi);
        mControlFieldPanel.setPointCloud(mPointCloud);
        mControlFieldPanel.setVoronoi(mVoronoi);
        mControlFieldPanel.setControlField(mControlField);
        mControlFieldPanel.render();
    }
}
