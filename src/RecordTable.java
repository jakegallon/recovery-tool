import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import javafx.util.Pair;

public class RecordTable extends JTable {

    private final static String MFT_RECORD_COLUMN = "MFT Record";
    private final static String IS_SELECTED_COLUMN = " ";
    private final static String FILE_SIZE_COLUMN = "File Size";

    private final String[] columns = {MFT_RECORD_COLUMN, IS_SELECTED_COLUMN, "Name", "Extension", FILE_SIZE_COLUMN, "Creation Date", "Modified Date"};
    private final DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
    private final TableRowSorter<DefaultTableModel> tableSorter = new TableRowSorter<>(tableModel);

    private final ArrayList<GenericRecord> recordsToRecover = new ArrayList<>();
    public ArrayList<GenericRecord> getRecordsToRecover() {
        return recordsToRecover;
    }

    private final FilterPanel parent;

    public RecordTable(FilterPanel parent) {
        this.parent = parent;
        setModel(tableModel);

        setTableRules();
        setColumnWidths();
        configureTableSorting();

        populateTable();
        addMouseListener();
    }

    private void onRecordsToRecoverUpdated() {
        if(recordsToRecover.isEmpty()) {
            parent.setSelectionSize(0);
            BottomPanel.setNextButtonEnabled(false);
            return;
        }
        long size = 0;
        for(GenericRecord record : recordsToRecover) {
            size += record.fileSizeBytes;
        }
        parent.setSelectionSize(size);
    }

    private void setTableRules() {
        getTableHeader().setReorderingAllowed(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void setColumnWidths() {
        TableColumn isSelectedColumn = getColumnModel().getColumn(getColumnModel().getColumnIndex(IS_SELECTED_COLUMN));
        isSelectedColumn.setMinWidth(getRowHeight());
        isSelectedColumn.setPreferredWidth(getRowHeight());
        isSelectedColumn.setMaxWidth(getRowHeight());

        TableColumn mftRecordColumn = getColumnModel().getColumn(getColumnModel().getColumnIndex(MFT_RECORD_COLUMN));
        mftRecordColumn.setMinWidth(0);
        mftRecordColumn.setPreferredWidth(0);
        mftRecordColumn.setMaxWidth(0);
    }

    private void configureTableSorting() {
        int sizeColumnIndex = getColumnModel().getColumnIndex(FILE_SIZE_COLUMN);
        Comparator<Long> longComparator = Long::compareTo;
        tableSorter.setComparator(sizeColumnIndex, longComparator);

        setRowSorter(tableSorter);
    }

    private void populateTable() {
        ArrayList<GenericRecord> deletedRecords = parent.deletedRecords;
        for(GenericRecord deletedRecord : deletedRecords) {
            Object[] row = {
                    deletedRecord,
                    false,
                    deletedRecord.getFileName(),
                    deletedRecord.getFileExtension(),
                    deletedRecord.getFileSizeBytes(),
                    deletedRecord.getCreationTime(),
                    deletedRecord.getModifiedTime()
            };
            tableModel.addRow(row);
        }
    }

    private void addMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = getColumnModel().getColumnIndex(IS_SELECTED_COLUMN);
                int visualRow = getSelectedRow();
                int row = getRowSorter().convertRowIndexToModel(visualRow);

                if(columnAtPoint(e.getPoint()) != column) setValueAt(!(boolean) getValueAt(visualRow, column), visualRow, column);
                boolean isSelected = (boolean) getValueAt(visualRow, column);

                GenericRecord thisRecord = (GenericRecord) tableModel.getValueAt(row, getColumnModel().getColumnIndex(MFT_RECORD_COLUMN));
                if(isSelected) {
                    if(!recordsToRecover.contains(thisRecord))
                        recordsToRecover.add(thisRecord);
                } else {
                    while(recordsToRecover.contains(thisRecord))
                        recordsToRecover.remove(thisRecord);
                }
                onRecordsToRecoverUpdated();
            }
        });
    }

    public void setRowFilter(RowFilter<DefaultTableModel, Object> rowFilter) {
        tableSorter.setRowFilter(rowFilter);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == getColumnModel().getColumnIndex(IS_SELECTED_COLUMN) ? Boolean.class : Object.class;
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == getColumnModel().getColumnIndex(IS_SELECTED_COLUMN))
            return new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.setSelected((Boolean) value);
                    return checkBox;
                }
            };
        else if (column == getColumnModel().getColumnIndex(FILE_SIZE_COLUMN))
            return new DefaultTableCellRenderer() {
                @Override
                public void setHorizontalAlignment(int alignment) {
                    super.setHorizontalAlignment(JLabel.RIGHT);
                }
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    long fileSize = (Long) value;

                    Pair<String, Long> storageUnitInfo = Utility.getUnitAndUnitSize(fileSize);
                    String storageUnit = storageUnitInfo.getKey();
                    long storageUnitSize = storageUnitInfo.getValue();

                    String convertedFileSize;
                    if(storageUnitSize != 0L) {
                        convertedFileSize = String.format("%.2f", (float) fileSize / storageUnitSize);
                    } else {
                        convertedFileSize = String.valueOf(fileSize);
                    }

                    return super.getTableCellRendererComponent(table, convertedFileSize + storageUnit, isSelected, hasFocus, row, column);
                }
            };
        return super.getCellRenderer(row, column);
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        return column == getColumnModel().getColumnIndex(IS_SELECTED_COLUMN) ? new DefaultCellEditor(new JCheckBox()) : super.getCellEditor(row, column);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == getColumnModel().getColumnIndex(IS_SELECTED_COLUMN);
    }
}