package com.sarzilsazibpharmacy;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * Stock Management dashboard.
 * Compatibility note: data/stock.txt remains in the original 7-field format
 * consumed by the existing SalesManagement.java. Category and Company are
 * stored separately in data/stock_extra.txt, keyed by Medicine ID.
 */
public class StockManagement extends JFrame {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path STOCK_FILE = DATA_DIR.resolve("stock.txt");
    private static final Path EXTRA_FILE = DATA_DIR.resolve("stock_extra.txt");
    private static final Path RECYCLE_FILE = DATA_DIR.resolve("stock_recycle_bin.txt");
    private static final Path MEDICINE_ID_COUNTER_FILE = DATA_DIR.resolve("medicine_id_counter.txt");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final String[] FORMS = {"Tablet", "Capsule", "Syrup", "Suspension", "Injection", "Drop", "Inhaler", "Cream", "Suppository", "cosmetics", "others"};
    private static final String[] CATEGORIES = {"Antibiotics", "Antacid", "Allergy", "Flue", "Antifungal", "Diabetic", "Blood pressure", "Cough Suppressant", "Vitamin / Mineral", "Paracetamol", "Dermatological", "Ear / Nasal Preparations", "Other"};
    private static final String[] COMPANIES = {"Square", "Incepta", "Beximco", "Renata", "Healthcare", "Opsonin", "Eskayef", "Aristopharma", "ACI", "Drug International", "Other"};
    private static final String[] SHELVES = {"A1", "A2", "B1", "B2", "C1", "C2", "D1", "D2", "E1", "E2", "others"};
    private static final String[] COLUMNS = {"SERIAL NO.", "DATE", "TIME", "MEDICINE ID", "MEDICINE NAME", "FORM", "CATEGORY", "COMPANY", "QUANTITY (PCS.)", "MRP (TK.)", "EXPIRY DATE", "SHELF NUMBER"};

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable stockTable = new JTable(tableModel);
    private final JLabel dateLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JLabel totalMedicinesLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel totalQuantityLabel = new JLabel("0", SwingConstants.CENTER);
    private final JLabel totalMrpLabel = new JLabel("Tk. 0.00", SwingConstants.CENTER);
    private final Color peach = new Color(247, 203, 173);
    private final Color maroon = new Color(105, 18, 42);
    private final Color olive = new Color(112, 105, 32);

    private static final class Medicine {
        String id, name, form, category, company, expiry, shelf;
        int quantity;
        double mrp;
        Medicine(String id, String name, String form, String category, String company,
                 int quantity, double mrp, String expiry, String shelf) {
            this.id=id; this.name=name; this.form=form; this.category=category; this.company=company;
            this.quantity=quantity; this.mrp=mrp; this.expiry=expiry; this.shelf=shelf;
        }
        String[] legacyFields() { return new String[]{id,name,form,String.valueOf(quantity),money(mrp),expiry,shelf}; }
    }

    // Recycle record keeps the deleted medicine data and the date/time it was moved to Recycle Bin.
    private static final class RecycleRecord {
        Medicine medicine; LocalDateTime deletedAt;
        RecycleRecord(Medicine medicine, LocalDateTime deletedAt) { this.medicine = medicine; this.deletedAt = deletedAt; }
    }

    public StockManagement() {
        setTitle("Sarzil-Sazib-Pharmacy - Stock Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1600, 900);
        setMinimumSize(new Dimension(1250, 760));
        setLocationRelativeTo(null);
        buildDashboard();
        refreshTable();
        startClock();
    }

    private void buildDashboard() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(255, 250, 245));
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(new Color(255, 250, 245));
        content.setBorder(new EmptyBorder(14, 18, 16, 18));

        JLabel brand = new JLabel("SARZIL SAZIB PHARMACY", SwingConstants.CENTER);
        brand.setFont(new Font("Arial", Font.BOLD, 30));
        brand.setForeground(maroon);

