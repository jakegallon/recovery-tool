import javax.swing.*;

public class DetailedProgressBar extends JPanel {

    private final JProgressBar progressBar = new JProgressBar(0, 0);

    private final JLabel percentageLabelPrefix = new JLabel("");
    private final JLabel percentageLabel = new JLabel("0%");

    private final JLabel progressLabel = new JLabel("0 / 0");
    private final JLabel progressLabelSuffix = new JLabel("");

    public int getMaximum() {
        return progressBar.getMaximum();
    }

    public void setMaximum(int i){
        progressBar.setMaximum(i);
    }

    public void setValue(int i){
        progressBar.setValue(i);
    }

    public void setPercentageLabelPrefix(String s) {
        percentageLabelPrefix.setText(s);
    }

    public void setProgressLabelSuffix(String s) {
        progressLabelSuffix.setText(s);
    }

    public void updateInformationLabels() {
        int val = progressBar.getValue();
        int max = progressBar.getMaximum();

        double percent = (double) val / max;
        int percentInt = (int) (percent * 100);
        percentageLabel.setText(percentInt + "%");

        progressLabel.setText(val + " / " + max);
    }

    public DetailedProgressBar() {
        SpringLayout springLayout = new SpringLayout();
        setLayout(springLayout);

        add(progressBar);
        add(percentageLabelPrefix);
        add(percentageLabel);
        add(progressLabel);
        add(progressLabelSuffix);

        int verticalGap = 10;
        int horizontalGap = new JLabel(" ").getPreferredSize().width;
        springLayout.putConstraint(SpringLayout.NORTH, percentageLabelPrefix, 0, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, percentageLabelPrefix, 0, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.NORTH, percentageLabel, 0, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, percentageLabel, horizontalGap, SpringLayout.EAST, percentageLabelPrefix);
        springLayout.putConstraint(SpringLayout.NORTH, progressBar, verticalGap, SpringLayout.SOUTH, percentageLabel);
        springLayout.putConstraint(SpringLayout.EAST, progressBar, 0, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.WEST, progressBar, 0, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.NORTH, progressLabelSuffix, 0, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.EAST, progressLabelSuffix, 0, SpringLayout.EAST, this);
        springLayout.putConstraint(SpringLayout.NORTH, progressLabel, 0, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.EAST, progressLabel, -horizontalGap, SpringLayout.WEST, progressLabelSuffix);
        springLayout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, progressBar);
    }
}
