import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class FilterPanel extends StepPanel {
    @Override
    public void onNextStep() {

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

    private final NTFSRecordTable recordTable;
    private JTextField searchField;
    private JCheckBox hideEmptyFilesCheckbox;
    private JCheckBox hideEmptyExtensionsCheckbox;

    List<RowFilter<Object, Object>> filters = new ArrayList<>();

    public FilterPanel() {
        setLayout(new BorderLayout());

        JPanel filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.PAGE_START);

        recordTable = new NTFSRecordTable();
        add(new JScrollPane(recordTable), BorderLayout.CENTER);

        doFilter();
    }

    private void doFilter() {
        if(filters.isEmpty()) {
            recordTable.setRowFilter(null);
        } else {
            recordTable.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new BorderLayout());

        initializeSearchField();
        filterPanel.add(searchField, BorderLayout.CENTER);

        initializeHideEmptyExtensionsCheckbox();
        filterPanel.add(hideEmptyExtensionsCheckbox, BorderLayout.LINE_END);

        initializeHideEmptyFilesCheckbox();
        filterPanel.add(hideEmptyFilesCheckbox, BorderLayout.LINE_START);

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
