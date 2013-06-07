package au.com.codeka.warworlds.driver;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.function.Function2D;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;


public class MainWindow {
    private JFrame mFrame;
    private JTextField mBaseUrl;
    private JTextField mUsers;
    private JTextField mThinkTime;
    private JSlider mViewStar;
    private JSlider mSimulate;
    private JSlider mBuild;

    private XYDataset mDataset;

    /**
     * @wbp.parser.entryPoint
     */
    public void show() {
        mFrame = new JFrame();
        mFrame.setTitle("War Worlds Driver");
        mFrame.setBounds(100, 100, 1200, 700);
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));

        JPanel graphPanel = new JPanel();
        mainPanel.add(graphPanel, BorderLayout.CENTER);

        mDataset = DatasetUtilities.sampleFunction2D(new X2(), 
                -4.0, 4.0, 40, "f(x)");

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Statistics", "", "", mDataset, PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getDomainAxis().setLowerMargin(0.0);
        plot.getDomainAxis().setUpperMargin(0.0);
        graphPanel.add(new ChartPanel(chart));

        mFrame.getContentPane().add(mainPanel);

        JPanel controlPanel = new JPanel();
        mainPanel.add(controlPanel, BorderLayout.WEST);
        controlPanel.setLayout(new GridBagLayout());

        JLabel label = new JLabel("Base URL:");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 5, 0);
        constraints.gridwidth = 3;
        constraints.gridx = 0;
        constraints.gridy = 0;
        controlPanel.add(label, constraints);

        mBaseUrl = new JTextField();
        label.setLabelFor(mBaseUrl);
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 0);
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 1;
        controlPanel.add(mBaseUrl, constraints);
        mBaseUrl.setText("http://localhost:8080/realms/beta/");

        label = new JLabel("Users:");
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 2;
        controlPanel.add(label, constraints);

        mUsers = new JTextField();
        label.setLabelFor(mUsers);
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 2;
        controlPanel.add(mUsers, constraints);
        mUsers.setColumns(10);
        mUsers.setText("100");

        label = new JLabel("Think Time:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;
        constraints.gridy = 3;
        controlPanel.add(label, constraints);

        mThinkTime = new JTextField();
        label.setLabelFor(mThinkTime);
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 3;
        controlPanel.add(mThinkTime, constraints);
        mThinkTime.setColumns(10);
        mThinkTime.setText("250");

        label = new JLabel("ms");
        constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 0);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 2;
        constraints.gridy = 3;
        controlPanel.add(label, constraints);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Request Mix"));
        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 0);
        constraints.gridwidth = 3;
        constraints.gridx = 0;
        constraints.gridy = 4;
        controlPanel.add(panel, constraints);

        label = new JLabel("View Star:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(label, constraints);

        mViewStar = new JSlider();
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        panel.add(mViewStar, constraints);

        label = new JLabel("Simulate:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;
        constraints.gridy = 1;
        panel.add(label, constraints);

        mSimulate = new JSlider();
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        panel.add(mSimulate, constraints);

        label = new JLabel("Build:");
        constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;
        constraints.gridy = 2;
        panel.add(label, constraints);

        mBuild = new JSlider();
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 2;
        panel.add(mBuild, constraints);

        panel = new JPanel();
        constraints = new GridBagConstraints();
        constraints.gridwidth = 3;
        constraints.insets = new Insets(5, 5, 0, 0);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 5;
        controlPanel.add(panel, constraints);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JButton button = new JButton("Start");
        panel.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent _) {
                UserManager.i.start(mBaseUrl.getText(),
                                    Integer.parseInt(mUsers.getText()),
                                    Integer.parseInt(mThinkTime.getText()));
            }
        });

        button = new JButton("Stop");
        panel.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent _) {
                UserManager.i.stop();
            }
        });

        panel = new JPanel();
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weighty = 1.0;
        controlPanel.add(panel, constraints);

        mFrame.setVisible(true);
    }

    static class X2 implements Function2D {
        public double getValue(double x) {
            return x * x + 2;
        }
    }
}
