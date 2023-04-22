import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class FilterPanel extends StepPanel {
    @Override
    public void onNextStep() {
        RecoveryPanel recoveryPanel = new RecoveryPanel(recordTable.getRecordsToRecover());
        Frame.setStepPanel(recoveryPanel);

        BottomPanel.setNextButtonEnabled(false);
        BottomPanel.setBackButtonEnabled(true);
    }

    @Override
    public void onBackStep() {
        ScanPanel scanPanel = new ScanPanel(deletedRecords);
        Frame.setStepPanel(scanPanel);

        BottomPanel.setNextButtonEnabled(true);
    }

    private final RowFilter<Object, Object> emptyFileFilter = new RowFilter<Object, Object>() {
        @Override
        public boolean include(RowFilter.Entry<?, ?> entry) {
            int sizeColumnIndex = recordTable.getColumnModel().getColumnIndex("File Size");
            Object fileSizeValue = entry.getValue(sizeColumnIndex);
            return fileSizeValue != null && !"0".equals(fileSizeValue.toString());
        }
    };
    private final RowFilter<Object, Object> emptyExtensionFilter = new RowFilter<Object, Object>() {
        @Override
        public boolean include(RowFilter.Entry<?, ?> entry) {
            int extensionColumnIndex = recordTable.getColumnModel().getColumnIndex("Extension");
            Object extensionValue = entry.getValue(extensionColumnIndex);
            return extensionValue != null && !"".equals(extensionValue.toString());
        }
    };
    private RowFilter<Object, Object> queryFilter;

    private JTextField searchField;
    private JCheckBox hideEmptyFilesCheckbox;
    private JCheckBox hideEmptyExtensionsCheckbox;

    private final RecordTable recordTable;
    private final JLabel tableInformation = new JLabel();

    private final JLabel sizeInformation = new JLabel();
    private final long usedSpace;
    private final Pair<String, Long> unitAndUnitSize;
    private final String convertedUsedSpace;
    private final JPanel content = new JPanel();

    List<RowFilter<Object, Object>> filters = new ArrayList<>();

    private final SpringLayout springLayout = new SpringLayout();

    protected final ArrayList<GenericRecord> deletedRecords;

    public FilterPanel(ArrayList<GenericRecord> deletedRecords) {
        this.deletedRecords = deletedRecords;
        setLayout(springLayout);

        Preferences preferences = Preferences.userNodeForPackage(PartitionPanel.class);
        String selectedFilePath = preferences.get("LAST_DIRECTORY", null);
        usedSpace = new File(selectedFilePath).getFreeSpace();
        unitAndUnitSize = Utility.getUnitAndUnitSize(usedSpace);

        DecimalFormat df = new DecimalFormat("#.##");
        convertedUsedSpace = df.format((double) usedSpace / unitAndUnitSize.getValue());

        Font headerFont = new Font("Arial", Font.BOLD, 17);
        Font textFont = new Font("Arial", Font.PLAIN, 14);

        JLabel filterLabel = new JLabel("Step 4: Filter");
        filterLabel.setFont(headerFont);
        add(filterLabel);
        springLayout.putConstraint(SpringLayout.NORTH, filterLabel, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.WEST, filterLabel, 10, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.EAST, filterLabel, -10, SpringLayout.EAST, this);

        JLabel waitText = new JLabel("<html>Below are all of the deleted files which the program found on your drive. By selecting them using the checkbox on the left, you can mark them for recovery. Click next to initiate recovery, or cancel if you do not wish to recover any files.</html>");
        waitText.setFont(textFont);
        add(waitText);
        springLayout.putConstraint(SpringLayout.NORTH, waitText, 0, SpringLayout.SOUTH, filterLabel);
        springLayout.putConstraint(SpringLayout.WEST, waitText, 0, SpringLayout.WEST, filterLabel);
        springLayout.putConstraint(SpringLayout.EAST, waitText, 0, SpringLayout.EAST, filterLabel);

        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setVgap(4);
        content.setLayout(borderLayout);

        JPanel filterPanel = createFilterPanel();
        content.add(filterPanel, BorderLayout.PAGE_START);

        JPanel tableHolder = new JPanel(new BorderLayout());
        recordTable = new RecordTable(this);
        tableHolder.add(new JScrollPane(recordTable), BorderLayout.CENTER);

        JPanel informationPanel = new JPanel(new BorderLayout());
        informationPanel.add(tableInformation, BorderLayout.LINE_START);
        informationPanel.add(sizeInformation, BorderLayout.LINE_END);

        tableHolder.add(informationPanel, BorderLayout.PAGE_END);
        content.add(tableHolder, BorderLayout.CENTER);

        add(content);
        springLayout.putConstraint(SpringLayout.NORTH, content, 10, SpringLayout.SOUTH, waitText);
        springLayout.putConstraint(SpringLayout.SOUTH, content, -10, SpringLayout.SOUTH, this);
        springLayout.putConstraint(SpringLayout.WEST, content, 0, SpringLayout.WEST, filterLabel);
        springLayout.putConstraint(SpringLayout.EAST, content, 0, SpringLayout.EAST, filterLabel);

        doFilter();
    }

    public void setSelectionSize(long l) {
        DecimalFormat df = new DecimalFormat("#.##");
        String convertedSelectedSpace = df.format((double) l / unitAndUnitSize.getValue());
        sizeInformation.setText(convertedSelectedSpace + unitAndUnitSize.getKey() + "/" + convertedUsedSpace + unitAndUnitSize.getKey());

        boolean isOverCapacity = l > usedSpace;
        if(isOverCapacity) {
            BottomPanel.setNextButtonEnabled(false);
            if(errorMessagePanel == null) {
                showErrorMessage();
            }
        } else {
            BottomPanel.setNextButtonEnabled(true);
            if(errorMessagePanel != null) {
                springLayout.getConstraints(content).setConstraint(SpringLayout.SOUTH, null);
                springLayout.putConstraint(SpringLayout.SOUTH, content, -10, SpringLayout.SOUTH, this);

                remove(errorMessagePanel);
                errorMessagePanel = null;

                revalidate();
                repaint();
            }
        }
    }

    private JPanel errorMessagePanel;
    private void showErrorMessage() {
        errorMessagePanel = new JPanel();
        errorMessagePanel.setBorder(new LineBorder(Color.red, 1));
        BoxLayout boxLayout = new BoxLayout(errorMessagePanel, BoxLayout.X_AXIS);
        errorMessagePanel.setLayout(boxLayout);

        JLabel exclamationMark = new JLabel("!");
        exclamationMark.setForeground(Color.RED);

        JLabel errorMessage = new JLabel("<html>The combined total size of all selected files exceeds the free space on your output drive. Consider selecting a new output drive, or restoring less files at a time.</html>");
        errorMessage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        errorMessagePanel.add(Box.createHorizontalStrut(4));
        errorMessagePanel.add(exclamationMark);
        errorMessagePanel.add(Box.createHorizontalStrut(4));
        errorMessagePanel.add(errorMessage);
        errorMessagePanel.add(Box.createHorizontalStrut(4));

        exclamationMark.setFont(new Font(Font.SANS_SERIF, Font.BOLD,  48));

        springLayout.putConstraint(SpringLayout.NORTH, errorMessagePanel, -errorMessagePanel.getPreferredSize().height, SpringLayout.SOUTH, errorMessagePanel);
        springLayout.putConstraint(SpringLayout.SOUTH, errorMessagePanel, -10, SpringLayout.SOUTH, this);
        springLayout.putConstraint(SpringLayout.WEST, errorMessagePanel, 0, SpringLayout.WEST, content);
        springLayout.putConstraint(SpringLayout.EAST, errorMessagePanel, 0, SpringLayout.EAST, content);

        springLayout.getConstraints(content).setConstraint(SpringLayout.SOUTH, null);
        springLayout.putConstraint(SpringLayout.SOUTH, content, -10, SpringLayout.NORTH, errorMessagePanel);

        revalidate();
        repaint();

        add(errorMessagePanel);
    }

    private void doFilter() {
        if(filters.isEmpty()) {
            recordTable.setRowFilter(null);
        } else {
            recordTable.setRowFilter(RowFilter.andFilter(filters));
        }
        tableInformation.setText(recordTable.getModel().getRowCount() + " rows, " +
                recordTable.getRowSorter().getViewRowCount() + " shown, " +
                (recordTable.getModel().getRowCount() - recordTable.getRowSorter().getViewRowCount()) + " filtered."
        );
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.PAGE_AXIS));

        initializeSearchField();
        initializeHideEmptyExtensionsCheckbox();
        initializeHideEmptyFilesCheckbox();

        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.add(new JLabel("Search: "), BorderLayout.LINE_START);
        queryPanel.add(searchField, BorderLayout.CENTER);

        JPanel filters = new JPanel(new GridLayout(2, 2));
        filters.add(hideEmptyExtensionsCheckbox);
        filters.add(new JPanel());
        filters.add(hideEmptyFilesCheckbox);
        filters.add(queryPanel);

        filterPanel.add(filters);

        return filterPanel;
    }

    private void initializeSearchField() {
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onQueryUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onQueryUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void initializeHideEmptyFilesCheckbox() {
        hideEmptyFilesCheckbox = new JCheckBox("Hide files with no data");
        hideEmptyFilesCheckbox.setSelected(true);
        filters.add(emptyFileFilter);

        hideEmptyFilesCheckbox.addActionListener(e -> {
            if(hideEmptyFilesCheckbox.isSelected()) {
                filters.add(emptyFileFilter);
            } else {
                filters.remove(emptyFileFilter);
            }
            doFilter();
        });
    }

    private void initializeHideEmptyExtensionsCheckbox() {
        hideEmptyExtensionsCheckbox = new JCheckBox("Hide files with no extension");
        hideEmptyExtensionsCheckbox.setSelected(true);
        filters.add(emptyExtensionFilter);

        hideEmptyExtensionsCheckbox.addActionListener(e -> {
            if(hideEmptyExtensionsCheckbox.isSelected()) {
                filters.add(emptyExtensionFilter);
            } else {
                filters.remove(emptyExtensionFilter);
            }
            doFilter();
        });
    }

    private void onQueryUpdate() {
        String query = searchField.getText().trim();
        if(query.length() == 0) {
            filters.remove(queryFilter);
            queryFilter = null;
        } else {
            filters.remove(queryFilter);
            queryFilter = RowFilter.regexFilter("(?i)" + query);
            filters.add(queryFilter);
        }
        doFilter();
    }
}
