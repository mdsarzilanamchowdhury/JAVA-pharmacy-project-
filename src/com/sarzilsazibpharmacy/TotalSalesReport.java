package com.sarzilsazibpharmacy;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
/**
 * Total Sales Report for Sarzil-Sazib-Pharmacy.
 *
 * Reads the same data/sales.txt and data/stock.txt files as SalesManagement.
 * sales.txt field layout (21 fields):
 * 0 Sale ID, 1 Date, 2 Time, 3 Sales By, 4 Customer ID, 5 Customer Name,
 * 6 Phone, 7 Address, 8 Medicine ID, 9 Medicine Name, 10 Form, 11 Quantity,
 * 12 MRP, 13 Selling Price, 14 Discount %, 15 Gross, 16 Discount amount,
 * 17 Net, 18 Paid, 19 Due, 20 Expiry.
 */
public class TotalSalesReport extends JFrame {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path SALES_FILE = DATA_DIR.resolve("sales.txt");
    private static final Path STOCK_FILE = DATA_DIR.resolve("stock.txt");
    private static final Path SALES_TEMP = DATA_DIR.resolve("sales_report_temp.txt");
    private static final Path STOCK_TEMP = DATA_DIR.resolve("stock_report_temp.txt");
    private static final Path DUE_FILE = DATA_DIR.resolve("due_collection.txt");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private final JTextField searchField = new JTextField(18);
    private final JTextField fromDateField = new JTextField(10);
    private final JTextField toDateField = new JTextField(10);
    private final JLabel grossValue = new JLabel("Tk. 0.00");
    private final JLabel discountValue = new JLabel("Tk. 0.00");
    private final JLabel netValue = new JLabel("Tk. 0.00");
    private final JLabel paidValue = new JLabel("Tk. 0.00");
    private final JLabel dueValue = new JLabel("Tk. 0.00");
    private final JLabel invoicesValue = new JLabel("0");
    private final JLabel quantityValue = new JLabel("0");

    private final String[] columns = {
        "Sale ID", "Date", "Time", "Sales By", "Customer ID", "Customer Name", "Customer Phone",
        "Medicine ID", "Medicine Name", "Form", "Expiry Date", "Quantity (pcs.)", "MRP (Tk.)",
        "TOTAL (WITHOUT DISCOUNT)", "Discount (%)", "Discount (Tk.)", "Net Price (After Discount)",
        "Summation (Net Price)", "Payable (Round Figure)", "Paid (Tk.)", "Due (Tk.)"
    };
    private final DefaultTableModel model = new DefaultTableModel(columns, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private List<SaleLine> allLines = new ArrayList<>();

    public TotalSalesReport() {
        setTitle("Sarzil-Sazib-Pharmacy - Total Sales Report");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1450, 800);
        setMinimumSize(new Dimension(1050, 650));
        setLocationRelativeTo(null);
        buildUI();
        loadSales();
        applyFilters();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        root.setBackground(new Color(240, 248, 255));

        JLabel heading = new JLabel("TOTAL SALES REPORT", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 28));
        heading.setForeground(new Color(25, 70, 150));
        root.add(heading, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);