        JPanel header = new JPanel(new BorderLayout(4, 5));
        header.setOpaque(false);
        header.add(brand, BorderLayout.NORTH);
        JPanel dateTime = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 2));
        dateTime.setOpaque(false);
        dateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        timeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        dateTime.add(dateLabel);
        dateTime.add(timeLabel);
        header.add(dateTime, BorderLayout.SOUTH);
        content.add(header, BorderLayout.NORTH);

        JPanel tableArea = new JPanel(new BorderLayout(8, 10));
        tableArea.setOpaque(false);
        JLabel tableTitle = new JLabel("STOCK TABLE OF MEDICINES", SwingConstants.CENTER);
        tableTitle.setFont(new Font("Arial", Font.BOLD, 23));
        tableTitle.setForeground(olive);
        tableArea.add(tableTitle, BorderLayout.NORTH);
        configureTable();
        JScrollPane scroll = new JScrollPane(stockTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(190, 170, 150), 1));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableArea.add(scroll, BorderLayout.CENTER);
        content.add(tableArea, BorderLayout.CENTER);

        JLabel footer = new JLabel("Sarzil-Sazib-Pharmacy", SwingConstants.CENTER);
        footer.setFont(new Font("Arial", Font.ITALIC, 12));
        footer.setForeground(new Color(105, 80, 70));
        content.add(footer, BorderLayout.SOUTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(peach);
        side.setPreferredSize(new Dimension(245, 0));
        side.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(190, 135, 105)),
                new EmptyBorder(22, 14, 18, 14)));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        String[] names = {"ADD STOCK", "UPDATE STOCK", "DELETE STOCK", "RECYCLE BIN", "STOCK ALERT", "SEARCH", "REFRESH", "BACK"};
        Color[] colors = {
                new Color(91, 8, 31), new Color(111, 18, 43), new Color(132, 34, 57),
                new Color(105, 18, 42), new Color(153, 55, 75), new Color(174, 83, 99),
                new Color(183, 91, 108), new Color(194, 112, 124)
        };
        for (int i = 0; i < names.length; i++) {
            final int action = i;
            JButton button = new JButton(names[i]);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            button.setPreferredSize(new Dimension(210, 48));
            button.setFont(new Font("Arial", Font.BOLD, 15));
            button.setBackground(colors[i]);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            button.addActionListener(e -> handleSidebarAction(action));
            side.add(button);
            side.add(Box.createVerticalStrut(11));
        }
        side.add(Box.createVerticalStrut(8));
        side.add(summaryBox("TOTAL NUMBER OF MEDICINES", totalMedicinesLabel));
        side.add(Box.createVerticalStrut(10));
        side.add(summaryBox("TOTAL QUANTITIES (PCS.)", totalQuantityLabel));
        side.add(Box.createVerticalStrut(10));
        side.add(summaryBox("TOTAL MRP (TK.) OF MEDICINES", totalMrpLabel));
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel summaryBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout(3, 5));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 91));
        box.setBackground(new Color(255, 239, 225));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(130, 55, 65), 1),
                new EmptyBorder(7, 5, 7, 5)));
        JLabel heading = new JLabel("<html><center>" + title + "</center></html>", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 11));
        heading.setForeground(maroon);
        value.setFont(new Font("Arial", Font.BOLD, 19));
        value.setForeground(new Color(65, 45, 35));
        box.add(heading, BorderLayout.NORTH);
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    private void configureTable() {
        stockTable.setFont(new Font("Arial", Font.PLAIN, 12));
        stockTable.setRowHeight(29);
        stockTable.setFillsViewportHeight(true);
        stockTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stockTable.setGridColor(new Color(220, 205, 190));
        stockTable.setSelectionBackground(new Color(255, 225, 150));
        stockTable.setSelectionForeground(Color.BLACK);
        JTableHeader header = stockTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 12));
        header.setBackground(new Color(238, 224, 205));
        header.setForeground(new Color(65, 45, 35));
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < stockTable.getColumnCount(); i++) stockTable.getColumnModel().getColumn(i).setCellRenderer(center);
        // Proportional preferred widths; AUTO_RESIZE_ALL_COLUMNS keeps all 12 visible.
        int[] widths = {65, 86, 91, 95, 150, 82, 115, 120, 105, 92, 105, 95};
        for (int i = 0; i < widths.length; i++) stockTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private void handleSidebarAction(int action) {
        switch (action) {
            case 0: addStock(); break;
            case 1: updateStock(); break;
            case 2: deleteStock(); break;
            case 3: showRecycleBin(); break;
            case 4: showStockAlert(); break;
            case 5: searchMedicine(); break;
            case 6: refreshStockTable(); break;
            case 7: dispose(); break;
            default: break;
        }
    }

    private void startClock() {
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> updateClockLabels());
        timer.start();
        updateClockLabels();
    }

    private void updateClockLabels() {
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText("DATE: " + now.format(DISPLAY_DATE));
        timeLabel.setText("TIME: " + now.format(TIME_FMT));
    }

    private void refreshTable() {
        try {
            List<Medicine> medicines = readMedicines();
            tableModel.setRowCount(0);
            LocalDateTime now = LocalDateTime.now();
            String date = now.format(DISPLAY_DATE);
            String time = now.format(TIME_FMT);
            long totalQty = 0;
            double totalValue = 0;
            int serial = 1;
            for (Medicine m : medicines) {
                tableModel.addRow(new Object[]{serial++, date, time, m.id, m.name, m.form, m.category,
                        m.company, m.quantity, money(m.mrp), m.expiry, m.shelf});
                totalQty += m.quantity;
                totalValue += m.quantity * m.mrp;
            }
            totalMedicinesLabel.setText(String.valueOf(medicines.size()));
            totalQuantityLabel.setText(String.valueOf(totalQty));
            totalMrpLabel.setText("Tk. " + money(totalValue));
        } catch (IOException ex) {
            showError("Could not load stock records.\n" + ex.getMessage());
        }
    }

    // ADD STOCK: Generate a permanent, non-editable Medicine ID automatically.
    private void addStock() {
        try {
            String generatedId = nextMedicineId();
            Medicine blank = new Medicine(generatedId, "", FORMS[0], CATEGORIES[CATEGORIES.length-1], COMPANIES[COMPANIES.length-1], 0, 0, "", SHELVES[0]);
            Medicine result = showMedicineEditor("ADD STOCK", blank, false);
            if (result == null) return;
            List<Medicine> list = readMedicines();
            for (Medicine m : list) {
                if (m.id.equalsIgnoreCase(result.id)) { errorDialog("Medicine ID already exists."); return; }
                if (m.name.equalsIgnoreCase(result.name)) { errorDialog("Medicine Name already exists."); return; }
            }
            list.add(result);
            writeMedicines(list);
            saveMedicineIdCounter(result.id);
            refreshTable();
            successDialog("Medicine added successfully. Generated ID: " + result.id);
        } catch (IOException ex) { showError("Unable to save medicine.\n" + ex.getMessage()); }
    }

    private Medicine showMedicineEditor(String title, Medicine old, boolean editing) {
        JTextField id = new JTextField(old.id, 18);
        JTextField name = new JTextField(old.name, 18);
        JComboBox<String> form = new JComboBox<>(FORMS);
        JComboBox<String> category = new JComboBox<>(CATEGORIES);
        JComboBox<String> company = new JComboBox<>(COMPANIES);
        JTextField quantity = new JTextField(editing ? String.valueOf(old.quantity) : "", 18);
        JTextField mrp = new JTextField(editing ? money(old.mrp) : "", 18);
        JTextField expiry = new JTextField(old.expiry, 18);
        JComboBox<String> shelf = new JComboBox<>(SHELVES);
        form.setSelectedItem(old.form);
        category.setSelectedItem(old.category);
        company.setSelectedItem(old.company);
        shelf.setSelectedItem(old.shelf);
        JPanel panel = new JPanel(new GridLayout(9, 2, 9, 9));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        addEditorRow(panel, "MEDICINE ID (AUTO / LOCKED)", id);
        addEditorRow(panel, "MEDICINE NAME", name);
        addEditorRow(panel, "FORM", form);
        addEditorRow(panel, "CATEGORY", category);
        addEditorRow(panel, "COMPANY", company);
        addEditorRow(panel, "QUANTITY (PCS.)", quantity);
        addEditorRow(panel, "MRP (TK.) PER UNIT", mrp);
        addEditorRow(panel, "EXPIRY DATE (DD-MM-YYYY)", expiry);
        addEditorRow(panel, "SHELF NUMBER", shelf);
        id.setEditable(false); // Medicine ID is generated by the system and cannot be edited.
        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return null;
            try {
                String medicineId = id.getText().trim();
                String medicineName = name.getText().trim();
                if (medicineId.isEmpty() || medicineName.isEmpty()) throw new IllegalArgumentException("Medicine ID and Medicine Name are required.");
                if (startsNegative(medicineName)) throw new IllegalArgumentException("Medicine Name cannot start with a minus sign.");
                int qty = Integer.parseInt(quantity.getText().trim());
                if (qty < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
                double unitMrp = Double.parseDouble(mrp.getText().trim());
                if (!Double.isFinite(unitMrp) || unitMrp < 0) throw new IllegalArgumentException("MRP must be a valid non-negative number.");
                String exp = expiry.getText().trim();
                LocalDate expiryDate = LocalDate.parse(exp, DATE_FMT);
                if (expiryDate.isBefore(LocalDate.now())) throw new IllegalArgumentException("Past date cannot be used as an expiry date.");
                return new Medicine(medicineId, medicineName, (String)form.getSelectedItem(),
                        (String)category.getSelectedItem(), (String)company.getSelectedItem(), qty,
                        unitMrp, exp, (String)shelf.getSelectedItem());
            } catch (NumberFormatException | DateTimeParseException ex) {
                errorDialog("Invalid Quantity, MRP or Expiry Date.\nUse DD-MM-YYYY for expiry date.");
            } catch (IllegalArgumentException ex) { errorDialog(ex.getMessage()); }
        }
    }

    private void addEditorRow(JPanel panel, String label, JComponent field) {
        JLabel l = new JLabel(label + ":");
        l.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(l);
        if (field instanceof JTextField) ((JTextField)field).setFont(new Font("Arial", Font.PLAIN, 13));
        panel.add(field);
    }
    // UPDATE STOCK: Medicine ID is locked; only name, quantity, form, category, company, MRP, expiry and shelf can change.
    private void updateStock() {
        try {
            List<Medicine> list = readMedicines();
            if (list.isEmpty()) { infoDialog("No medicine is available in stock."); return; }
            JComboBox<String> ids = new JComboBox<>();
            for (Medicine m : list) ids.addItem(m.id);
            JPanel choosePanel = new JPanel(new BorderLayout(8, 8));
            choosePanel.add(new JLabel("Select Medicine ID:"), BorderLayout.NORTH);
            choosePanel.add(ids, BorderLayout.CENTER);
            if (JOptionPane.showConfirmDialog(this, choosePanel, "UPDATE STOCK - SELECT MEDICINE", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            String selectedId = (String)ids.getSelectedItem();
            Medicine old = findById(list, selectedId);
            if (old == null) return;
            Medicine updated = showMedicineEditor("UPDATE STOCK - MEDICINE DETAILS", old, true);
            if (updated == null) return;
            for (Medicine m : list) {
                if (!m.id.equalsIgnoreCase(old.id) && m.name.equalsIgnoreCase(updated.name)) { errorDialog("Medicine Name already exists."); return; }
            }
            int index = list.indexOf(old);
            list.set(index, updated);
            writeMedicines(list);
            refreshTable();
            successDialog("Medicine updated successfully.");
        } catch (IOException ex) { showError("Unable to update medicine.\n" + ex.getMessage()); }
    }

    // DELETE STOCK: Move the complete medicine record to Recycle Bin instead of permanently deleting it.
    private void deleteStock() {
        try {
            List<Medicine> list = readMedicines();
            if (list.isEmpty()) { infoDialog("No medicine is available in stock."); return; }
            JComboBox<String> ids = new JComboBox<>();
            for (Medicine m : list) ids.addItem(m.id);
            if (JOptionPane.showConfirmDialog(this, ids, "DELETE STOCK - SELECT MEDICINE ID", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
            Medicine selected = findById(list, (String)ids.getSelectedItem());
            if (selected == null) return;
            JTextArea details = new JTextArea("MEDICINE ID: " + selected.id + "\nMEDICINE NAME: " + selected.name
                    + "\nFORM: " + selected.form + "\nCATEGORY: " + selected.category + "\nCOMPANY: " + selected.company
                    + "\nQUANTITY (PCS.): " + selected.quantity + "\nMRP (TK.): " + money(selected.mrp)
                    + "\nEXPIRY DATE: " + selected.expiry + "\nSHELF NUMBER: " + selected.shelf
                    + "\n\nMOVE THIS MEDICINE TO RECYCLE BIN?");
            details.setEditable(false);
            details.setFont(new Font("Arial", Font.BOLD, 13));
            details.setBackground(new Color(250, 245, 238));
            int confirm = JOptionPane.showConfirmDialog(this, new JScrollPane(details), "CONFIRM DELETE STOCK", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            List<RecycleRecord> recycle = readRecycleBin();
            recycle.add(new RecycleRecord(selected, LocalDateTime.now()));
            writeRecycleBin(recycle);
            list.remove(selected);
            writeMedicines(list);
            refreshTable();
            successDialog("Medicine moved to Recycle Bin successfully.");
        } catch (IOException ex) { showError("Unable to move medicine to Recycle Bin.\n" + ex.getMessage()); }
    }

    // RECYCLE BIN: Display deleted stock as complete rows and provide Restore / Permanent Delete actions.
    private void showRecycleBin() {
        try {
            List<RecycleRecord> records = readRecycleBin();
            String[] cols = {"SERIAL NO.", "DATE", "TIME", "MEDICINE ID", "MEDICINE NAME", "FORM", "CATEGORY", "COMPANY", "QUANTITY (PCS.)", "MRP (TK.)", "EXPIRY DATE", "SHELF NUMBER"};
            DefaultTableModel model = readOnlyModel(cols);
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            for (int i = 0; i < records.size(); i++) {
                RecycleRecord r = records.get(i); Medicine m = r.medicine;
                model.addRow(new Object[]{i + 1, r.deletedAt.format(dateFmt), r.deletedAt.format(TIME_FMT), m.id, m.name, m.form,
                        m.category, m.company, m.quantity, money(m.mrp), m.expiry, m.shelf});
            }
            JTable recycleTable = new JTable(model);
            recycleTable.setFont(new Font("Arial", Font.PLAIN, 12));
            recycleTable.setRowHeight(30);
            recycleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            recycleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JTableHeader h = recycleTable.getTableHeader();
            h.setFont(new Font("Arial", Font.BOLD, 12)); h.setBackground(new Color(238, 224, 205));
            h.setForeground(maroon); h.setReorderingAllowed(false);
            JScrollPane scroll = new JScrollPane(recycleTable);
            scroll.setPreferredSize(new Dimension(1250, 430));
            JButton permanentButton = new JButton("DELETE PERMANENT");
            permanentButton.setBackground(new Color(91, 8, 31)); permanentButton.setForeground(Color.WHITE);
            permanentButton.setFont(new Font("Arial", Font.BOLD, 14)); permanentButton.setFocusPainted(false);
            permanentButton.setPreferredSize(new Dimension(230, 48));
            JButton restoreButton = new JButton("RESTORE");
            restoreButton.setBackground(new Color(183, 91, 108)); restoreButton.setForeground(Color.WHITE);
            restoreButton.setFont(new Font("Arial", Font.BOLD, 14)); restoreButton.setFocusPainted(false);
            restoreButton.setPreferredSize(new Dimension(180, 48));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 8));
            actions.add(permanentButton); actions.add(restoreButton);
            JPanel panel = new JPanel(new BorderLayout(8, 10));
            panel.add(scroll, BorderLayout.CENTER); panel.add(actions, BorderLayout.SOUTH);
            JDialog dialog = new JDialog(this, "RECYCLE BIN", true);
            dialog.setContentPane(panel); dialog.pack(); dialog.setLocationRelativeTo(this);

            // PERMANENT DELETE: Remove the selected deleted record from the Recycle Bin file forever.
            permanentButton.addActionListener(e -> {
                int row = recycleTable.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(dialog, "Please select a medicine row."); return; }
                int answer = JOptionPane.showConfirmDialog(dialog, "Permanently delete this medicine record?", "CONFIRM PERMANENT DELETE", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) return;
                try {
                    List<RecycleRecord> latest = readRecycleBin();
                    if (row < latest.size()) latest.remove(row);
                    writeRecycleBin(latest);
                    dialog.dispose(); showRecycleBin();
                } catch (IOException ex) { showError("Permanent delete failed.\n" + ex.getMessage()); }
            });

            // RESTORE: Return the selected medicine to the active Stock Table without changing its Medicine ID.
            restoreButton.addActionListener(e -> {
                int row = recycleTable.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(dialog, "Please select a medicine row."); return; }
                try {
                    List<RecycleRecord> latest = readRecycleBin();
                    if (row >= latest.size()) return;
                    RecycleRecord record = latest.get(row);
                    List<Medicine> active = readMedicines();
                    for (Medicine m : active) {
                        if (m.id.equalsIgnoreCase(record.medicine.id)) { errorDialog("This Medicine ID already exists in active stock."); return; }
                        if (m.name.equalsIgnoreCase(record.medicine.name)) { errorDialog("This Medicine Name already exists in active stock."); return; }
                    }
                    active.add(record.medicine);
                    writeMedicines(active);
                    latest.remove(row);
                    writeRecycleBin(latest);
                    refreshTable();
                    dialog.dispose();
                    successDialog("Medicine restored to Stock Table successfully.");
                } catch (IOException ex) { showError("Restore failed.\n" + ex.getMessage()); }
            });
            dialog.setVisible(true);
        } catch (IOException ex) { showError("Unable to open Recycle Bin.\n" + ex.getMessage()); }
    }

    // REFRESH: Clear any search highlight and reload the current stock records and summary cards.
    private void refreshStockTable() {
        stockTable.clearSelection();
        refreshTable();
    }

    // STOCK ALERT: Show medicines with low quantity or an expiry date within one month.
    private void showStockAlert() {
        try {
            List<Medicine> list = readMedicines();
            String[] qtyCols = {"MEDICINE ID", "MEDICINE NAME", "FORM", "QUANTITY (PCS.)", "SHELF NUMBER"};
            String[] expCols = {"MEDICINE ID", "MEDICINE NAME", "EXPIRY DATE", "QUANTITY (PCS.)", "SHELF NUMBER"};
            DefaultTableModel qtyModel = readOnlyModel(qtyCols);
            DefaultTableModel expModel = readOnlyModel(expCols);
            LocalDate today = LocalDate.now();
            LocalDate limit = today.plusMonths(1);
            for (Medicine m : list) {
                if (m.quantity <= 20) qtyModel.addRow(new Object[]{m.id,m.name,m.form,m.quantity,m.shelf});
                LocalDate exp = LocalDate.parse(m.expiry, DATE_FMT);
                if (!exp.isAfter(limit)) expModel.addRow(new Object[]{m.id,m.name,m.expiry,m.quantity,m.shelf});
            }
            JTable qtyTable = alertTable(qtyModel);
            JTable expTable = alertTable(expModel);
            JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(new Font("Arial", Font.BOLD, 16));
            tabs.addTab("ALERT BY QUANTITY (PCS.) — 20 OR LESS", new JScrollPane(qtyTable));
            tabs.addTab("ALERT BY EXPIRY DATE — WITHIN ONE MONTH", new JScrollPane(expTable));
            tabs.setPreferredSize(new Dimension(1050, 590));
            JOptionPane.showMessageDialog(this, tabs, "STOCK ALERT", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException | DateTimeParseException ex) { showError("Unable to generate Stock Alert.\n" + ex.getMessage()); }
    }

    private JTable alertTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(32);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JTableHeader h = table.getTableHeader();
        h.setFont(new Font("Arial", Font.BOLD, 14));
        h.setBackground(new Color(238, 224, 205));
        h.setForeground(maroon);
        h.setReorderingAllowed(false);
        return table;
    }

    private DefaultTableModel readOnlyModel(String[] cols) {
        return new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }

    // SEARCH: Find a medicine by its exact name and highlight its row in the Stock Table.
    private void searchMedicine() {
        stockTable.clearSelection();
        String query = JOptionPane.showInputDialog(this, "Enter Medicine Name:", "SEARCH MEDICINE", JOptionPane.QUESTION_MESSAGE);
        if (query == null) return;
        query = query.trim();
        if (query.isEmpty()) { errorDialog("Medicine Name cannot be empty."); return; }
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String name = String.valueOf(tableModel.getValueAt(row, 4));
            if (name.equalsIgnoreCase(query)) {
                stockTable.setRowSelectionInterval(row, row);
                stockTable.scrollRectToVisible(stockTable.getCellRect(row, 0, true));
                stockTable.requestFocusInWindow();
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "NOT FOUND", "SEARCH MEDICINE", JOptionPane.ERROR_MESSAGE);
    }

    // MEDICINE ID GENERATOR: Read the saved counter and existing IDs so deleted IDs are never reused.
    private String nextMedicineId() throws IOException {
        Files.createDirectories(DATA_DIR);
        int max = 0;
        if (Files.exists(MEDICINE_ID_COUNTER_FILE)) {
            String value = new String(Files.readAllBytes(MEDICINE_ID_COUNTER_FILE), StandardCharsets.UTF_8).trim();
            if (!value.isEmpty()) max = Integer.parseInt(value);
        }
        List<Medicine> active = readMedicines();
        for (Medicine m : active) max = Math.max(max, extractMedicineSequence(m.id));
        for (RecycleRecord r : readRecycleBin()) max = Math.max(max, extractMedicineSequence(r.medicine.id));
        return String.format(Locale.ROOT, "MED-%03d", max + 1);
    }

    // Save the last issued ID number so a deleted Medicine ID is not generated again after restart.
    private void saveMedicineIdCounter(String id) throws IOException {
        int sequence = extractMedicineSequence(id);
        if (sequence > 0) Files.write(MEDICINE_ID_COUNTER_FILE, String.valueOf(sequence).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static int extractMedicineSequence(String id) {
        if (id == null || !id.toUpperCase(Locale.ROOT).startsWith("MED-")) return 0;
        try { return Integer.parseInt(id.substring(4)); } catch (NumberFormatException ex) { return 0; }
    }

    // Read all deleted medicines from the Recycle Bin storage file.
    private List<RecycleRecord> readRecycleBin() throws IOException {
        List<RecycleRecord> records = new ArrayList<>();
        if (!Files.exists(RECYCLE_FILE)) return records;
        int lineNo = 0;
        for (String line : Files.readAllLines(RECYCLE_FILE, StandardCharsets.UTF_8)) {
            lineNo++;
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 11) throw new IOException("Invalid Recycle Bin record at line " + lineNo + ".");
            try {
                LocalDateTime deletedAt = LocalDateTime.parse(f[0]);
                int qty = Integer.parseInt(f[6]); double unitMrp = Double.parseDouble(f[8]);
                Medicine m = new Medicine(f[1], f[2], f[3], f[4], f[5], qty, unitMrp, f[9], f[7]);
                records.add(new RecycleRecord(m, deletedAt));
            } catch (RuntimeException ex) { throw new IOException("Invalid Recycle Bin data at line " + lineNo + ".", ex); }
        }
        return records;
    }

    // Save the complete Recycle Bin list in a dedicated file; stock.txt remains unchanged in format.
    private void writeRecycleBin(List<RecycleRecord> records) throws IOException {
        Files.createDirectories(DATA_DIR);
        List<String> lines = new ArrayList<>();
        for (RecycleRecord r : records) {
            Medicine m = r.medicine;
            lines.add(join(r.deletedAt.toString(), m.id, m.name, m.form, m.category, m.company,
                    String.valueOf(m.quantity), m.shelf, money(m.mrp), m.expiry, "RECYCLED"));
        }
        Files.write(RECYCLE_FILE, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private List<Medicine> readMedicines() throws IOException {
        Files.createDirectories(DATA_DIR);
        Map<String, String[]> extra = readExtra();
        List<Medicine> result = new ArrayList<>();
        if (!Files.exists(STOCK_FILE)) return result;
        int lineNo = 0;
        for (String line : Files.readAllLines(STOCK_FILE, StandardCharsets.UTF_8)) {
            lineNo++;
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 7) throw new IOException("stock.txt line " + lineNo + " must contain exactly 7 fields for Sales Management compatibility.");
            try {
                int qty = Integer.parseInt(f[3].trim());
                double unitMrp = Double.parseDouble(f[4].trim());
                LocalDate.parse(f[5].trim(), DATE_FMT);
                String[] metadata = extra.get(f[0].trim().toLowerCase(Locale.ROOT));
                String category = metadata == null ? "Other" : metadata[0];
                String company = metadata == null ? "Other" : metadata[1];
                result.add(new Medicine(f[0].trim(), f[1].trim(), f[2].trim(), category, company, qty, unitMrp, f[5].trim(), f[6].trim()));
            } catch (NumberFormatException | DateTimeParseException ex) {
                throw new IOException("Invalid quantity, MRP or expiry date in stock.txt at line " + lineNo + ".", ex);
            }
        }
        return result;
    }

    private Map<String, String[]> readExtra() throws IOException {
        Map<String, String[]> map = new HashMap<>();
        if (!Files.exists(EXTRA_FILE)) return map;
        int lineNo = 0;
        for (String line : Files.readAllLines(EXTRA_FILE, StandardCharsets.UTF_8)) {
            lineNo++;
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length >= 3) map.put(f[0].trim().toLowerCase(Locale.ROOT), new String[]{f[1].trim(), f[2].trim()});
            else throw new IOException("Invalid stock_extra.txt line " + lineNo + ".");
        }
        return map;
    }

    private void writeMedicines(List<Medicine> medicines) throws IOException {
        Files.createDirectories(DATA_DIR);
        List<String> stockLines = new ArrayList<>();
        List<String> extraLines = new ArrayList<>();
        for (Medicine m : medicines) {
            stockLines.add(join(m.legacyFields()));
            extraLines.add(join(m.id, m.category, m.company));
        }
        Path stockTemp = DATA_DIR.resolve("stock_management_stock.tmp");
        Path extraTemp = DATA_DIR.resolve("stock_management_extra.tmp");
        Files.write(stockTemp, stockLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(extraTemp, extraLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        replaceFile(stockTemp, STOCK_FILE);
        replaceFile(extraTemp, EXTRA_FILE);
    }

    private void replaceFile(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static Medicine findById(List<Medicine> list, String id) {
        if (id == null) return null;
        for (Medicine m : list) if (m.id.equalsIgnoreCase(id)) return m;
        return null;
    }

    private static String join(String... fields) {
        StringJoiner j = new StringJoiner("|");
        for (String f : fields) j.add(f == null ? "" : f.replace('|', '/').replace('\n', ' ').replace('\r', ' '));
        return j.toString();
    }

    private static boolean startsNegative(String value) {
        return value.startsWith("-") || value.startsWith("−") || value.startsWith("–") || value.startsWith("—");
    }

    private static String money(double value) { return String.format(Locale.US, "%.2f", value); }

    private void errorDialog(String message) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBackground(new Color(255, 225, 225));
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(190, 25, 35), 2), new EmptyBorder(14, 18, 14, 18)));
        JLabel title = new JLabel("FAILED!", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 19)); title.setForeground(new Color(170, 0, 0));
        JLabel msg = new JLabel("<html><center>" + message.replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
        msg.setFont(new Font("Arial", Font.PLAIN, 14));
        p.add(title, BorderLayout.NORTH); p.add(msg, BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, p, "Validation Error", JOptionPane.PLAIN_MESSAGE);
    }

    private void successDialog(String message) { JOptionPane.showMessageDialog(this, message, "SUCCESSFUL!", JOptionPane.INFORMATION_MESSAGE); }
    private void infoDialog(String message) { JOptionPane.showMessageDialog(this, message, "Stock Management", JOptionPane.INFORMATION_MESSAGE); }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Stock Management - Error", JOptionPane.ERROR_MESSAGE); }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new StockManagement().setVisible(true)); }
}