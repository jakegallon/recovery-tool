import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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

    List<RowFilter<Object, Object>> filters = new ArrayList<>();

    public FilterPanel() {
        SpringLayout springLayout = new SpringLayout();
        setLayout(springLayout);

        JPanel content = new JPanel();
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setVgap(4);
        content.setLayout(borderLayout);

        JPanel filterPanel = createFilterPanel();
        content.add(filterPanel, BorderLayout.PAGE_START);

        JPanel tableHolder = new JPanel(new BorderLayout());
        recordTable = new RecordTable();
        tableHolder.add(new JScrollPane(recordTable), BorderLayout.CENTER);
        tableHolder.add(tableInformation, BorderLayout.PAGE_END);
        content.add(tableHolder, BorderLayout.CENTER);

        add(content);
        springLayout.putConstraint(SpringLayout.NORTH, content, 10, SpringLayout.NORTH, this);
        springLayout.putConstraint(SpringLayout.SOUTH, content, -10, SpringLayout.SOUTH, this);
        springLayout.putConstraint(SpringLayout.WEST, content, 10, SpringLayout.WEST, this);
        springLayout.putConstraint(SpringLayout.EAST, content, -10, SpringLayout.EAST, this);

        doFilter();
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