        JPanel filters = new JPanel(new BorderLayout(12, 8));
        filters.setBackground(Color.WHITE);
        TitledBorder filterBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(45, 105, 210), 2),
                "SEARCH / FILTER DATE",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 22), Color.BLACK);
        filters.setBorder(BorderFactory.createCompoundBorder(filterBorder,
                BorderFactory.createEmptyBorder(18, 18, 14, 18)));

        JPanel filterChoices = new JPanel(new GridBagLayout());
        filterChoices.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setOpaque(false);
        JLabel searchTitle = new JLabel("SEARCH", SwingConstants.CENTER);
        searchTitle.setFont(new Font("Arial", Font.BOLD, 18));
        searchTitle.setForeground(new Color(0, 170, 230));
        JLabel searchLabel = new JLabel("Sale ID / Customer Name / Customer ID / Customer Phone :", SwingConstants.LEFT);
        searchLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setFont(new Font("Arial", Font.PLAIN, 16));
        searchField.setPreferredSize(new Dimension(420, 42));
        GridBagConstraints sg = new GridBagConstraints();
        sg.gridx = 0; sg.gridy = 0; sg.gridwidth = 1; sg.fill = GridBagConstraints.HORIZONTAL;
        sg.insets = new Insets(3, 3, 10, 3); sg.weightx = 1;
        searchPanel.add(searchTitle, sg);
        sg.gridy = 1; sg.insets = new Insets(3, 3, 5, 3);
        searchPanel.add(searchLabel, sg);
        sg.gridy = 2; sg.insets = new Insets(3, 3, 3, 3);
        searchPanel.add(searchField, sg);

        JLabel orLabel = new JLabel("OR", SwingConstants.CENTER);
        orLabel.setFont(new Font("Arial", Font.BOLD, 30));
        orLabel.setForeground(Color.RED);

        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.setOpaque(false);
        JLabel dateTitle = new JLabel("FILTER DATE", SwingConstants.CENTER);
        dateTitle.setFont(new Font("Arial", Font.BOLD, 18));
        dateTitle.setForeground(new Color(0, 170, 230));
        JLabel fromLabel = new JLabel("FROM (DD-MM-YYYY) :", SwingConstants.CENTER);
        JLabel toLabel = new JLabel("TO (DD-MM-YYYY) :", SwingConstants.CENTER);
        fromLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        toLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        fromDateField.setFont(new Font("Arial", Font.PLAIN, 16));
        toDateField.setFont(new Font("Arial", Font.PLAIN, 16));
        fromDateField.setPreferredSize(new Dimension(170, 42));
        toDateField.setPreferredSize(new Dimension(170, 42));
        GridBagConstraints dg = new GridBagConstraints();
        dg.insets = new Insets(3, 8, 8, 8); dg.fill = GridBagConstraints.HORIZONTAL;
        dg.gridx = 0; dg.gridy = 0; dg.gridwidth = 2; dg.weightx = 1;
        datePanel.add(dateTitle, dg);
        dg.gridwidth = 1; dg.gridy = 1; dg.gridx = 0;
        datePanel.add(fromLabel, dg);
        dg.gridx = 1;
        datePanel.add(toLabel, dg);
        dg.gridy = 2; dg.gridx = 0; dg.insets = new Insets(3, 8, 3, 8);
        datePanel.add(fromDateField, dg);
        dg.gridx = 1;
        datePanel.add(toDateField, dg);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.48;
        filterChoices.add(searchPanel, gbc);
        gbc.gridx = 1; gbc.weightx = 0.04;
        filterChoices.add(orLabel, gbc);
        gbc.gridx = 2; gbc.weightx = 0.48;
        filterChoices.add(datePanel, gbc);
        filters.add(filterChoices, BorderLayout.CENTER);

        JButton search = button("SEARCH", new Color(0, 174, 239), Color.BLACK);
        JButton reset = button("RESET FILTER", Color.YELLOW, Color.BLACK);
        JButton medicineReportButton = button("SALE REPORT OF MEDICINE", Color.BLACK, Color.WHITE);
        JPanel filterButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        filterButtons.setOpaque(false);
        filterButtons.add(search);
        filterButtons.add(reset);
        filterButtons.add(medicineReportButton);
        filters.add(filterButtons, BorderLayout.SOUTH);
        medicineReportButton.addActionListener(e -> new MedicineSalesReportWindow().setVisible(true));
        search.addActionListener(e -> applyFilters());
        searchField.addActionListener(e -> applyFilters());
        reset.addActionListener(e -> {
            searchField.setText(""); fromDateField.setText(""); toDateField.setText("");
            applyFilters();
        });

        styleTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(25, 80, 180), 2));

        JPanel summary = new JPanel(new GridLayout(1, 7, 6, 0));
        summary.setOpaque(false);
        summary.add(summaryBox("TOTAL (EXCEPT DISCOUNT)", grossValue, new Color(30, 80, 170)));
        summary.add(summaryBox("TOTAL DISCOUNT", discountValue, new Color(180, 80, 20)));
        summary.add(summaryBox("TOTAL PAYABLE", netValue, new Color(0, 120, 70)));
        summary.add(summaryBox("TOTAL PAID", paidValue, new Color(25, 100, 160)));
        summary.add(summaryBox("TOTAL DUE", dueValue, new Color(190, 40, 40)));
        summary.add(summaryBox("TOTAL QUANTITY (PCS.)", quantityValue, new Color(0, 120, 120)));
        summary.add(summaryBox("TOTAL INVOICES", invoicesValue, new Color(95, 65, 155)));

        JPanel upper = new JPanel(new BorderLayout(6, 6));
        upper.setOpaque(false);
        upper.add(filters, BorderLayout.NORTH);
        upper.add(scroll, BorderLayout.CENTER);
        center.add(upper, BorderLayout.CENTER);
        center.add(summary, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 5));
        actions.setOpaque(false);
        JButton delete = button("DELETE SALE", new Color(210, 45, 45), Color.WHITE);
        JButton invoice = button("VIEW INVOICE", new Color(30, 145, 100), Color.WHITE);
        JButton refresh = button("REFRESH", new Color(244, 177, 131), Color.BLACK);
        JButton export = button("EXPORT CSV", new Color(255, 105, 180), Color.BLACK);
        JButton print = button("PRINT REPORT", new Color(95, 95, 110), Color.WHITE);
        JButton back = button("BACK", new Color(190, 220, 245), Color.BLACK);
        actions.add(delete); actions.add(invoice); actions.add(refresh); actions.add(export); actions.add(print); actions.add(back);
        delete.addActionListener(e -> deleteSelectedSale());
        invoice.addActionListener(e -> viewSelectedInvoice());
        refresh.addActionListener(e -> { loadSales(); applyFilters(); });
        export.addActionListener(e -> exportCsv());
        print.addActionListener(e -> {
            try { table.print(); }
            catch (java.awt.print.PrinterException ex) { error("Could not print report: " + ex.getMessage()); }
        });
        back.addActionListener(e -> dispose());
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void styleTable() {
        table.setFont(new Font("Arial", Font.PLAIN, 14));
        table.setRowHeight(28);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(210, 220, 230));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(70, 130, 180));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        int[] widths = {100, 105, 95, 125, 115, 160, 145, 100, 165, 100, 110, 110, 105, 205, 105, 125, 205, 190, 205, 115, 115};
        FontMetrics headerMetrics = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont());
        for (int i = 0; i < widths.length; i++) {
            int headerWidth = headerMetrics.stringWidth(table.getColumnName(i)) + 28;
            int finalWidth = Math.max(widths[i], headerWidth);
            table.getColumnModel().getColumn(i).setPreferredWidth(finalWidth);
            table.getColumnModel().getColumn(i).setMinWidth(finalWidth);
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    private JPanel summaryBox(String title, JLabel value, Color color) {
        JPanel p = new JPanel(new BorderLayout(3, 4));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(25, 80, 180), 2),
                BorderFactory.createEmptyBorder(6, 2, 6, 2)));
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(color);
        label.setToolTipText(title);
        value.setFont(new Font("Arial", Font.BOLD, 17));
        value.setForeground(color);
        value.setHorizontalAlignment(SwingConstants.CENTER);
        p.add(label, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 14));
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(Math.max(150, text.length() * 10 + 38), 44));
        b.setMinimumSize(new Dimension(Math.max(150, text.length() * 10 + 38), 44));
        return b;
    }

    private static double totalDueCollected(String saleId) {
        if (!Files.exists(DUE_FILE)) return 0.0;
        double sum = 0.0;
        try {
            for (String line : Files.readAllLines(DUE_FILE, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) continue;
                String[] f = line.split("\\|", -1);
                // Collection ID | Sale ID | Customer ID | Customer Name | Phone | Date | Amount | Remaining
                if (f.length >= 8 && f[1].trim().equalsIgnoreCase(saleId)) sum += num(f[6]);
            }
        } catch (IOException ignored) { }
        return sum;
    }

    private void loadSales() {
        allLines.clear();
        try {
            Files.createDirectories(DATA_DIR);
            if (!Files.exists(SALES_FILE)) Files.createFile(SALES_FILE);
            int lineNo = 0;
            for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                String[] f = line.split("\\|", -1);
                if (f.length < 21) {
                    throw new IOException("Invalid sales.txt record at line " + lineNo
                            + ". Expected 21 fields, found " + f.length + ".");
                }
                allLines.add(new SaleLine(f));
            }
        } catch (IOException ex) {
            error("Could not read data/sales.txt.\n" + ex.getMessage());
        }
    }

    private void applyFilters() {
        model.setRowCount(0);
        String query = searchField.getText().trim();
        String queryLower = query.toLowerCase(Locale.ROOT);
        String normalizedPhoneQuery = normalizePhone(query);
        LocalDate from = null, to = null;
        try {
            if (!fromDateField.getText().trim().isEmpty()) from = LocalDate.parse(fromDateField.getText().trim(), DATE_FMT);
            if (!toDateField.getText().trim().isEmpty()) to = LocalDate.parse(toDateField.getText().trim(), DATE_FMT);
            if (from != null && to != null && from.isAfter(to)) {
                error("From Date cannot be later than To Date."); return;
            }
        } catch (DateTimeParseException ex) {
            error("Date must use DD-MM-YYYY format."); return;
        }

        Map<String, List<SaleLine>> groups = new LinkedHashMap<>();
        for (SaleLine line : allLines) groups.computeIfAbsent(line.saleId, k -> new ArrayList<>()).add(line);

        double grossSum = 0, discountSum = 0, payableSum = 0, paidSum = 0, dueSum = 0, totalQty = 0;
        int invoiceCount = 0;
        for (List<SaleLine> rows : groups.values()) {
            SaleLine first = rows.get(0);
            LocalDate saleDate;
            try { saleDate = LocalDate.parse(first.date, DATE_FMT); }
            catch (DateTimeParseException ex) { continue; }
            if (from != null && saleDate.isBefore(from)) continue;
            if (to != null && saleDate.isAfter(to)) continue;

            boolean groupMatches = query.isEmpty();
            if (!groupMatches) {
                for (SaleLine r : rows) {
                    String storedPhone = normalizePhone(r.phone);
                    if (contains(r.saleId, queryLower) || contains(r.customerId, queryLower)
                            || contains(r.customerName, queryLower)
                            || (!normalizedPhoneQuery.isEmpty() && storedPhone.equals(normalizedPhoneQuery))) {
                        groupMatches = true; break;
                    }
                }
            }
            if (!groupMatches) continue;

            invoiceCount++;
            double saleGross = 0, saleDiscount = 0, saleNetExact = 0, saleQty = 0;
            for (SaleLine r : rows) {
                double qty = num(r.fields[11]);
                double mrp = num(r.fields[12]);
                double lineGross = qty * mrp;
                double lineDiscount = num(r.fields[16]);
                saleGross += lineGross;
                saleDiscount += lineDiscount;
                saleNetExact += (lineGross - lineDiscount);
                saleQty += qty;
            }
            double roundedPayable = Math.round(saleNetExact);
            double collected = totalDueCollected(first.saleId);
            double salePaid = num(first.fields[18]) + collected;
            double saleDue = Math.max(0.0, num(first.fields[19]) - collected);
            grossSum += saleGross; discountSum += saleDiscount; payableSum += roundedPayable;
            paidSum += salePaid; dueSum += saleDue; totalQty += saleQty;

            for (int i = 0; i < rows.size(); i++) {
                SaleLine r = rows.get(i);
                boolean isFirst = i == 0;
                double qty = num(r.fields[11]);
                double mrp = num(r.fields[12]);
                double lineGross = qty * mrp;
                double lineDiscount = num(r.fields[16]);
                double netAfterDiscount = lineGross - lineDiscount;
                model.addRow(new Object[] {
                    isFirst ? r.saleId : "", isFirst ? r.date : "", isFirst ? r.time : "",
                    isFirst ? r.salesBy : "", isFirst ? r.customerId : "", isFirst ? r.customerName : "",
                    isFirst ? r.phone : "", r.medicineId, r.medicineName, r.form, r.fields[20], r.quantity,
                    "Tk. " + money(mrp), "Tk. " + money(lineGross), r.discountPercent + "%",
                    "Tk. " + money(lineDiscount), "Tk. " + money(netAfterDiscount),
                    isFirst ? "Tk. " + money(saleNetExact) : "",
                    isFirst ? "Tk. " + money(roundedPayable) : "",
                    isFirst ? "Tk. " + money(salePaid) : "", isFirst ? "Tk. " + money(saleDue) : ""
                });
            }
        }
        grossValue.setText("Tk. " + money(grossSum));
        discountValue.setText("Tk. " + money(discountSum));
        netValue.setText("Tk. " + money(payableSum));
        paidValue.setText("Tk. " + money(paidSum));
        dueValue.setText("Tk. " + money(dueSum));
        quantityValue.setText(money(totalQty));
        invoicesValue.setText(String.valueOf(invoiceCount));
    }

    private static String normalizePhone(String value) {
        if (value == null) return "";
        String digits = value.trim().replaceAll("\\D", "");
        if (digits.startsWith("88") && digits.length() == 13) digits = digits.substring(2);
        return digits.length() == 11 ? digits : "";
    }

    private void deleteSelectedSale() {
        int row = table.getSelectedRow();
        if (row < 0) {
            error("Select a row belonging to the Sale ID you want to delete."); return;
        }
        String saleId = String.valueOf(model.getValueAt(row, 0)).trim();
        if (saleId.isEmpty()) {
            // Rows after the first medicine line intentionally have blank Sale ID cells.
            int r = row;
            while (r >= 0 && String.valueOf(model.getValueAt(r, 0)).trim().isEmpty()) r--;
            if (r >= 0) saleId = String.valueOf(model.getValueAt(r, 0)).trim();
        }
        if (saleId.isEmpty()) { error("Could not identify the Sale ID."); return; }

        int answer = JOptionPane.showConfirmDialog(this,
                "ARE YOU SURE YOU WANT TO DELETE SALE ID " + saleId + "?\n"
                + "All medicine rows for this Sale ID will be deleted and sold quantities restored to stock.",
                "Confirm Delete Sale", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;

        try {
            List<String> sales = Files.exists(SALES_FILE)
                    ? Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8) : new ArrayList<>();
            List<String> keep = new ArrayList<>();
            Map<String, Integer> restore = new LinkedHashMap<>();
            int removed = 0;
            for (String line : sales) {
                if (line.trim().isEmpty()) continue;
                String[] f = line.split("\\|", -1);
                if (f.length >= 21 && f[0].equalsIgnoreCase(saleId)) {
                    int qty = Integer.parseInt(f[11].trim());
                    restore.merge(f[8].trim(), qty, Integer::sum);
                    removed++;
                } else keep.add(line);
            }
            if (removed == 0) { error("NO SALES RECORDS AVAILABLE for " + saleId); return; }

            List<String> stock = Files.exists(STOCK_FILE)
                    ? Files.readAllLines(STOCK_FILE, StandardCharsets.UTF_8) : new ArrayList<>();
            List<String> updatedStock = new ArrayList<>();
            StringBuilder restoredMessage = new StringBuilder();
            for (String line : stock) {
                if (line.trim().isEmpty()) continue;
                String[] f = line.split("\\|", -1);
                if (f.length != 7) throw new IOException("Invalid stock.txt record; expected 7 fields.");
                Integer add = restore.remove(f[0].trim());
                if (add != null) {
                    int newQty = Integer.parseInt(f[3].trim()) + add;
                    f[3] = String.valueOf(newQty);
                    restoredMessage.append("NEW RESTORED QUANTITY OF ").append(f[1])
                            .append(" IS\n+").append(add).append(" pcs (now ").append(newQty).append(" pcs)\n\n");
                }
                updatedStock.add(String.join("|", f));
            }
            if (!restore.isEmpty()) throw new IOException("Medicine IDs missing from stock.txt: " + restore.keySet());

            Files.write(SALES_TEMP, keep, StandardCharsets.UTF_8);
            Files.write(STOCK_TEMP, updatedStock, StandardCharsets.UTF_8);
            try {
                Files.move(STOCK_TEMP, STOCK_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(STOCK_TEMP, STOCK_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(SALES_TEMP, SALES_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(SALES_TEMP, SALES_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
            showRestoredDialog(saleId, restoredMessage.toString());
            loadSales();
            applyFilters();
        } catch (Exception ex) {
            error("Sale could not be deleted.\n" + ex.getMessage());
        }
    }

    private void showRestoredDialog(String saleId, String lines) {
        JDialog d = new JDialog(this, "Sale Deleted - Stock Restored", true);
        d.setSize(720, 480);
        d.setMinimumSize(new Dimension(650, 400));
        d.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        panel.setBackground(new Color(235, 248, 255));
        JLabel title = new JLabel("SALE ID " + saleId + " DELETED SUCCESSFULLY", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 23));
        title.setForeground(new Color(0, 115, 60));
        panel.add(title, BorderLayout.NORTH);
        JTextPane details = new JTextPane();
        details.setEditable(false);
        details.setBackground(Color.WHITE);
        details.setBorder(BorderFactory.createLineBorder(new Color(30, 90, 190), 2));
        details.setFont(new Font("Arial", Font.BOLD, 17));
        details.setText("STOCK QUANTITY RESTORED:\n\n" + lines);
        panel.add(new JScrollPane(details), BorderLayout.CENTER);
        JButton ok = button("OK", new Color(45, 105, 210), Color.WHITE);
        ok.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(); south.setOpaque(false); south.add(ok);
        panel.add(south, BorderLayout.SOUTH);
        d.setContentPane(panel);
        d.setVisible(true);
    }

    private void viewSelectedInvoice() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a row from the invoice you want to view."); return; }
        String saleId = String.valueOf(model.getValueAt(row, 0)).trim();
        if (saleId.isEmpty()) {
            int r = row;
            while (r >= 0 && String.valueOf(model.getValueAt(r, 0)).trim().isEmpty()) r--;
            if (r >= 0) saleId = String.valueOf(model.getValueAt(r, 0)).trim();
        }
        if (saleId.isEmpty()) { error("Could not identify the Sale ID."); return; }
        List<SaleLine> rows = new ArrayList<>();
        for (SaleLine line : allLines) if (line.saleId.equalsIgnoreCase(saleId)) rows.add(line);
        if (rows.isEmpty()) { error("No saved invoice found for " + saleId); return; }

        SaleLine first = rows.get(0);
        double gross = 0, discount = 0, payable = 0;
        DefaultTableModel invoiceModel = new DefaultTableModel(
                new String[]{"Serial No.", "Medicine Name", "Expiry Date", "Quantity", "MRP (Tk.)", "Discount (%)", "Payable (Tk.)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        int serial = 1;
        for (SaleLine r : rows) {
            gross += num(r.fields[15]); discount += num(r.fields[16]); payable += num(r.fields[17]);
            invoiceModel.addRow(new Object[]{serial++, r.medicineName, r.fields[20], r.quantity,
                    "Tk. " + money(num(r.fields[12])), r.discountPercent + "%", "Tk. " + money(num(r.fields[17]))});
        }
        double collected = totalDueCollected(saleId);
        double originalPaid = num(first.fields[18]), originalDue = num(first.fields[19]);
        double paid = originalPaid + collected, due = Math.max(0.0, originalDue - collected);
        double rounded = originalPaid + originalDue;

        JFrame invoice = new JFrame("INVOICE - " + saleId);
        invoice.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        invoice.setSize(1120, 800); invoice.setMinimumSize(new Dimension(1000, 720)); invoice.setLocationRelativeTo(this);
        JPanel page = new JPanel(new BorderLayout(8, 8)); page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));
        JPanel header = new JPanel(new GridLayout(4, 1, 2, 2)); header.setBackground(Color.WHITE);
        JLabel h1 = new JLabel("INVOICE", SwingConstants.CENTER); h1.setFont(new Font("Arial", Font.BOLD, 20)); h1.setForeground(new Color(25,70,150));
        JLabel h2 = new JLabel("SARZIL SAZIB PHARMACY", SwingConstants.CENTER); h2.setFont(new Font("Arial", Font.BOLD, 24));
        JLabel h3 = new JLabel("PHONE NUMBER : 01772-4646", SwingConstants.CENTER); h3.setFont(new Font("Arial", Font.BOLD, 15));
        JLabel pub = new JLabel("PUBLISHING DATE = " + LocalDate.now().format(DATE_FMT), SwingConstants.CENTER);
        pub.setFont(new Font("Arial", Font.BOLD, 19)); pub.setForeground(Color.RED); pub.setOpaque(true); pub.setBackground(new Color(255,245,245));
        pub.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED,2), BorderFactory.createEmptyBorder(4,8,4,8)));
        header.add(h1); header.add(h2); header.add(h3); header.add(pub); page.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(); center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS)); center.setBackground(Color.WHITE);
        JPanel info = new JPanel(new GridLayout(1,2,24,4)); info.setBackground(Color.WHITE);
        JPanel sale = new JPanel(new GridLayout(5,1)); sale.setBackground(Color.WHITE);
        JPanel customer = new JPanel(new GridLayout(5,1)); customer.setBackground(Color.WHITE);
        sale.add(invoiceLabel("SALE INFORMATION", new Color(0,145,55), true));
        sale.add(invoiceLabel("Sale ID: " + first.saleId, Color.BLACK, false));
        sale.add(invoiceLabel("Selling Date: " + first.date, Color.RED, false));
        sale.add(invoiceLabel("Time: " + first.time, Color.BLACK, false));
        sale.add(invoiceLabel("Sales By: " + first.salesBy, Color.BLACK, false));
        customer.add(invoiceLabel("CUSTOMER INFORMATION", new Color(0,145,55), true));
        customer.add(invoiceLabel("Customer ID: " + first.customerId, Color.BLACK, false));
        customer.add(invoiceLabel("Customer Name: " + first.customerName, Color.BLACK, false));
        customer.add(invoiceLabel("Phone: " + first.phone, Color.BLACK, false));
        customer.add(invoiceLabel("Address: " + first.fields[7], Color.BLACK, false));
        info.add(sale); info.add(customer); info.setMaximumSize(new Dimension(Integer.MAX_VALUE,105)); center.add(info); center.add(Box.createVerticalStrut(5));

        JTable invoiceTable = new JTable(invoiceModel); invoiceTable.setRowHeight(25); invoiceTable.setFont(new Font("Arial",Font.PLAIN,12));
        invoiceTable.getTableHeader().setFont(new Font("Arial",Font.BOLD,13)); invoiceTable.getTableHeader().setBackground(new Color(70,130,180));
        invoiceTable.getTableHeader().setForeground(Color.WHITE); invoiceTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane tableScroll = new JScrollPane(invoiceTable); tableScroll.setPreferredSize(new Dimension(1040,155)); tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE,155));
        center.add(tableScroll); center.add(Box.createVerticalStrut(6));

        JPanel details = new JPanel(new BorderLayout(24,4)); details.setBackground(Color.WHITE);
        JPanel totals = new JPanel(new GridLayout(6,1,2,2)); totals.setBackground(Color.WHITE);
        String[] totalTexts = {"TOTAL (EXCEPT DISCOUNT): Tk. " + money(gross), "DISCOUNT: Tk. " + money(discount),
            "PAYABLE: Tk. " + money(payable), "ROUND FIGURE: Tk. " + money(rounded), "PAID: Tk. " + money(paid), "DUE: Tk. " + money(due)};
        for (String t : totalTexts) totals.add(invoiceLabel(t, t.startsWith("DUE:") ? Color.RED : Color.BLACK, false));
        totals.setPreferredSize(new Dimension(390,130)); details.add(totals, BorderLayout.WEST);
        JPanel history = new JPanel(); history.setLayout(new BoxLayout(history,BoxLayout.Y_AXIS)); history.setBackground(Color.WHITE);
        boolean foundCollection = false;
        try {
            if (Files.exists(DUE_FILE)) for (String line : Files.readAllLines(DUE_FILE, StandardCharsets.UTF_8)) {
                String[] d = line.split("\\|", -1);
                if (d.length >= 8 && d[1].trim().equalsIgnoreCase(saleId)) {
                    foundCollection = true;
                    history.add(invoiceLabel("DUE COLLECTION DATE: " + d[5], Color.RED, false));
                    history.add(invoiceLabel("DUE COLLECTION AMOUNT (TK.): " + money(num(d[6])), Color.RED, false));
                    history.add(invoiceLabel("REMAINING DUE (TK.): " + money(num(d[7])), Color.RED, false));
                    history.add(Box.createVerticalStrut(5));
                }
            }
        } catch (IOException ex) { history.add(invoiceLabel("Could not load due collection history.", Color.RED, false)); }
        if (!foundCollection) history.add(invoiceLabel("NO DUE COLLECTIONS RECORDED", Color.GRAY, false));
        JScrollPane historyScroll = new JScrollPane(history); historyScroll.setPreferredSize(new Dimension(470,150));
        historyScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); details.add(historyScroll, BorderLayout.CENTER);
        center.add(details); page.add(center, BorderLayout.CENTER);
        JPanel footer = new JPanel(new GridLayout(2,1,2,2)); footer.setBackground(Color.WHITE);
        JLabel f1 = new JLabel("SOLD MEDICINES ARE NOT RETURNABLE.", SwingConstants.CENTER); f1.setFont(new Font("Arial",Font.BOLD,15));
        JLabel f2 = new JLabel("WE APPRECIATE YOUR VISIT", SwingConstants.CENTER); f2.setFont(new Font("Arial",Font.BOLD,15));
        footer.add(f1); footer.add(f2); page.add(footer,BorderLayout.SOUTH);
        invoice.setContentPane(page); invoice.setVisible(true);
    }

    private JLabel invoiceLabel(String text, Color color, boolean heading) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, heading ? 14 : 13));
        label.setForeground(color);
        return label;
    }

    private final class MedicineSalesReportWindow extends JFrame {
        private final JComboBox<String> medicineNameCombo = new JComboBox<>();
        private final JTextField fromField = new JTextField(10);
        private final JTextField toField = new JTextField(10);
        private final JLabel grossTotalLabel = new JLabel("Tk. 0.00", SwingConstants.CENTER);
        private final JLabel netTotalLabel = new JLabel("Tk. 0.00", SwingConstants.CENTER);
        private final String[] reportColumns = {"Medicine Name (Primary Key)", "Sale ID", "Date", "Time", "Sales By",
            "Customer ID", "Customer Name", "Medicine ID", "Medicine Name", "Form", "Expiry Date", "Quantity (pcs.)",
            "MRP (Tk.)", "TOTAL (WITHOUT DISCOUNT)", "Discount (%)", "Discount (Tk.)", "Net Price (After Discount)"};
        private final DefaultTableModel reportModel = new DefaultTableModel(reportColumns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        private final JTable reportTable = new JTable(reportModel);

        MedicineSalesReportWindow() {
            setTitle("SALE REPORT OF MEDICINE");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(1500, 850); setMinimumSize(new Dimension(1100, 700)); setLocationRelativeTo(TotalSalesReport.this);
            buildReportUI();
        }

        private void buildReportUI() {
            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            root.setBackground(new Color(240, 248, 255));
            JPanel searchBox = new JPanel(new BorderLayout(8, 12)); searchBox.setBackground(Color.WHITE);
            TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(45,105,210), 2),
                "SALE REPORT OF MEDICINE", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 24), new Color(0, 170, 90));
            searchBox.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(18,18,14,18)));
            JPanel fields = new JPanel(new GridBagLayout()); fields.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(8,10,8,10); c.fill = GridBagConstraints.HORIZONTAL;
            JLabel medicineLabel = new JLabel("MEDICINE NAME :");
            medicineLabel.setFont(new Font("Arial", Font.BOLD, 16));
            JLabel fromLabel = new JLabel("FROM (DD-MM-YYYY)");
            JLabel toLabel = new JLabel("TO (DD-MM-YYYY)");
            fromLabel.setFont(new Font("Arial", Font.BOLD, 16));
            toLabel.setFont(new Font("Arial", Font.BOLD, 16));

            medicineNameCombo.setFont(new Font("Arial", Font.PLAIN, 16));
            medicineNameCombo.setEditable(false);
            medicineNameCombo.setPreferredSize(new Dimension(300, 38));
            fromField.setFont(new Font("Arial", Font.PLAIN, 16));
            toField.setFont(new Font("Arial", Font.PLAIN, 16));
            fromField.setPreferredSize(new Dimension(180, 38));
            toField.setPreferredSize(new Dimension(180, 38));

            c.gridx=0; c.gridy=0; c.weightx=0; fields.add(medicineLabel,c);
            c.gridx=1; c.weightx=0.5; fields.add(medicineNameCombo,c);
            c.gridx=2; c.weightx=0; fields.add(fromLabel,c);
            c.gridx=3; c.weightx=0.2; fields.add(fromField,c);
            c.gridx=4; c.weightx=0; fields.add(toLabel,c);
            c.gridx=5; c.weightx=0.2; fields.add(toField,c);
            loadMedicineNamesIntoCombo();
            JLabel required = new JLabel("(COMPULSORY)", SwingConstants.CENTER); required.setForeground(Color.RED); required.setFont(new Font("Arial",Font.BOLD,14));
            c.gridx=1;c.gridy=1;c.weightx=0.5;fields.add(required,c);
            JLabel optional = new JLabel("(OPTIONAL)", SwingConstants.CENTER); optional.setForeground(Color.RED); optional.setFont(new Font("Arial",Font.BOLD,14));
            c.gridx=3;c.gridwidth=3;fields.add(optional,c); c.gridwidth=1;
            JButton search = button("SEARCH", new Color(0,170,75), Color.WHITE);
            JPanel searchButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); searchButtonPanel.setOpaque(false); searchButtonPanel.add(search);
            searchBox.add(fields,BorderLayout.CENTER); searchBox.add(searchButtonPanel,BorderLayout.SOUTH);
            root.add(searchBox,BorderLayout.NORTH);

            reportTable.setFont(new Font("Arial",Font.PLAIN,13)); reportTable.setRowHeight(27); reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            reportTable.getTableHeader().setFont(new Font("Arial",Font.BOLD,13)); reportTable.getTableHeader().setBackground(new Color(70,130,180));
            reportTable.getTableHeader().setForeground(Color.WHITE); reportTable.getTableHeader().setReorderingAllowed(false); reportTable.getTableHeader().setPreferredSize(new Dimension(0, 34));
            int[] widths={220,100,110,100,115,115,155,110,155,110,115,125,120,215,115,130,220};
            FontMetrics reportHeaderMetrics = reportTable.getTableHeader().getFontMetrics(reportTable.getTableHeader().getFont());
            for(int i=0;i<widths.length;i++) {
                int headerWidth = reportHeaderMetrics.stringWidth(reportTable.getColumnName(i)) + 30;
                int finalWidth = Math.max(widths[i], headerWidth);
                reportTable.getColumnModel().getColumn(i).setPreferredWidth(finalWidth);
                reportTable.getColumnModel().getColumn(i).setMinWidth(finalWidth);
            }
            root.add(new JScrollPane(reportTable),BorderLayout.CENTER);

            JPanel totals = new JPanel(new GridLayout(1,2,12,0)); totals.setOpaque(false);
            totals.add(summaryBox("TOTAL (EXCEPT DISCOUNT) (TK.)",grossTotalLabel,new Color(30,80,170)));
            totals.add(summaryBox("TOTAL SALE (AFTER DISCOUNT) (TK.)",netTotalLabel,new Color(0,120,70)));
            JButton refresh=button("REFRESH",new Color(244,177,131),Color.BLACK);
            JButton export=button("EXPORT CSV",new Color(255,105,180),Color.BLACK);
            JButton print=button("PRINT REPORT",new Color(95,95,110),Color.WHITE);
            JButton back=button("BACK",new Color(190,220,245),Color.BLACK);
            JPanel bottom=new JPanel(new BorderLayout(8,8)); bottom.setOpaque(false); bottom.add(totals,BorderLayout.CENTER);
            JPanel actions=new JPanel(new FlowLayout(FlowLayout.CENTER,12,4)); actions.setOpaque(false);
            actions.add(refresh);actions.add(export);actions.add(print);actions.add(back);bottom.add(actions,BorderLayout.SOUTH);root.add(bottom,BorderLayout.SOUTH);
            search.addActionListener(e -> runMedicineSearch()); medicineNameCombo.addActionListener(e -> { /* select a stock medicine; press SEARCH to run report */ });
            refresh.addActionListener(e -> { loadMedicineNamesIntoCombo(); if (medicineNameCombo.getItemCount() > 0) medicineNameCombo.setSelectedIndex(0); fromField.setText(""); toField.setText(""); reportModel.setRowCount(0); grossTotalLabel.setText("Tk. 0.00"); netTotalLabel.setText("Tk. 0.00"); });
            export.addActionListener(e -> exportMedicineCsv());
            print.addActionListener(e -> { try { reportTable.print(); } catch (java.awt.print.PrinterException ex) { error("Could not print report: " + ex.getMessage()); } });
            back.addActionListener(e -> dispose());
            setContentPane(root);
        }

        private void loadMedicineNamesIntoCombo() {
            Object selected = medicineNameCombo.getSelectedItem();
            String previous = selected == null ? "" : selected.toString();
            medicineNameCombo.removeAllItems();
            LinkedHashSet<String> names = new LinkedHashSet<>();
            try {
                if (Files.exists(STOCK_FILE)) {
                    for (String line : Files.readAllLines(STOCK_FILE, StandardCharsets.UTF_8)) {
                        if (line.trim().isEmpty()) continue;
                        String[] fields = line.split("\\|", -1);
                        // stock.txt layout: Medicine ID | Medicine Name | Form | Quantity | MRP | Expiry | Shelf
                        if (fields.length >= 7 && !fields[1].trim().isEmpty()) {
                            names.add(fields[1].trim());
                        }
                    }
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not load medicine names from data/stock.txt.\\n" + ex.getMessage(),
                        "Stock Read Error", JOptionPane.ERROR_MESSAGE);
            }
            for (String name : names) medicineNameCombo.addItem(name);
            if (!previous.isEmpty()) medicineNameCombo.setSelectedItem(previous);
            if (medicineNameCombo.getSelectedIndex() < 0 && medicineNameCombo.getItemCount() > 0) {
                medicineNameCombo.setSelectedIndex(0);
            }
        }

        private void runMedicineSearch() {
            String medicine = medicineNameCombo.getSelectedItem() == null ? "" : medicineNameCombo.getSelectedItem().toString().trim();
            if (medicine.isEmpty()) { JOptionPane.showMessageDialog(this,"Medicine Name is compulsory.","Invalid Input",JOptionPane.ERROR_MESSAGE);return; }
            LocalDate from=null,to=null;
            try {
                if(!fromField.getText().trim().isEmpty()) from=LocalDate.parse(fromField.getText().trim(),DATE_FMT);
                if(!toField.getText().trim().isEmpty()) to=LocalDate.parse(toField.getText().trim(),DATE_FMT);
                if(from!=null&&to!=null&&from.isAfter(to)){JOptionPane.showMessageDialog(this,"From Date cannot be later than To Date.","Invalid Date",JOptionPane.ERROR_MESSAGE);return;}
            } catch(DateTimeParseException ex){JOptionPane.showMessageDialog(this,"Date must use DD-MM-YYYY format.","Invalid Date",JOptionPane.ERROR_MESSAGE);return;}
            reportModel.setRowCount(0); double grossSum=0,netSum=0; int found=0;
            for(SaleLine r:allLines){
                if(!r.medicineName.equalsIgnoreCase(medicine)) continue;
                LocalDate date; try{date=LocalDate.parse(r.date,DATE_FMT);}catch(DateTimeParseException ex){continue;}
                if(from!=null&&date.isBefore(from))continue; if(to!=null&&date.isAfter(to))continue;
                double gross=num(r.fields[11])*num(r.fields[12]); double discount=num(r.fields[16]); double net=gross-discount;
                reportModel.addRow(new Object[]{r.medicineName,r.saleId,r.date,r.time,r.salesBy,r.customerId,r.customerName,r.medicineId,r.medicineName,r.form,r.fields[20],r.quantity,
                    "Tk. "+money(num(r.fields[12])),"Tk. "+money(gross),r.discountPercent+"%","Tk. "+money(discount),"Tk. "+money(net)});
                grossSum+=gross;netSum+=net;found++;
            }
            grossTotalLabel.setText("Tk. "+money(grossSum));netTotalLabel.setText("Tk. "+money(netSum));
            if(found==0) JOptionPane.showMessageDialog(this,"No sales found for medicine: "+medicine,"No Results",JOptionPane.INFORMATION_MESSAGE);
        }

        private void exportMedicineCsv() {
            JFileChooser chooser=new JFileChooser();chooser.setDialogTitle("Export Medicine Sales Report as CSV");chooser.setSelectedFile(new File("MedicineSalesReport.csv"));
            if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;
            try(BufferedWriter writer=Files.newBufferedWriter(chooser.getSelectedFile().toPath(),StandardCharsets.UTF_8)){
                for(int c=0;c<reportModel.getColumnCount();c++){if(c>0)writer.write(",");writer.write(csv(reportModel.getColumnName(c)));}writer.newLine();
                for(int r=0;r<reportModel.getRowCount();r++){for(int c=0;c<reportModel.getColumnCount();c++){if(c>0)writer.write(",");writer.write(csv(String.valueOf(reportModel.getValueAt(r,c))));}writer.newLine();}
                JOptionPane.showMessageDialog(this,"Medicine Sales Report exported successfully.","Export Successful",JOptionPane.INFORMATION_MESSAGE);
            }catch(IOException ex){JOptionPane.showMessageDialog(this,"Could not export CSV: "+ex.getMessage(),"Export Error",JOptionPane.ERROR_MESSAGE);}
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Total Sales Report as CSV");
        chooser.setSelectedFile(new File("TotalSalesReport.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) writer.write(",");
                writer.write(csv(model.getColumnName(c)));
            }
            writer.newLine();
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) writer.write(",");
                    writer.write(csv(String.valueOf(model.getValueAt(r, c))));
                }
                writer.newLine();
            }
            JOptionPane.showMessageDialog(this, "Report exported successfully:\n" + file.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) { error("Could not export CSV: " + ex.getMessage()); }
    }

    private static String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }
    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }
    private static double num(String value) {
        try { return Double.parseDouble(value.trim()); } catch (Exception ex) { return 0.0; }
    }
    private static String money(double value) { return String.format(Locale.US, "%.2f", value); }
    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Total Sales Report", JOptionPane.ERROR_MESSAGE);
    }

    private static final class SaleLine {
        final String[] fields;
        final String saleId, date, time, salesBy, customerId, customerName, phone;
        final String medicineId, medicineName, form, quantity, discountPercent;
        SaleLine(String[] f) {
            fields = f;
            saleId=f[0].trim(); date=f[1].trim(); time=f[2].trim(); salesBy=f[3].trim();
            customerId=f[4].trim(); customerName=f[5].trim(); phone=f[6].trim();
            medicineId=f[8].trim(); medicineName=f[9].trim(); form=f[10].trim();
            quantity=f[11].trim(); discountPercent=f[14].trim();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TotalSalesReport().setVisible(true));
    }
}