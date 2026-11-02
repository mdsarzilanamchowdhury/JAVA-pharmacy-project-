package com.sarzilsazibpharmacy;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
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
 * Pharmacy Sales Management with stock-linked sale entry and sale-record deletion.
 */
public class SalesManagement extends JFrame {

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path STOCK_FILE = DATA_DIR.resolve("stock.txt");
    private static final Path SALES_FILE = DATA_DIR.resolve("sales.txt");
    private static final Path CUSTOMERS_FILE = DATA_DIR.resolve("customers.txt");
    private static final Path CUSTOMER_COUNTER_FILE = DATA_DIR.resolve("customer_id_counter.txt");
    private static final Path SALE_COUNTER_FILE = DATA_DIR.resolve("sale_id_counter.txt");
    private static final Path STOCK_TEMP = DATA_DIR.resolve("stock_sales_temp.txt");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    public SalesManagement() {
        setTitle("Sarzil-Sazib-Pharmacy - Sales Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1450, 850);
        setMinimumSize(new Dimension(1250, 760));
        setLocationRelativeTo(null);
        // Sales Management now opens directly into the sale-entry window.
        // Existing callers that invoke setVisible(true) will launch the same window.
    }
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            SwingUtilities.invokeLater(() -> new AddSaleWindow().setVisible(true));
        } else {
            super.setVisible(false);
        }
    }
    private void buildMainMenu() {
        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBorder(BorderFactory.createEmptyBorder(28, 40, 25, 40));
        root.setBackground(new Color(240, 248, 255));

        JLabel title = new JLabel("SALES MANAGEMENT", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 30));
        title.setForeground(new Color(25, 70, 150));
        root.add(title, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 14, 14));
        buttonPanel.setOpaque(false);

        JButton addSaleButton = menuButton("1. Add Sale");
        JButton deleteSaleButton = menuButton("2. Delete Sale");
        JButton updateSaleButton = menuButton("3. Update Sale");
        JButton backButton = menuButton("4. Back");

        addSaleButton.addActionListener(e -> new AddSaleWindow().setVisible(true));
        deleteSaleButton.addActionListener(e -> JOptionPane.showMessageDialog(
                this, "Delete Sale will be implemented in the next step.",
                "Delete Sale", JOptionPane.INFORMATION_MESSAGE));
        updateSaleButton.addActionListener(e -> JOptionPane.showMessageDialog(
                this, "Update Sale will be implemented in the next step.",
                "Update Sale", JOptionPane.INFORMATION_MESSAGE));
        backButton.addActionListener(e -> dispose());

        buttonPanel.add(addSaleButton);
        buttonPanel.add(deleteSaleButton);
        buttonPanel.add(updateSaleButton);
        buttonPanel.add(backButton);
        root.add(buttonPanel, BorderLayout.CENTER);

        JLabel footer = new JLabel("Sarzil-Sazib-Pharmacy", SwingConstants.CENTER);
        footer.setFont(new Font("Arial", Font.ITALIC, 13));
        footer.setForeground(new Color(70, 90, 120));
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JButton menuButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 20));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(400, 65));
        return button;
    }

    /** The existing sale-entry screen, opened by Add Sale. */
    private static class AddSaleWindow extends JFrame {
    private final JTextField salesByField = new JTextField(16);
    private final JTextField saleIdValueLabel = new JTextField(10);
    private final JLabel dateValueLabel = new JLabel(LocalDate.now().format(DATE_FMT));
    private final JLabel timeValueLabel = new JLabel(LocalDateTime.now().format(TIME_FMT));
    private final JTextField customerIdField = new JTextField(12);
    private final JTextField customerNameField = new JTextField(14);
    private final JTextField phoneField = new JTextField("+88", 12);
    private final JLabel phoneWarningLabel = new JLabel("ENTER 11 DIGITS AFTER +88");
    private final JTextField addressField = new JTextField(18);
    private final JComboBox<StockItem> medicineCombo = new JComboBox<>();
    private final JTextField mrpPerUnitField = new JTextField(8);
    private final JTextField quantityField = new JTextField(8);
    private final JLabel quantityWarningLabel = new JLabel("STOCK QUANTITY IS = --- PCS");
    private final JTextField discountField = new JTextField(8);
    private boolean phoneInvalidDialogShown = false;
    private boolean quantityInvalidDialogShown = false;
    private final JLabel statusLabel = infoLabel("");
    private final JLabel grossLabel = new JLabel("Tk. 0.00", SwingConstants.CENTER);
    private final JLabel discountTotalLabel = new JLabel("DISCOUNT: Tk. 0.00", SwingConstants.CENTER);
    private final JLabel netLabel = new JLabel("PAYABLE (TK.): 0.00");
    private final JLabel roundFigureLabel = new JLabel("ROUND FIGURE: 0");
    private final JTextField paidField = new JTextField(8);
    private final JLabel dueLabel = new JLabel("DUE (TK.): ");
    private final JComboBox<String> invoiceSaleCombo = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{
            "Medicine ID", "Medicine Name", "Form", "Expiry Date", "MRP per Unit (Tk.)",
            "Quantity for Sale (pcs.)", "Total Price (Tk.)", "Discount (%)",
            "Discount (Tk.)", "Payable (Tk.)", "Remaining Quantity"
    }, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable cartTable = new JTable(model);
    private final List<CartItem> cart = new ArrayList<>();
    private List<StockItem> stockItems = new ArrayList<>();

    AddSaleWindow() {
        setTitle("Sarzil-Sazib-Pharmacy - Sales Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1500, 880);
        setMinimumSize(new Dimension(1250, 760));
        setLocationRelativeTo(null);
        buildUI();
        refreshStockChoices();
        refreshSaleHeader();
        refreshInvoiceSaleChoices();
        initializeCustomerId();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        root.setBackground(new Color(240, 248, 255));

        JLabel heading = new JLabel("SALES MANAGEMENT", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 29));
        heading.setForeground(new Color(25, 70, 130));
        root.add(heading, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(10, 8));
        top.setOpaque(false);

        // Sale details on the left, a large centered title, customer details on the right.
        JPanel saleInfo = new JPanel(new GridLayout(4, 2, 8, 9));
        saleInfo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        saleInfo.setBackground(Color.WHITE);
        addField(saleInfo, "Sale ID:", saleIdValueLabel);
        addField(saleInfo, "Selling Date:", dateValueLabel);
        addField(saleInfo, "Time:", timeValueLabel);
        addField(saleInfo, "Sales By:", salesByField);

        JPanel customerInfo = new JPanel(new GridLayout(4, 2, 8, 9));
        customerInfo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        customerInfo.setBackground(Color.WHITE);
        addField(customerInfo, "Customer Phone:", createPhoneInputPanel());
        addField(customerInfo, "Customer ID:", customerIdField);
        addField(customerInfo, "Customer Name:", customerNameField);
        addField(customerInfo, "Customer Address:", addressField);

        JLabel centerTitle = new JLabel("SALES MANAGEMENT", SwingConstants.CENTER);
        centerTitle.setFont(new Font("Arial", Font.BOLD, 32));
        centerTitle.setForeground(new Color(25, 70, 170));
        centerTitle.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        styleSaleIdField(saleIdValueLabel);
        styleInfoComponent(dateValueLabel);
        styleInfoComponent(timeValueLabel);
        dateValueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        timeValueLabel.setFont(new Font("Arial", Font.BOLD, 18));
        styleField(salesByField);
        styleField(customerIdField);
        customerIdField.setEditable(false);
        customerIdField.setFocusable(false);
        customerIdField.setBackground(new Color(245, 248, 252));
        customerIdField.setToolTipText("Automatically generated Customer ID; cannot be edited.");
        styleField(customerNameField);
        styleField(phoneField);
        phoneWarningLabel.setFont(new Font("Arial", Font.BOLD, 14));
        phoneWarningLabel.setPreferredSize(new Dimension(300, 22));
        phoneWarningLabel.setForeground(Color.RED);
        phoneField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePhoneWarning(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePhoneWarning(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePhoneWarning(); }
        });
        updatePhoneWarning();
        styleField(addressField);
        saleInfo.setPreferredSize(new Dimension(485, 205));
        customerInfo.setPreferredSize(new Dimension(550, 225));

        saleInfo.setBorder(createSectionBorder("SALE INFORMATION"));
        customerInfo.setBorder(createSectionBorder("CUSTOMER INFORMATION"));

        JPanel saleCustomerHeader = new JPanel(new BorderLayout(12, 4));
        saleCustomerHeader.setBackground(new Color(240, 248, 255));
        saleCustomerHeader.add(saleInfo, BorderLayout.WEST);
        saleCustomerHeader.add(centerTitle, BorderLayout.CENTER);
        saleCustomerHeader.add(customerInfo, BorderLayout.EAST);
        top.add(saleCustomerHeader, BorderLayout.NORTH);

        JPanel medicinePanel = new JPanel(new GridBagLayout());
        javax.swing.border.TitledBorder medicineBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(25, 80, 180), 3),
                "MEDICINE DETAILS (FROM STOCK)",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 18),
                new Color(25, 70, 130));
        medicinePanel.setBorder(medicineBorder);
        medicinePanel.setBackground(Color.WHITE);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 7, 3, 7);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel medicineNameLabel = new JLabel("Medicine Name:");
        JLabel mrpLabel = new JLabel("MRP per Unit (Tk.):");
        mrpLabel.setForeground(Color.RED);
        JLabel mrpStockHint = new JLabel("BY DEFAULT FROM STOCK");
        mrpStockHint.setFont(new Font("Arial", Font.BOLD, 14));
        mrpStockHint.setForeground(Color.RED);
        JLabel quantityLabel = new JLabel("Quantity for Sale (pcs):");
        JLabel discountLabel = new JLabel("Total Discount (%):");
        for (JLabel label : new JLabel[]{medicineNameLabel, mrpLabel, quantityLabel, discountLabel}) {
            label.setFont(new Font("Arial", Font.BOLD, 18));
        }

        medicineCombo.setEditable(false);
        medicineCombo.setMaximumRowCount(8);
        medicineCombo.setRenderer(new StockItemRenderer());
        medicineCombo.setPreferredSize(new Dimension(360, 30));

        // Left side: Medicine Name and MRP. Right side: Quantity and Discount.
        // A blue vertical separator divides the two sides.
        g.gridx = 0; g.gridy = 0; g.weightx = 0; g.gridwidth = 1;
        medicinePanel.add(medicineNameLabel, g);
        g.gridx = 1; g.weightx = 1;
        medicinePanel.add(medicineCombo, g);

        JSeparator verticalSeparator = new JSeparator(SwingConstants.VERTICAL);
        verticalSeparator.setForeground(new Color(25, 80, 180));
        verticalSeparator.setBackground(new Color(25, 80, 180));
        verticalSeparator.setPreferredSize(new Dimension(3, 64));
        g.gridx = 2; g.gridy = 0; g.gridheight = 2; g.weightx = 0;
        g.fill = GridBagConstraints.VERTICAL;
        medicinePanel.add(verticalSeparator, g);
        g.gridheight = 1;
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 3; g.gridy = 0; g.weightx = 0;
        medicinePanel.add(quantityLabel, g);

        // Short quantity input with the warning immediately to its right.
        JPanel quantityInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quantityInputPanel.setOpaque(false);
        quantityField.setPreferredSize(new Dimension(105, 32));
        quantityWarningLabel.setFont(new Font("Arial", Font.BOLD, 14));
        quantityWarningLabel.setForeground(Color.RED);
        quantityWarningLabel.setPreferredSize(new Dimension(300, 30));
        quantityInputPanel.add(quantityField);
        quantityInputPanel.add(quantityWarningLabel);
        g.gridx = 4; g.weightx = 1;
        medicinePanel.add(quantityInputPanel, g);

        g.gridx = 0; g.gridy = 1; g.weightx = 0;
        medicinePanel.add(mrpLabel, g);
        JPanel mrpPanel = new JPanel(new BorderLayout(8, 0));
        mrpPanel.setOpaque(false);
        mrpPanel.add(mrpPerUnitField, BorderLayout.CENTER);
        mrpPanel.add(mrpStockHint, BorderLayout.EAST);
        g.gridx = 1; g.weightx = 1;
        medicinePanel.add(mrpPanel, g);
        g.gridx = 3; g.weightx = 0;
        medicinePanel.add(discountLabel, g);
        g.gridx = 4; g.weightx = 1;
        medicinePanel.add(discountField, g);

        styleField(quantityField);
        styleField(discountField);
        styleField(mrpPerUnitField);
        mrpPerUnitField.setEditable(false);
        mrpPerUnitField.setBackground(new Color(245, 248, 252));
        mrpPerUnitField.setForeground(Color.RED);

        quantityField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { checkQuantityAgainstStock(); }
            @Override public void removeUpdate(DocumentEvent e) { checkQuantityAgainstStock(); }
            @Override public void changedUpdate(DocumentEvent e) { checkQuantityAgainstStock(); }
        });

        medicineCombo.addActionListener(e -> {
            if (medicineCombo.getSelectedItem() != null) loadSelectedMedicine();
        });

        top.add(medicinePanel, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        cartTable.setRowHeight(30);
        cartTable.setFont(new Font("Arial", Font.PLAIN, 12));
        cartTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        cartTable.setGridColor(new Color(205, 215, 225));
        cartTable.setShowVerticalLines(true);
        cartTable.setShowHorizontalLines(true);
        cartTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        cartTable.getTableHeader().setReorderingAllowed(false);
        cartTable.getTableHeader().setBackground(new Color(70, 130, 180));
        cartTable.getTableHeader().setForeground(Color.WHITE);
        cartTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setOpaque(true);
                label.setFont(new Font("Arial", Font.BOLD, 14));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, Color.WHITE),
                        BorderFactory.createEmptyBorder(4, 5, 4, 5)));
                if (column == 10) {
                    label.setBackground(Color.RED);
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(70, 130, 180));
                    label.setForeground(Color.WHITE);
                }
                return label;
            }
        });

        // Keep all 11 columns visible without a horizontal scrollbar.
        // AUTO_RESIZE_ALL_COLUMNS distributes available width across the table.
        int[] preferredWidths = {75, 115, 65, 95, 105, 125, 105, 80, 95, 95, 125};
        int[] minimumWidths  = {58, 85, 48, 75, 80, 95, 80, 60, 75, 75, 90};
        for (int i = 0; i < preferredWidths.length; i++) {
            cartTable.getColumnModel().getColumn(i).setPreferredWidth(preferredWidths[i]);
            cartTable.getColumnModel().getColumn(i).setMinWidth(minimumWidths[i]);
        }
        cartTable.getColumnModel().getColumn(10).setHeaderValue("Remaining Quantity");
        cartTable.getColumnModel().getColumn(10).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                c.setForeground(selected ? table.getSelectionForeground() : Color.RED);
                return c;
            }
        });
        cartTable.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                c.setForeground(selected ? table.getSelectionForeground() : Color.RED);
                return c;
            }
        });

        // Sale table heading: centered, larger, and surrounded by a bold blue border.
        JScrollPane scroll = new JScrollPane(cartTable);
        javax.swing.border.TitledBorder saleTableBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(25, 80, 180), 3),
                "SALE TABLE",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 20),
                new Color(25, 70, 150));
        scroll.setBorder(saleTableBorder);
        scroll.setPreferredSize(new Dimension(1400, 320));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Nine action buttons: each button width follows its text, so labels remain visible.
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy = 0;
        bc.fill = GridBagConstraints.NONE;
        bc.weightx = 0;
        bc.insets = new Insets(4, 3, 4, 3);
        bc.anchor = GridBagConstraints.CENTER;

        JButton addSaleButton = gradientButton("ADD SALE", new Color(0, 176, 240), new Color(170, 235, 255));
        JButton updateSaleButton = gradientButton("UPDATE SALE", new Color(35, 185, 235), new Color(180, 240, 220));
        JButton removeButton = gradientButton("REMOVE SALE", new Color(55, 195, 205), new Color(175, 240, 195));
        JButton confirmButton = gradientButton("CONFIRM", new Color(65, 200, 175), new Color(165, 240, 170));
        JButton invoiceButton = gradientButton("INVOICE", new Color(75, 205, 145), new Color(155, 235, 145));
        JButton dueCollectionButton = gradientButton("DUE COLLECT", new Color(65, 200, 120), new Color(145, 230, 125));
        JButton clearButton = gradientButton("CLEAR ALL", new Color(55, 190, 95), new Color(130, 225, 110));
        JButton backButton = gradientButton("BACK", new Color(40, 175, 75), new Color(110, 215, 100));
        JButton deleteSaleButton = coloredButton("DELETE SALE", new Color(210, 45, 45), Color.WHITE);

        JButton[] actionButtons = {addSaleButton, updateSaleButton, removeButton, confirmButton,
                invoiceButton, dueCollectionButton, clearButton, backButton, deleteSaleButton};
        Font actionFont = new Font("Arial", Font.BOLD, 15);
        FontMetrics fm = getFontMetrics(actionFont);
        for (JButton b : actionButtons) {
            b.setFont(actionFont);
            b.setMargin(new Insets(3, 8, 3, 8));
            int textWidth = fm.stringWidth(b.getText());
            b.setPreferredSize(new Dimension(textWidth + 30, 52));
            b.setMinimumSize(new Dimension(textWidth + 24, 48));
            b.setMaximumSize(new Dimension(textWidth + 40, 52));
        }
        addSaleButton.addActionListener(e -> addItemToCart());
        deleteSaleButton.addActionListener(e -> chooseSaleToDelete());
        updateSaleButton.addActionListener(e -> updateCartMedicine());
        removeButton.addActionListener(e -> removeSelected());
        clearButton.addActionListener(e -> clearCart());
        confirmButton.addActionListener(e -> confirmSale());
        invoiceButton.addActionListener(e -> chooseSaleForInvoice());
        dueCollectionButton.addActionListener(e -> new DueReportWindow().setVisible(true));
        backButton.addActionListener(e -> dispose());
        for (int i = 0; i < actionButtons.length; i++) {
            bc.gridx = i;
            buttons.add(actionButtons[i], bc);
        }

        // Main top area: sale/customer information, medicine details, then buttons.
        top.add(medicinePanel, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);

        // Three separate total boxes with bold blue borders.
        // Equal-width boxes keep TOTAL, DISCOUNT, PAYABLE, PAID and DUE visible.
        JPanel totals = new JPanel(new GridBagLayout());
        totals.setOpaque(false);
        GridBagConstraints tc = new GridBagConstraints(); tc.gridy = 0; tc.fill = GridBagConstraints.BOTH; tc.weighty = 1; tc.insets = new Insets(0, 3, 0, 3);
        tc.gridx=0; tc.weightx=1.0; totals.add(createTotalBox("TOTAL (EXCEPT DISCOUNT)", grossLabel), tc);
        tc.gridx=1; tc.weightx=0.9; totals.add(createTotalBox("DISCOUNT (TK.)", discountTotalLabel), tc);
        tc.gridx=2; tc.weightx=1.55; totals.add(createPayableRoundBox(), tc);
        paidField.setFont(new Font("Arial", Font.BOLD, 18));
        paidField.setText("");
        dueLabel.setText("DUE (TK.): ");
        paidField.setHorizontalAlignment(SwingConstants.LEFT);
        paidField.setToolTipText("Enter the amount paid by the customer");
        paidField.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        paidField.setBackground(new Color(255, 245, 220));
        JPanel paidBox = createTotalBox("PAID (TK.)", paidField);
        tc.gridx=3; tc.weightx=0.9; totals.add(paidBox, tc);
        tc.gridx=4; tc.weightx=0.9; totals.add(createTotalBox("DUE (TK.)", dueLabel), tc);
        paidField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updatePaidDuePreview(); }
            @Override public void removeUpdate(DocumentEvent e) { updatePaidDuePreview(); }
            @Override public void changedUpdate(DocumentEvent e) { updatePaidDuePreview(); }
        });
        JPanel bottom = new JPanel(new BorderLayout(8, 4));
        bottom.setOpaque(false);
        bottom.add(totals, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        setContentPane(root);

    }

    private JPanel createPhoneInputPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        phoneField.setAlignmentX(Component.LEFT_ALIGNMENT);
        phoneWarningLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(phoneField);
        panel.add(phoneWarningLabel);
        return panel;
    }

    private void updatePhoneWarning() {
        String value = phoneField.getText().trim();
        if (!value.startsWith("+88")) {
            phoneWarningLabel.setText("ENTER 11 DIGITS AFTER +88");
            phoneWarningLabel.setForeground(Color.RED);
            phoneInvalidDialogShown = false;
            return;
        }

        String suffix = value.substring(3).replaceAll("\\s+", "");
        if (suffix.matches("01[3-9]\\d{8}")) lookupCustomer(suffix);
        if (!suffix.matches("\\d*") || suffix.length() > 11) {
            phoneWarningLabel.setText("ENTER 11 DIGITS AFTER +88");
            phoneWarningLabel.setForeground(Color.RED);
            if (!phoneInvalidDialogShown) {
                phoneInvalidDialogShown = true;
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this, "INVALID", "Customer Phone", JOptionPane.ERROR_MESSAGE));
            }
        } else if (suffix.length() == 11) {
            phoneWarningLabel.setText("VALID");
            phoneWarningLabel.setForeground(new Color(0, 125, 45));
            phoneInvalidDialogShown = false;
        } else {
            phoneWarningLabel.setText("ENTER 11 DIGITS AFTER +88");
            phoneWarningLabel.setForeground(Color.RED);
            phoneInvalidDialogShown = false;
        }
    }

    private void initializeCustomerId() {
        try { customerIdField.setText(nextCustomerId()); }
        catch (IOException ex) { showError("Could not initialize Customer ID: " + ex.getMessage()); }
    }

    private String nextCustomerId() throws IOException {
        Files.createDirectories(DATA_DIR);
        int counter = 0;
        if (Files.exists(CUSTOMER_COUNTER_FILE)) {
            String value = Files.readString(CUSTOMER_COUNTER_FILE, StandardCharsets.UTF_8).trim();
            if (!value.isEmpty()) counter = Integer.parseInt(value);
        }
        int maxSaved = 0;
        if (Files.exists(CUSTOMERS_FILE)) {
            for (String[] row : readCustomers()) {
                if (row.length >= 2 && row[1].matches("CUS-\\d+")) {
                    maxSaved = Math.max(maxSaved, Integer.parseInt(row[1].substring(4)));
                }
            }
        }
        return String.format(Locale.ROOT, "CUS-%03d", Math.max(counter, maxSaved) + 1);
    }

    private List<String[]> readCustomers() throws IOException {
        Files.createDirectories(DATA_DIR);
        if (!Files.exists(CUSTOMERS_FILE)) Files.createFile(CUSTOMERS_FILE);
        List<String[]> rows = new ArrayList<>();
        for (String line : Files.readAllLines(CUSTOMERS_FILE, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 4) throw new IOException("Invalid customers.txt record; expected 4 fields.");
            rows.add(f);
        }
        return rows;
    }

    private void lookupCustomer(String phoneDigits) {
        try {
            for (String[] r : readCustomers()) {
                if (r[0].equals(phoneDigits)) {
                    customerIdField.setText(r[1]); customerNameField.setText(r[2]); addressField.setText(r[3]); return;
                }
            }
            customerIdField.setText(nextCustomerId()); customerNameField.setText(""); addressField.setText("");
        } catch (IOException ex) { showError("Could not read customers.txt: " + ex.getMessage()); }
    }

    private String customerIdForPhone(String phone) throws IOException {
        String digits = phone.replaceAll("\\s+", "").substring(3);
        for (String[] r : readCustomers()) if (r[0].equals(digits)) return r[1];
        return nextCustomerId();
    }

    private void saveCustomerAfterConfirmedSale(String phone, String name, String address) throws IOException {
        String digits = phone.replaceAll("\\s+", "").substring(3);
        List<String[]> rows = readCustomers();
        for (String[] r : rows) {
            if (r[0].equals(digits)) { r[2] = safeCustomerField(name); r[3] = safeCustomerField(address); writeCustomers(rows); return; }
        }
        String id = nextCustomerId();
        rows.add(new String[]{digits, id, safeCustomerField(name), safeCustomerField(address)});
        int n = Integer.parseInt(id.substring(4));
        Files.writeString(CUSTOMER_COUNTER_FILE, String.valueOf(n), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        writeCustomers(rows);
    }

    private void writeCustomers(List<String[]> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String[] r : rows) lines.add(String.join("|", r));
        Files.write(CUSTOMERS_FILE, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String safeCustomerField(String value) {
        return value == null ? "" : value.trim().replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }

    private void checkQuantityAgainstStock() {
        String text = quantityField.getText().trim();
        if (text.isEmpty()) {
            quantityInvalidDialogShown = false;
            return;
        }
        Object selected = medicineCombo.getSelectedItem();
        if (!(selected instanceof StockItem)) {
            quantityInvalidDialogShown = false;
            return;
        }
        try {
            int qty = Integer.parseInt(text);
            StockItem item = (StockItem) selected;
            if (qty > item.quantity && !quantityInvalidDialogShown) {
                quantityInvalidDialogShown = true;
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this, "INVALID", "Quantity Validation", JOptionPane.ERROR_MESSAGE));
            } else if (qty <= item.quantity) {
                quantityInvalidDialogShown = false;
            }
        } catch (NumberFormatException ex) {
            quantityInvalidDialogShown = false;
        }
    }

    private static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        String normalized = phone.trim().replaceAll("\\s+", "");
        return normalized.matches("\\+88\\d{11}");
    }

    private static void addField(JPanel panel, String label, Component component) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setFont(new Font("Arial", Font.BOLD, 18));
        fieldLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(fieldLabel);
        panel.add(component);
    }
    private static JLabel totalLabel(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 17));
        label.setForeground(color);
        return label;
    }

    private static JPanel createTotalBox(String title, Component valueLabel) {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(25,80,180),3), BorderFactory.createEmptyBorder(5,12,5,12)));
        if (valueLabel instanceof JLabel) ((JLabel) valueLabel).setFont(new Font("Arial", Font.BOLD, 17));
        if (valueLabel instanceof JTextField) ((JTextField) valueLabel).setFont(new Font("Arial", Font.BOLD, 18));
        if (title == null || title.isBlank()) panel.add(valueLabel, BorderLayout.CENTER);
        else { JLabel titleLabel = new JLabel(title, SwingConstants.CENTER); titleLabel.setFont(new Font("Arial",Font.BOLD,15)); titleLabel.setForeground(new Color(25,70,150)); panel.add(titleLabel,BorderLayout.NORTH); panel.add(valueLabel,BorderLayout.CENTER); }
        panel.setPreferredSize(new Dimension(220, 70));
        panel.setMinimumSize(new Dimension(0, 70));
        return panel;
    }

    private JPanel createPayableRoundBox() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(25,80,180),3), BorderFactory.createEmptyBorder(5,12,5,12)));
        netLabel.setFont(new Font("Arial", Font.BOLD, 16));
        roundFigureLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(netLabel); panel.add(roundFigureLabel);
        panel.setPreferredSize(new Dimension(300,70));
        panel.setMinimumSize(new Dimension(0,70));
        return panel;
    }

    private static javax.swing.border.TitledBorder createSectionBorder(String title) {
        javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(25, 80, 180), 3),
                title,
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 19),
                new Color(25, 70, 130));
        return border;
    }

    private static void styleInfoComponent(JLabel label) {
        label.setFont(new Font("Arial", Font.BOLD, 15));
        label.setForeground(new Color(35, 65, 110));
    }
    private static void styleSaleIdField(JTextField field) {
        field.setFont(new Font("Arial", Font.BOLD, 15));
        field.setForeground(new Color(35, 65, 110));
        field.setEditable(false);
        field.setFocusable(false);
        field.setBackground(new Color(245, 248, 252));
        field.setToolTipText("Automatically generated Sale ID; cannot be edited.");
    }
    private static void styleField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setPreferredSize(new Dimension(150, 30));
    }
    private static JLabel infoLabel(String text) {
        JLabel l = new JLabel(text); l.setForeground(new Color(45, 90, 145)); l.setFont(new Font("Arial", Font.PLAIN, 14)); return l;
    }
    private static JButton button(String text) {
        return coloredButton(text, new Color(70, 130, 180), Color.WHITE);
    }
    private static JButton gradientButton(String text, Color start, Color end) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false); b.setOpaque(false); b.setForeground(Color.BLACK); b.setFocusPainted(false);
        return b;
    }

    private static JButton coloredButton(String text, Color background, Color foreground) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 15));
        b.setBackground(background);
        b.setForeground(foreground);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(Math.max(120, text.length() * 9 + 28), 42));
        return b;
    }

    private void refreshSaleHeader() {
    LocalDateTime now = LocalDateTime.now();

    dateValueLabel.setText(now.toLocalDate().format(DATE_FMT));
    timeValueLabel.setText(now.format(TIME_FMT));

    try {
        String newSaleID = nextSaleId();
        saleIdValueLabel.setText(newSaleID);
    } catch (IOException ex) {
        saleIdValueLabel.setText("ERROR");
        showError("SALE ID GENERATION FAILED!\n" + ex.getMessage());
    }
}

    private void refreshStockChoices() {
        try {
            Files.createDirectories(DATA_DIR);
            if (!Files.exists(STOCK_FILE)) Files.createFile(STOCK_FILE);
            stockItems = readStock();
            medicineCombo.removeAllItems();
            for (StockItem item : stockItems) medicineCombo.addItem(item);
            medicineCombo.setSelectedIndex(-1);
            mrpPerUnitField.setText("");
            quantityField.setText("");
            discountField.setText("");
            quantityWarningLabel.setText("STOCK QUANTITY IS = --- PCS");
            statusLabel.setText("");
        } catch (IOException ex) {
            showError("Could not read stock file: " + ex.getMessage());
        }
    }

    private void loadSelectedMedicine() {
        Object selected = medicineCombo.getSelectedItem();
        if (!(selected instanceof StockItem)) {
            mrpPerUnitField.setText("");
            quantityField.setText("");
            discountField.setText("");
            quantityWarningLabel.setText("STOCK QUANTITY IS = --- PCS");
            quantityInvalidDialogShown = false;
            return;
        }
        StockItem chosen = (StockItem) selected;
        quantityWarningLabel.setText("STOCK QUANTITY IS = " + chosen.quantity + " PCS");
        try {
            stockItems = readStock();
            StockItem found = findByName(stockItems, chosen.name);
            if (found == null) {
                mrpPerUnitField.setText("");
                quantityField.setText("");
                discountField.setText("");
                return;
            }
            mrpPerUnitField.setText(String.format(Locale.US, "%.2f", found.mrp));
            quantityField.setText("");
            discountField.setText("");
            quantityInvalidDialogShown = false;
            statusLabel.setText("");
        } catch (IOException ex) {
            showError("Unable to load medicine details from stock.txt.\\n" + ex.getMessage());
        }
    }

    private String selectedMedicineName() {
        Object selected = medicineCombo.getSelectedItem();
        return selected instanceof StockItem ? ((StockItem) selected).name : "";
    }

    private void addItemToCart() {
        try {
            stockItems = readStock();
            String name = selectedMedicineName();
            StockItem stock = findByName(stockItems, name);
            if (stock == null) throw new IllegalArgumentException("Medicine name was not found in stock.txt.");
            LocalDate expiry = LocalDate.parse(stock.expiry, DATE_FMT);
            if (expiry.isBefore(LocalDate.now())) throw new IllegalArgumentException("This medicine has expired and cannot be sold.");
            int qty = Integer.parseInt(quantityField.getText().trim());
            double price = stock.mrp;
            String discountText = discountField.getText().trim();
            if (!discountText.endsWith("%")) {
                JOptionPane.showMessageDialog(this, "ENTER %", "Discount Validation", JOptionPane.ERROR_MESSAGE);
                discountField.requestFocus();
                return;
            }
            String numericDiscount = discountText.substring(0, discountText.length() - 1).trim();
            double discount = Double.parseDouble(numericDiscount);
            if (qty <= 0) throw new IllegalArgumentException("Sale quantity must be a positive integer.");
            if (qty > stock.quantity) {
                JOptionPane.showMessageDialog(this, "INVALID", "Quantity Validation", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!Double.isFinite(price) || price <= 0) throw new IllegalArgumentException("MRP per unit must be greater than zero.");
            if (!Double.isFinite(discount) || discount < 0 || discount > 100) throw new IllegalArgumentException("Discount must be between 0 and 100%.");
            for (CartItem existing : cart) {
                if (existing.stock.id.equalsIgnoreCase(stock.id)) {
                    if (existing.quantity + qty > stock.quantity) {
                        JOptionPane.showMessageDialog(this, "INVALID", "Quantity Validation", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    existing.quantity += qty;
                    existing.sellingPrice = price;
                    existing.discountPercent = discount;
                    refreshCartTable();
                    statusLabel.setText("Quantity updated for " + stock.name + ".");
                    return;
                }
            }
            cart.add(new CartItem(stock, qty, price, discount));
            refreshCartTable();
            statusLabel.setText("Medicine added to current sale.");
        } catch (NumberFormatException ex) {
            showError("Quantity must be a whole number and Total Discount must contain a valid percentage, e.g. 5%.");
        } catch (IOException ex) {
            showError("Could not read stock file: " + ex.getMessage());
        } catch (DateTimeParseException ex) {
            showError("Expiry date in stock.txt must use DD-MM-YYYY format.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    /** Update one medicine currently listed in the sale table. */
    private void updateCartMedicine() {
        if (cart.isEmpty()) {
            showInfo("Add a medicine to the current sale before using Update Sale.");
            return;
        }
        DefaultListModel<String> namesModel = new DefaultListModel<>();
        for (CartItem item : cart) namesModel.addElement(item.stock.name);
        JList<String> namesList = new JList<>(namesModel);
        namesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        namesList.setFont(new Font("Arial", Font.PLAIN, 17));
        namesList.setVisibleRowCount(Math.min(8, namesModel.size()));
        namesList.setSelectedIndex(0);
        int choose = JOptionPane.showConfirmDialog(this, new JScrollPane(namesList),
                "Choose Medicine to Update", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choose != JOptionPane.OK_OPTION || namesList.getSelectedIndex() < 0) return;
        int cartIndex = namesList.getSelectedIndex();
        CartItem oldItem = cart.get(cartIndex);
        try {
            stockItems = readStock();
        } catch (IOException ex) {
            showError("Could not read stock file: " + ex.getMessage());
            return;
        }

        JComboBox<StockItem> medicineChoice = new JComboBox<>();
        medicineChoice.setRenderer(new StockItemRenderer());
        for (StockItem stock : stockItems) medicineChoice.addItem(stock);
        for (int i = 0; i < medicineChoice.getItemCount(); i++) {
            if (medicineChoice.getItemAt(i).id.equalsIgnoreCase(oldItem.stock.id)) {
                medicineChoice.setSelectedIndex(i); break;
            }
        }
        JTextField mrp = new JTextField(12);
        mrp.setEditable(false);
        mrp.setForeground(Color.RED);
        JTextField qty = new JTextField(String.valueOf(oldItem.quantity), 12);
        JTextField discount = new JTextField(String.format(Locale.US, "%.2f%%", oldItem.discountPercent), 12);
        Runnable updateMrp = () -> {
            StockItem selected = (StockItem) medicineChoice.getSelectedItem();
            mrp.setText(selected == null ? "" : money(selected.mrp));
        };
        medicineChoice.addActionListener(e -> updateMrp.run());
        updateMrp.run();
        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.add(new JLabel("Medicine Name:")); form.add(medicineChoice);
        form.add(new JLabel("MRP (Tk.):")); form.add(mrp);
        form.add(new JLabel("Quantity for Sale (pcs.):")); form.add(qty);
        form.add(new JLabel("Discount (%):")); form.add(discount);
        for (Component c : form.getComponents()) c.setFont(new Font("Arial", Font.BOLD, 15));
        int result = JOptionPane.showConfirmDialog(this, form, "Update Sale Medicine Details",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            StockItem newStock = (StockItem) medicineChoice.getSelectedItem();
            if (newStock == null) throw new IllegalArgumentException("Select a medicine from stock.");
            if (LocalDate.parse(newStock.expiry, DATE_FMT).isBefore(LocalDate.now()))
                throw new IllegalArgumentException("Expired medicine cannot be sold.");
            int newQty = Integer.parseInt(qty.getText().trim());
            String dText = discount.getText().trim();
            if (!dText.endsWith("%")) {
                JOptionPane.showMessageDialog(this, "ENTER %", "Discount Validation", JOptionPane.ERROR_MESSAGE);
                return;
            }
            double newDiscount = Double.parseDouble(dText.substring(0, dText.length() - 1).trim());
            if (newQty <= 0) throw new IllegalArgumentException("Quantity must be a positive whole number.");
            if (!Double.isFinite(newDiscount) || newDiscount < 0 || newDiscount > 100)
                throw new IllegalArgumentException("Discount must be between 0% and 100%.");
            StockItem latest = findByName(readStock(), newStock.name);
            if (latest == null) throw new IllegalArgumentException("Selected medicine is no longer in stock.");
            int otherCartQty = 0;
            for (int i = 0; i < cart.size(); i++) {
                if (i != cartIndex && cart.get(i).stock.id.equalsIgnoreCase(latest.id)) otherCartQty += cart.get(i).quantity;
            }
            if (newQty + otherCartQty > latest.quantity) {
                JOptionPane.showMessageDialog(this, "INVALID", "Quantity Validation", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!latest.id.equalsIgnoreCase(oldItem.stock.id)) {
                for (int i = 0; i < cart.size(); i++) {
                    if (i != cartIndex && cart.get(i).stock.id.equalsIgnoreCase(latest.id))
                        throw new IllegalArgumentException("This medicine is already in the current sale. Update that row instead.");
                }
            }
            oldItem.stock = latest;
            oldItem.quantity = newQty;
            oldItem.sellingPrice = latest.mrp;
            oldItem.discountPercent = newDiscount;
            refreshCartTable();
            statusLabel.setText("Sale row updated successfully.");
        } catch (NumberFormatException ex) {
            showError("Quantity must be a whole number and Discount must be a percentage, e.g. 5%.");
        } catch (IOException | DateTimeParseException ex) {
            showError("Unable to update sale: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void removeSelected() {
        int row = cartTable.getSelectedRow();
        if (row < 0 || row >= cart.size()) { showInfo("Select a medicine row to remove."); return; }
        cart.remove(row); refreshCartTable();
    }
    private void clearCart() {
        if (cart.isEmpty()) { showInfo("The current sale is already empty."); return; }
        int choice = JOptionPane.showConfirmDialog(this, "Clear all medicines from this sale?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) { cart.clear(); paidField.setText(""); dueLabel.setText("DUE (TK.): "); refreshCartTable(); }
    }

    private void refreshCartTable() {
        model.setRowCount(0);
        for (CartItem item : cart) {
            double totalPrice = item.quantity * item.stock.mrp;
            double disc = totalPrice * item.discountPercent / 100.0;
            int remainingQuantity = Math.max(0, item.stock.quantity - item.quantity);
            double payable = Math.max(0.0, totalPrice - disc);
            model.addRow(new Object[]{item.stock.id, item.stock.name, item.stock.form, item.stock.expiry,
                    money(item.stock.mrp), item.quantity, money(totalPrice),
                    String.format(Locale.US, "%.2f%%", item.discountPercent), money(disc), money(payable), remainingQuantity});
        }
        grossLabel.setText("Tk. " + money(totalGross()));
        grossLabel.setHorizontalAlignment(SwingConstants.CENTER);
        discountTotalLabel.setText("DISCOUNT: Tk. " + money(totalDiscount()));
        discountTotalLabel.setHorizontalAlignment(SwingConstants.CENTER);
        netLabel.setText("PAYABLE: Tk. " + money(totalNet()));
        roundFigureLabel.setText("ROUND FIGURE: " + String.format(Locale.US, "%.0f", (double) Math.round(totalNet())));
        updatePaidDuePreview();
    }

    private void updatePaidDuePreview() {
        String text = paidField.getText().trim();
        if (text.isEmpty() || cart.isEmpty()) { dueLabel.setText("DUE (TK.): "); return; }
        try {
            double paid = Double.parseDouble(text);
            double rounded = Math.round(totalNet());
            if (Double.isFinite(paid) && paid >= 0 && paid <= rounded) dueLabel.setText("DUE (TK.): " + money(rounded - paid));
            else dueLabel.setText("DUE (TK.): INVALID");
        } catch (NumberFormatException ex) { dueLabel.setText("DUE (TK.): INVALID"); }
    }
    private double totalGross() { return cart.stream().mapToDouble(i -> i.quantity * i.sellingPrice).sum(); }
    private double totalDiscount() { return cart.stream().mapToDouble(i -> i.quantity * i.sellingPrice * i.discountPercent / 100.0).sum(); }
    private double totalNet() { return Math.max(0, totalGross() - totalDiscount()); }

    private void confirmSale() {
        if (cart.isEmpty()) { showError("Add at least one medicine before confirming the sale."); return; }
        String salesBy = salesByField.getText().trim();
        String customerId = customerIdField.getText().trim();
        String customerName = customerNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String requestedSaleId = saleIdValueLabel.getText().trim();
        if (requestedSaleId.isEmpty()) { showError("Sale ID could not be generated. Please restart and try again."); saleIdValueLabel.requestFocus(); return; }
        if (requestedSaleId.contains("|") || requestedSaleId.contains("\n") || requestedSaleId.contains("\r")) {
            showError("Sale ID cannot contain the | symbol or a line break."); saleIdValueLabel.requestFocus(); return;
        }
        if (salesBy.isEmpty()) { showError("Sales By cannot be empty."); salesByField.requestFocus(); return; }
        if (!isValidPhone(phone)) {
            showError("Invalid Customer Phone. Enter exactly 11 digits after +88.\nExample: +8801712345678");
            phoneField.requestFocus();
            return;
        }
        if (address.isEmpty()) { showError("Customer Address cannot be empty."); addressField.requestFocus(); return; }
        if (customerName.isEmpty()) { showError("Customer Name cannot be empty."); customerNameField.requestFocus(); return; }
        String paidText = paidField.getText().trim();
        if (paidText.isEmpty()) { showError("Enter the amount in the PAID box before confirming."); paidField.requestFocus(); return; }
        final double paid;
        try { paid = Double.parseDouble(paidText); }
        catch (NumberFormatException ex) { showError("PAID amount must be a valid number."); paidField.requestFocus(); return; }
        double roundedPayable = (double) Math.round(totalNet());
        if (!Double.isFinite(paid) || paid < 0 || paid > roundedPayable + 0.000001) {
            showError("PAID must be between Tk. 0 and the rounded payable amount."); paidField.requestFocus(); return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "ARE YOU SURE TO CONFIRM?", "CONFIRM SALE",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            // Reload latest stock and validate every item again before writing anything.
            List<StockItem> latestStock = readStock();
            Map<String, StockItem> latestById = new HashMap<>();
            for (StockItem s : latestStock) latestById.put(s.id.toLowerCase(Locale.ROOT), s);
            for (CartItem item : cart) {
                StockItem latest = latestById.get(item.stock.id.toLowerCase(Locale.ROOT));
                if (latest == null) throw new IOException("Medicine was removed from stock: " + item.stock.name);
                if (latest.quantity < item.quantity) throw new IOException("Stock changed. Available now for " + latest.name + ": " + latest.quantity);
                if (LocalDate.parse(latest.expiry, DATE_FMT).isBefore(LocalDate.now())) throw new IOException("Medicine has expired: " + latest.name);
                if (item.sellingPrice > latest.mrp + 0.000001) throw new IOException("Selling price now exceeds MRP for " + latest.name);
            }
            final String saleId = saleIdValueLabel.getText().trim();
            if (saleIdExists(saleId)) {
                throw new IOException("Sale ID '" + saleId + "' already exists. Enter a different ID.");
            }
            LocalDateTime now = LocalDateTime.now();
            String date = now.toLocalDate().format(DATE_FMT);
            String time = now.format(TIME_FMT);
            List<StockItem> updatedStock = new ArrayList<>();
            for (StockItem s : latestStock) {
                int sold = 0;
                for (CartItem item : cart) if (item.stock.id.equalsIgnoreCase(s.id)) sold += item.quantity;
                updatedStock.add(s.withQuantity(s.quantity - sold));
            }
            String saleCustomerId = customerIdForPhone(phone);
            List<String> saleLines = new ArrayList<>();
            for (CartItem item : cart) {
                double gross = item.quantity * item.sellingPrice;
                double disc = gross * item.discountPercent / 100.0;
                double net = gross - disc;
                saleLines.add(joinFields(saleId, date, time, salesBy, saleCustomerId, customerName, phone, address,
                        item.stock.id, item.stock.name, item.stock.form, String.valueOf(item.quantity),
                        money(item.stock.mrp), money(item.sellingPrice), money(item.discountPercent), money(gross), money(disc), money(net), money(paid), money(roundedPayable - paid), item.stock.expiry));
            }
            // Prepare stock temp file first; replace stock only after all sale data is prepared.
            Files.createDirectories(DATA_DIR);
            List<String> stockLines = new ArrayList<>();
            for (StockItem s : updatedStock) stockLines.add(joinFields(s.id, s.name, s.form, String.valueOf(s.quantity), money(s.mrp), s.expiry, s.shelf));
            Files.write(STOCK_TEMP, stockLines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            boolean salesExisted = Files.exists(SALES_FILE);
            if (!salesExisted) Files.createFile(SALES_FILE);
            // Append the completed sale before replacing stock; on append failure stock remains unchanged.
            Files.write(SALES_FILE, saleLines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            try {
                Files.move(STOCK_TEMP, STOCK_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(STOCK_TEMP, STOCK_FILE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException stockWriteFailure) {
                // Best-effort rollback of the appended sale if stock replacement fails.
                try {
                    List<String> existing = Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8);
                    existing.removeIf(line -> line.startsWith(saleId + "|"));
                    Files.write(SALES_FILE, existing, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                } catch (IOException rollbackFailure) { stockWriteFailure.addSuppressed(rollbackFailure); }
                throw stockWriteFailure;
            }
            saveCustomerAfterConfirmedSale(phone, customerName, address);
            JOptionPane.showMessageDialog(this,
                    String.format(Locale.US, "SALE COMPLETED\nSale ID: %s\nDate: %s   Time: %s\nPayable: Tk. %.2f\nRound Figure: Tk. %.0f\nPaid: Tk. %.2f\nDue: Tk. %.2f\n\nSales saved to data/sales.txt and stock updated.", saleId, date, time, totalNet(), roundedPayable, paid, roundedPayable - paid),
                    "Successful", JOptionPane.INFORMATION_MESSAGE);
            cart.clear(); refreshCartTable(); refreshStockChoices(); refreshSaleHeader(); refreshInvoiceSaleChoices();
            paidField.setText(""); dueLabel.setText("DUE (TK.): ");
            initializeCustomerId(); customerNameField.setText(""); phoneField.setText("+88"); addressField.setText("");
            quantityField.setText(""); discountField.setText(""); mrpPerUnitField.setText("");
            statusLabel.setText("");
        } catch (IOException | DateTimeParseException ex) {
            showError("Sale was not completed.\n" + ex.getMessage());
        }
    }

    private List<String> getSavedSaleIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        try {
            if (Files.exists(SALES_FILE)) for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
                String[] f = line.split("\\|", -1);
                if (f.length >= 21 && !f[0].trim().isEmpty()) ids.add(f[0].trim());
            }
        } catch (IOException ex) { showError("Could not read saved sales: " + ex.getMessage()); }
        return new ArrayList<>(ids);
    }

    private void chooseSaleForInvoice() {
        List<String> ids = getSavedSaleIds();
        if (ids.isEmpty()) { showInfo("NO SALES RECORDS AVAILABLE"); return; }
        JList<String> saleIdList = new JList<>(ids.toArray(new String[0]));
        saleIdList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        saleIdList.setFont(new Font("Arial", Font.BOLD, 16));
        saleIdList.setVisibleRowCount(Math.min(10, ids.size()));
        saleIdList.setSelectedIndex(ids.size() - 1);
        int answer = JOptionPane.showConfirmDialog(this, new JScrollPane(saleIdList),
                "Select Sale ID to generate invoice", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION || saleIdList.getSelectedValue() == null) return;
        displaySavedInvoice(saleIdList.getSelectedValue());
    }

    private void displaySavedInvoice(String saleId) {
        try {
            List<String[]> rows = new ArrayList<>();
            for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
                String[] f = line.split("\\|", -1);
                if (f.length >= 21 && f[0].equalsIgnoreCase(saleId)) rows.add(f);
            }
            if (rows.isEmpty()) { showError("No saved record found for " + saleId); return; }
            displayInvoice(saleId, rows);
        } catch (IOException ex) { showError("Could not read saved invoice: " + ex.getMessage()); }
    }

    private void chooseSaleToDelete() {
        TotalSalesReportWindow report = new TotalSalesReportWindow();
        report.setVisible(true);
    }

    /** Populate the invoice dropdown from saved sales; one Sale ID may have several medicine rows. */
    private void refreshInvoiceSaleChoices() {
        String previous = (String) invoiceSaleCombo.getSelectedItem();
        invoiceSaleCombo.removeAllItems();
        if (!Files.exists(SALES_FILE)) return;
        try {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
                String[] f = line.split("\\|", -1);
                if (f.length >= 21 && !f[0].trim().isEmpty()) ids.add(f[0].trim());
            }
            for (String id : ids) invoiceSaleCombo.addItem(id);
            if (previous != null) invoiceSaleCombo.setSelectedItem(previous);
            else if (invoiceSaleCombo.getItemCount() > 0) invoiceSaleCombo.setSelectedIndex(invoiceSaleCombo.getItemCount() - 1);
        } catch (IOException ex) { showError("Could not load invoice Sale IDs: " + ex.getMessage()); }
    }

    private void showSelectedInvoice() { chooseSaleForInvoice(); }

    private void displayInvoice(String saleId, List<String[]> rows) {
        String[] first = rows.get(0);
        double gross = 0, discount = 0, net = 0;

        DefaultTableModel invoiceModel = new DefaultTableModel(
                new String[]{"Serial No.", "Medicine Name", "Expiry Date", "Quantity",
                    "MRP (Tk.)", "Discount (%)", "Payable (Tk.)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        int serial = 1;
        for (String[] f : rows) {
            gross += parseMoneySafe(f[15]);
            discount += parseMoneySafe(f[16]);
            net += parseMoneySafe(f[17]);
            invoiceModel.addRow(new Object[]{serial++, f[9], f[20], f[11], f[12], f[14] + "%", f[17]});
        }

        double originalPaid = parseMoneySafe(first[18]);
        double originalDue = parseMoneySafe(first[19]);
        double collectedSoFar = 0.0;
        try {
            collectedSoFar = totalDueCollected(saleId);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not read due collection history: " + ex.getMessage(),
                    "Due Collection", JOptionPane.ERROR_MESSAGE);
        }
        double paid = originalPaid + collectedSoFar;
        double due = Math.max(0.0, originalDue - collectedSoFar);
        double rounded = originalPaid + originalDue;

        JFrame invoice = new JFrame("INVOICE - " + saleId);
        invoice.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        invoice.setSize(1120, 800);
        invoice.setMinimumSize(new Dimension(1000, 720));
        invoice.setLocationRelativeTo(this);

        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));

        JPanel header = new JPanel(new GridLayout(4, 1, 2, 2));
        header.setBackground(Color.WHITE);

        JLabel h1 = new JLabel("INVOICE", SwingConstants.CENTER);
        h1.setFont(new Font("Arial", Font.BOLD, 20));
        h1.setForeground(new Color(25, 70, 150));

        JLabel h2 = new JLabel("SARZIL SAZIB PHARMACY", SwingConstants.CENTER);
        h2.setFont(new Font("Arial", Font.BOLD, 24));

        JLabel h3 = new JLabel("PHONE NUMBER : 01772-4646", SwingConstants.CENTER);
        h3.setFont(new Font("Arial", Font.BOLD, 15));

        JLabel publishingDate = new JLabel(
                "PUBLISHING DATE = " + LocalDate.now().format(DATE_FMT), SwingConstants.CENTER);
        publishingDate.setFont(new Font("Arial", Font.BOLD, 19));
        publishingDate.setForeground(Color.RED);
        publishingDate.setOpaque(true);
        publishingDate.setBackground(new Color(255, 245, 245));
        publishingDate.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED, 2),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        header.add(h1);
        header.add(h2);
        header.add(h3);
        header.add(publishingDate);
        page.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(Color.WHITE);

        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 24, 4));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel saleDetails = new JPanel(new GridLayout(5, 1, 0, 0));
        JPanel customerDetails = new JPanel(new GridLayout(5, 1, 0, 0));
        saleDetails.setBackground(Color.WHITE);
        customerDetails.setBackground(Color.WHITE);
        JLabel saleHeading = new JLabel("SALE INFORMATION");
        JLabel customerHeading = new JLabel("CUSTOMER INFORMATION");
        for (JLabel headingLabel : new JLabel[]{saleHeading, customerHeading}) {
            headingLabel.setFont(new Font("Arial", Font.BOLD, 14));
            headingLabel.setForeground(new Color(0, 145, 55));
        }
        saleDetails.add(saleHeading);
        saleDetails.add(invoiceInfoLabel("Sale ID: " + first[0], Color.BLACK));
        saleDetails.add(invoiceInfoLabel("Selling Date: " + first[1], Color.RED));
        saleDetails.add(invoiceInfoLabel("Time: " + first[2], Color.BLACK));
        saleDetails.add(invoiceInfoLabel("Sales By: " + first[3], Color.BLACK));
        customerDetails.add(customerHeading);
        customerDetails.add(invoiceInfoLabel("Customer ID: " + first[4], Color.BLACK));
        customerDetails.add(invoiceInfoLabel("Customer Name: " + first[5], Color.BLACK));
        customerDetails.add(invoiceInfoLabel("Phone: " + first[6], Color.BLACK));
        customerDetails.add(invoiceInfoLabel("Address: " + first[7], Color.BLACK));
        infoPanel.add(saleDetails);
        infoPanel.add(customerDetails);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        center.add(infoPanel);
        center.add(Box.createVerticalStrut(5));

        JTable invoiceTable = new JTable(invoiceModel);
        invoiceTable.setRowHeight(25);
        invoiceTable.setFont(new Font("Arial", Font.PLAIN, 12));
        invoiceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        invoiceTable.setFillsViewportHeight(true);
        invoiceTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        invoiceTable.getTableHeader().setReorderingAllowed(false);
        invoiceTable.getTableHeader().setBackground(new Color(70, 130, 180));
        invoiceTable.getTableHeader().setForeground(Color.WHITE);

        int[] pref = {75, 240, 120, 85, 110, 105, 130};
        int[] min =  {55, 150, 90, 65, 85, 80, 95};
        for (int i = 0; i < pref.length; i++) {
            invoiceTable.getColumnModel().getColumn(i).setPreferredWidth(pref[i]);
            invoiceTable.getColumnModel().getColumn(i).setMinWidth(min[i]);
        }

        JScrollPane tableScroll = new JScrollPane(invoiceTable);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Header + five medicine rows: a compact table approximately five rows tall.
        tableScroll.setPreferredSize(new Dimension(1040, 155));
        tableScroll.setMinimumSize(new Dimension(700, 155));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155));
        center.add(tableScroll);
        center.add(Box.createVerticalStrut(6));

        JPanel summaryAndCollections = new JPanel(new BorderLayout(24, 4));
        summaryAndCollections.setBackground(Color.WHITE);
        summaryAndCollections.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel totalsPanel = new JPanel(new GridLayout(6, 1, 2, 2));
        totalsPanel.setBackground(Color.WHITE);
        totalsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] totalTexts = {
                "TOTAL (EXCEPT DISCOUNT): Tk. " + money(gross),
                "DISCOUNT: Tk. " + money(discount),
                "PAYABLE: Tk. " + money(net),
                "ROUND FIGURE: Tk. " + money(rounded),
                "PAID: Tk. " + money(paid),
                "DUE: Tk. " + money(due)
        };
        for (String text : totalTexts) {
            JLabel label = new JLabel(text);
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (text.startsWith("DUE:")) label.setForeground(Color.RED);
            totalsPanel.add(label);
        }
        totalsPanel.setPreferredSize(new Dimension(390, 130));
        summaryAndCollections.add(totalsPanel, BorderLayout.WEST);

        // Each successful collection is read from data/due_collection.txt and shown on the invoice.
        // Record format: Collection ID | Sale ID | Customer ID | Customer Name | Phone | Date | Amount | Remaining
        JPanel collectionHistoryPanel = new JPanel();
        collectionHistoryPanel.setLayout(new BoxLayout(collectionHistoryPanel, BoxLayout.Y_AXIS));
        collectionHistoryPanel.setBackground(Color.WHITE);
        collectionHistoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        try {
            if (Files.exists(DUE_FILE)) {
                for (String line : Files.readAllLines(DUE_FILE, StandardCharsets.UTF_8)) {
                    String[] d = line.split("\\|", -1);
                    if (d.length >= 8 && d[1].trim().equalsIgnoreCase(saleId)) {
                        collectionHistoryPanel.add(invoiceInfoLabel("DUE COLLECTION DATE: " + d[5], Color.RED));
                        collectionHistoryPanel.add(invoiceInfoLabel("DUE COLLECTION AMOUNT (TK.): " + money(parseMoneySafe(d[6])), Color.RED));
                        collectionHistoryPanel.add(invoiceInfoLabel("REMAINING DUE (TK.): " + money(parseMoneySafe(d[7])), Color.RED));
                        collectionHistoryPanel.add(Box.createVerticalStrut(5));
                    }
                }
            }
        } catch (IOException ex) {
            collectionHistoryPanel.add(invoiceInfoLabel("Could not load due collection history.", Color.RED));
        }
        if (collectionHistoryPanel.getComponentCount() == 0) {
            collectionHistoryPanel.add(invoiceInfoLabel("NO DUE COLLECTIONS RECORDED", Color.GRAY));
        }
        JScrollPane collectionScroll = new JScrollPane(collectionHistoryPanel);
        collectionScroll.setBorder(BorderFactory.createEmptyBorder());
        collectionScroll.setPreferredSize(new Dimension(470, 150));
        collectionScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        summaryAndCollections.add(collectionScroll, BorderLayout.CENTER);
        center.add(summaryAndCollections);
        center.add(Box.createVerticalStrut(5));

        page.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridLayout(2, 1, 2, 2));
        footer.setBackground(Color.WHITE);
        JLabel f1 = new JLabel("SOLD MEDICINES ARE NOT RETURNABLE.", SwingConstants.CENTER);
        f1.setFont(new Font("Arial", Font.BOLD, 15));
        JLabel f2 = new JLabel("WE APPRECIATE YOUR VISIT", SwingConstants.CENTER);
        f2.setFont(new Font("Arial", Font.BOLD, 15));
        footer.add(f1);
        footer.add(f2);
        page.add(footer, BorderLayout.SOUTH);

        JButton printButton = coloredButton("PRINT", new Color(70, 130, 180), Color.WHITE);
        printButton.addActionListener(e -> {
            try {
                invoiceTable.print();
            } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(invoice, "Could not print invoice: " + ex.getMessage(),
                        "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(printButton);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(page, BorderLayout.CENTER);
        wrapper.add(bottomPanel, BorderLayout.SOUTH);
        invoice.setContentPane(wrapper);
        invoice.setVisible(true);
    }

    private static JLabel invoiceInfoLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static final Path DUE_FILE = DATA_DIR.resolve("due_collection.txt");

    private static double totalDueCollected(String saleId) throws IOException {
        if (!Files.exists(DUE_FILE)) return 0.0;
        double sum = 0.0;
        for (String line : Files.readAllLines(DUE_FILE, StandardCharsets.UTF_8)) {
            String[] f = line.split("\\|", -1);
            // Collection format: Collection ID | Sale ID | Customer ID | Customer Name | Phone | Date | Amount | Remaining
            if (f.length >= 8 && f[1].trim().equalsIgnoreCase(saleId)) {
                sum += parseMoneySafe(f[6]);
            }
        }
        return sum;
    }

    private static Map<String, List<String[]>> readSaleGroups() throws IOException {
        Map<String, List<String[]>> groups = new LinkedHashMap<>();
        if (!Files.exists(SALES_FILE)) return groups;
        for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length >= 21) groups.computeIfAbsent(f[0].trim(), k -> new ArrayList<>()).add(f);
        }
        return groups;
    }

    /** Due report: only sales whose current outstanding due is greater than zero. */
    private final class DueReportWindow extends JFrame {
        private final DefaultTableModel dueModel = new DefaultTableModel(new String[]{
            "Sale ID", "Sales By", "Customer ID", "Customer Name", "Customer Phone",
            "Selling Date", "Medicine Name", "Quantity", "MRP (Tk.)", "Discount (%)",
            "Payable (Tk.)", "Paid (Tk.)", "Due (Tk.)"
        }, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        private final JTable dueTable = new JTable(dueModel);

        DueReportWindow() {
            setTitle("Sarzil-Sazib-Pharmacy - Total Due Report");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(1450, 680);
            setMinimumSize(new Dimension(1250, 550));
            setLocationRelativeTo(AddSaleWindow.this);
            buildUI();
            refreshReport();
        }

        private void buildUI() {
            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            root.setBackground(new Color(240, 248, 255));
            JLabel title = new JLabel("TOTAL DUE REPORT", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 25));
            title.setForeground(new Color(25, 70, 150));
            root.add(title, BorderLayout.NORTH);

            dueTable.setFont(new Font("Arial", Font.PLAIN, 13));
            dueTable.setRowHeight(28);
            dueTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            // Fit all 13 columns in the window: no left-right scrolling.
            dueTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            dueTable.setGridColor(new Color(205, 215, 225));
            dueTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
            dueTable.getTableHeader().setReorderingAllowed(false);
            dueTable.getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean selected, boolean focus, int row, int column) {
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setOpaque(true);
                    label.setFont(new Font("Arial", Font.BOLD, 12));
                    if (column == 12) { label.setBackground(Color.RED); label.setForeground(Color.WHITE); }
                    else { label.setBackground(new Color(70, 130, 180)); label.setForeground(Color.WHITE); }
                    return label;
                }
            });
            dueTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean selected, boolean focus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                    if (column == 12) {
                        c.setBackground(selected ? new Color(255, 180, 180) : new Color(255, 225, 225));
                        c.setForeground(Color.RED);
                        if (c instanceof JLabel) ((JLabel)c).setFont(new Font("Arial", Font.BOLD, 12));
                    } else {
                        c.setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
                        c.setForeground(selected ? table.getSelectionForeground() : Color.BLACK);
                    }
                    return c;
                }
            });
            int[] widths = {70, 75, 78, 100, 105, 88, 145, 62, 78, 76, 90, 75, 75};
            for (int i = 0; i < widths.length; i++) {
                dueTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
                dueTable.getColumnModel().getColumn(i).setMinWidth(i == 6 ? 100 : 55);
            }
            JScrollPane scroll = new JScrollPane(dueTable);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            root.add(scroll, BorderLayout.CENTER);

            JButton collect = coloredButton("COLLECT", new Color(0, 91, 48), Color.WHITE);
            JButton refresh = coloredButton("REFRESH", new Color(0, 190, 75), Color.WHITE);
            JButton back = coloredButton("BACK", new Color(170, 230, 180), Color.BLACK);
            JButton dueCollectionInvoice = coloredButton("DUE COLLECTION INVOICE", Color.RED, Color.WHITE);
            collect.setPreferredSize(new Dimension(160, 45));
            refresh.setPreferredSize(new Dimension(150, 45));
            back.setPreferredSize(new Dimension(150, 45));
            dueCollectionInvoice.setPreferredSize(new Dimension(260, 45));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 5));
            actions.setOpaque(false);
            // Required button order: COLLECT, REFRESH, BACK, DUE COLLECTION INVOICE.
            actions.add(collect);
            actions.add(refresh);
            actions.add(back);
            actions.add(dueCollectionInvoice);
            root.add(actions, BorderLayout.SOUTH);
            collect.addActionListener(e -> chooseDueSale());
            refresh.addActionListener(e -> refreshReport());
            back.addActionListener(e -> dispose());
            dueCollectionInvoice.addActionListener(e -> chooseDueCollectionInvoice());
            setContentPane(root);
        }

        private void refreshReport() {
            dueModel.setRowCount(0);
            try {
                for (Map.Entry<String, List<String[]>> entry : readSaleGroups().entrySet()) {
                    List<String[]> rows = entry.getValue();
                    if (rows.isEmpty()) continue;
                    String[] first = rows.get(0);
                    double initialPaid = parseMoneySafe(first[18]);
                    double initialDue = parseMoneySafe(first[19]);
                    double collected = totalDueCollected(entry.getKey());
                    double currentDue = Math.max(0.0, initialDue - collected);
                    double currentPaid = initialPaid + collected;
                    if (currentDue <= 0.000001) continue;
                    double roundedPayable = initialPaid + initialDue;
                    for (int i = 0; i < rows.size(); i++) {
                        String[] f = rows.get(i);
                        boolean firstMedicine = i == 0;
                        dueModel.addRow(new Object[]{
                            firstMedicine ? f[0] : "",
                            firstMedicine ? f[3] : "",
                            firstMedicine ? f[4] : "",
                            firstMedicine ? f[5] : "",
                            firstMedicine ? f[6] : "",
                            firstMedicine ? f[1] : "",
                            f[9], f[11], money(parseMoneySafe(f[12])), f[14] + "%",
                            firstMedicine ? money(roundedPayable) : "",
                            firstMedicine ? money(currentPaid) : "",
                            firstMedicine ? money(currentDue) : ""
                        });
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not load due report:\n" + ex.getMessage(),
                        "Due Report", JOptionPane.ERROR_MESSAGE);
            }
        }

        /**
         * Lists only Sale IDs that have at least one saved due-collection transaction.
         * Selecting an ID opens the exact same invoice used by the normal INVOICE button.
         */
        private void chooseDueCollectionInvoice() {
            LinkedHashSet<String> collectedSaleIds = new LinkedHashSet<>();
            try {
                if (Files.exists(DUE_FILE)) {
                    for (String line : Files.readAllLines(DUE_FILE, StandardCharsets.UTF_8)) {
                        if (line.trim().isEmpty()) continue;
                        String[] fields = line.split("\\|", -1);
                        // Collection format: Collection ID | Sale ID | Customer ID | Customer Name | Phone | Date | Amount | Remaining
                        if (fields.length >= 8 && !fields[1].trim().isEmpty()) {
                            collectedSaleIds.add(fields[1].trim());
                        }
                    }
                }
                Map<String, List<String[]>> savedSales = readSaleGroups();
                List<String> validIds = new ArrayList<>();
                for (String id : collectedSaleIds) {
                    if (savedSales.containsKey(id)) validIds.add(id);
                }
                if (validIds.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "NO SALE ID WITH A SAVED DUE COLLECTION FOUND",
                            "Due Collection Invoice", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                JList<String> list = new JList<>(validIds.toArray(new String[0]));
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                list.setFont(new Font("Arial", Font.BOLD, 16));
                list.setVisibleRowCount(Math.min(10, validIds.size()));
                list.setSelectedIndex(validIds.size() - 1);
                int answer = JOptionPane.showConfirmDialog(this, new JScrollPane(list),
                        "Select Sale ID with Due Collection", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
                if (answer == JOptionPane.OK_OPTION && list.getSelectedValue() != null) {
                    displaySavedInvoice(list.getSelectedValue());
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not load due collection invoice list:\n" + ex.getMessage(),
                        "Due Collection Invoice", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void chooseDueSale() {
            if (dueModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "NO SALES WITH OUTSTANDING DUE", "Due Collection", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            DefaultListModel<String> ids = new DefaultListModel<>();
            Set<String> added = new LinkedHashSet<>();
            for (int i = 0; i < dueModel.getRowCount(); i++) {
                String id = String.valueOf(dueModel.getValueAt(i, 0)).trim();
                if (!id.isEmpty() && added.add(id)) ids.addElement(id);
            }
            JList<String> list = new JList<>(ids);
            list.setFont(new Font("Arial", Font.BOLD, 17));
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectedIndex(0);
            JScrollPane pane = new JScrollPane(list);
            pane.setPreferredSize(new Dimension(320, Math.min(400, 70 + ids.size() * 28)));
            int result = JOptionPane.showConfirmDialog(this, pane, "Select Sale ID with Due",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION && list.getSelectedValue() != null) {
                new DueCollectionWindow(list.getSelectedValue(), this).setVisible(true);
            }
        }
    }

    /** Due collection form: read-only sale details plus editable collection amount. */
    private final class DueCollectionWindow extends JFrame {
        private final JTextField dateField = new JTextField(LocalDate.now().format(DATE_FMT), 12);
        private final JTextField saleIdField = new JTextField(12);
        private final JTextField customerIdField = new JTextField(12);
        private final JTextField customerNameField = new JTextField(18);
        private final JTextField phoneField = new JTextField(14);
        private final JTextField payableField = new JTextField(12);
        private final JTextField paidField = new JTextField(12);
        private final JTextField dueField = new JTextField(12);
        private final JTextField collectionField = new JTextField(12);
        private final JTextField remainingField = new JTextField(12);
        private final DueReportWindow parent;
        private String selectedSaleId = "";

        DueCollectionWindow(String saleId, DueReportWindow parent) {
            this.parent = parent;
            setTitle("Sarzil-Sazib-Pharmacy - Due Collection");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(780, 660);
            setMinimumSize(new Dimension(700, 600));
            setLocationRelativeTo(parent);
            buildUI();
            loadSale(saleId);
        }

        private void buildUI() {
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
            root.setBackground(new Color(240, 248, 255));

            JPanel form = new JPanel(new GridLayout(0, 2, 12, 10));
            form.setBackground(Color.WHITE);
            form.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(25, 80, 180), 2),
                    BorderFactory.createEmptyBorder(15, 18, 15, 18)));
            addDueField(form, "Collection Date:", dateField);
            addDueField(form, "Sale ID:", saleIdField);
            addDueField(form, "Customer ID:", customerIdField);
            addDueField(form, "Customer Name:", customerNameField);
            addDueField(form, "Customer Phone:", phoneField);
            addDueField(form, "Payable (Round Figure) (Tk.):", payableField);
            addDueField(form, "Paid (Tk.):", paidField);
            addDueField(form, "Due (Tk.):", dueField);
            addDueField(form, "Collection Amount (Tk.):", collectionField);
            addDueField(form, "Remaining Due (Tk.):", remainingField);
            for (Component c : form.getComponents()) c.setFont(new Font("Arial", Font.BOLD, 14));
            for (JTextField f : new JTextField[]{dateField, saleIdField, customerIdField, customerNameField, phoneField, payableField, paidField, dueField, remainingField}) {
                f.setEditable(false);
                f.setBackground(new Color(238, 238, 238));
            }
            dateField.setForeground(Color.RED);
            dateField.setFont(new Font("Arial", Font.BOLD, 14));
            collectionField.setFont(new Font("Arial", Font.BOLD, 16));
            collectionField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { updateRemainingPreview(); }
                @Override public void removeUpdate(DocumentEvent e) { updateRemainingPreview(); }
                @Override public void changedUpdate(DocumentEvent e) { updateRemainingPreview(); }
            });
            root.add(form, BorderLayout.CENTER);

            JButton done = coloredButton("DONE", new Color(0, 90, 45), Color.WHITE);
            JButton clear = coloredButton("CLEAR", new Color(0, 190, 70), Color.WHITE);
            JButton back = coloredButton("BACK", new Color(170, 230, 180), Color.BLACK);
            for (JButton b : new JButton[]{done, clear, back}) b.setPreferredSize(new Dimension(140, 46));
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 5));
            actions.setOpaque(false);
            actions.add(done); actions.add(clear); actions.add(back);
            root.add(actions, BorderLayout.SOUTH);
            done.addActionListener(e -> saveCollection());
            clear.addActionListener(e -> collectionField.setText(""));
            back.addActionListener(e -> dispose());
            setContentPane(root);
        }

        private void addDueField(JPanel panel, String label, JTextField field) {
            JLabel l = new JLabel(label);
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            if (label.startsWith("Collection Date")) l.setForeground(Color.RED);
            panel.add(l); panel.add(field);
        }

        private void loadSale(String saleId) {
            try {
                List<String[]> rows = readSaleGroups().get(saleId);
                if (rows == null || rows.isEmpty()) throw new IOException("Sale ID not found: " + saleId);
                String[] first = rows.get(0);
                double originalPaid = parseMoneySafe(first[18]);
                double originalDue = parseMoneySafe(first[19]);
                double collected = totalDueCollected(saleId);
                double currentDue = Math.max(0.0, originalDue - collected);
                selectedSaleId = first[0].trim();
                dateField.setText(LocalDate.now().format(DATE_FMT));
                saleIdField.setText(first[0]);
                customerIdField.setText(first[4]);
                customerNameField.setText(first[5]);
                phoneField.setText(first[6]);
                payableField.setText(money(originalPaid + originalDue));
                paidField.setText(money(originalPaid + collected));
                dueField.setText(money(currentDue));
                remainingField.setText(money(currentDue));
                if (currentDue <= 0.000001) {
                    JOptionPane.showMessageDialog(this, "This sale is already fully paid.", "Due Collection", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not load sale:\n" + ex.getMessage(), "Due Collection", JOptionPane.ERROR_MESSAGE);
                dispose();
            }
        }

        private void updateRemainingPreview() {
            try {
                double currentDue = Double.parseDouble(dueField.getText().trim());
                double amount = Double.parseDouble(collectionField.getText().trim());
                if (Double.isFinite(amount) && amount >= 0) remainingField.setText(money(Math.max(0.0, currentDue - amount)));
                else remainingField.setText(money(currentDue));
            } catch (Exception ex) {
                if (!dueField.getText().trim().isEmpty()) remainingField.setText(dueField.getText().trim());
            }
        }

        private void saveCollection() {
            try {
                if (selectedSaleId.isEmpty()) throw new IllegalArgumentException("Select a Sale ID with due.");
                double currentDue = Double.parseDouble(dueField.getText().trim());
                double amount = Double.parseDouble(collectionField.getText().trim());
                if (!Double.isFinite(amount) || amount <= 0) throw new IllegalArgumentException("Collection amount must be greater than zero.");
                if (amount > currentDue + 0.000001) throw new IllegalArgumentException("Collection amount cannot exceed the current due.");
                double remaining = Math.max(0.0, currentDue - amount);
                Files.createDirectories(DATA_DIR);
                if (!Files.exists(DUE_FILE)) Files.createFile(DUE_FILE);
                String collectionId = "DC-" + (Files.readAllLines(DUE_FILE, StandardCharsets.UTF_8).size() + 1);
                LocalDate collectionDate;
                try {
                    collectionDate = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Collection Date must use DD-MM-YYYY format.");
                }
                String record = joinFields(collectionId, selectedSaleId, customerIdField.getText(),
                        customerNameField.getText(), phoneField.getText(), collectionDate.format(DATE_FMT),
                        money(amount), money(remaining));
                Files.write(DUE_FILE, Collections.singletonList(record), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                JOptionPane.showMessageDialog(this, "DUE COLLECTION SUCCESSFUL\nSale ID: " + selectedSaleId
                        + "\nCollected: Tk. " + money(amount) + "\nRemaining Due: Tk. " + money(remaining),
                        "Successful", JOptionPane.INFORMATION_MESSAGE);
                parent.refreshReport();
                dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Enter a valid collection amount.", "Due Collection", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Due Collection", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Full-screen-capable report window used to inspect and delete completed sales. */
    private final class TotalSalesReportWindow extends JFrame {
       private final DefaultTableModel reportModel = new DefaultTableModel(new String[]{
"Sale ID", "Date", "Sales By", "Medicine ID", "Medicine Name",
"Form", "Quantity", "MRP (Tk.)", "Discount (%)",
"PAYABLE (ROUND FIGURE)", "Paid (Tk.)", "Due (Tk.)", "Sale ID Key"
}, 0) {
@Override
public boolean isCellEditable(int r, int c) {
return false;
}
};

        private final JTable reportTable = new JTable(reportModel);
        TotalSalesReportWindow() {
            setTitle("TOTAL SALES REPORT"); setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(1450, 700); setMinimumSize(new Dimension(900, 500)); setLocationRelativeTo(AddSaleWindow.this);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            JLabel title = new JLabel("TOTAL SALES REPORT", SwingConstants.CENTER); title.setFont(new Font("Arial", Font.BOLD, 26)); title.setForeground(new Color(25,70,150));
            reportTable.setFont(new Font("Arial", Font.PLAIN, 14)); reportTable.setRowHeight(27); reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            reportTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
            reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            reportTable.setGridColor(new Color(205,215,225));
            reportTable.getColumnModel().removeColumn(reportTable.getColumnModel().getColumn(12));
            JScrollPane sp = new JScrollPane(reportTable);
            JButton delete = coloredButton(
"DELETE SALE",
new Color(210, 45, 45),
Color.WHITE
);

JButton refresh = coloredButton(
"REFRESH",
new Color(0, 190, 75),
Color.WHITE
);

JButton close = coloredButton(
"BACK",
new Color(190, 220, 245),
Color.BLACK
);

delete.setPreferredSize(new Dimension(260, 48));
refresh.setPreferredSize(new Dimension(180, 48));
close.setPreferredSize(new Dimension(150, 48));

delete.setFont(new Font("Arial", Font.BOLD, 16));
refresh.setFont(new Font("Arial", Font.BOLD, 16));
close.setFont(new Font("Arial", Font.BOLD, 16));

delete.addActionListener(e -> chooseSaleIdAndDelete());

refresh.addActionListener(e -> loadReport());

close.addActionListener(e -> dispose());

JPanel actions = new JPanel(
new FlowLayout(FlowLayout.CENTER, 15, 10)
);

actions.add(delete);
actions.add(refresh);
actions.add(close);

add(title, BorderLayout.NORTH);
add(sp, BorderLayout.CENTER);
add(actions, BorderLayout.SOUTH);

loadReport();

        }
        private void loadReport() {
            reportModel.setRowCount(0);
            try {
                if (!Files.exists(SALES_FILE)) { JOptionPane.showMessageDialog(this,"NO SALES RECORDS AVAILABLE","Total Sales Report",JOptionPane.INFORMATION_MESSAGE); return; }
                List<String> lines = Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8);
                if (lines.stream().noneMatch(x -> !x.trim().isEmpty())) { JOptionPane.showMessageDialog(this,"NO SALES RECORDS AVAILABLE","Total Sales Report",JOptionPane.INFORMATION_MESSAGE); return; }
                Map<String,List<String[]>> groups = new LinkedHashMap<>();
                for (String line : lines) { if (line.trim().isEmpty()) continue; String[] f=line.split("\\|",-1); if(f.length>=21) groups.computeIfAbsent(f[0],k->new ArrayList<>()).add(f); }
               
                for (Map.Entry<String, List<String[]>> entry : groups.entrySet()) {

List<String[]> rows = entry.getValue();

if (rows.isEmpty()) {
    continue;
}

String[] first = rows.get(0);

// Original sale payment information: read only once per Sale ID.
double originalPaid = parseMoneySafe(first[18]);
double originalDue = parseMoneySafe(first[19]);

// Calculate subsequent due collections for this Sale ID.
double collected = 0.0;

try {
    collected = totalDueCollected(entry.getKey());
} catch (IOException ex) {
    JOptionPane.showMessageDialog(
        this,
        "Could not read due collection records:\n" + ex.getMessage(),
        "Due Report Error",
        JOptionPane.ERROR_MESSAGE
    );
    return;
}

// Payable Round Figure = original paid + original due.
double roundedPayable = originalPaid + originalDue;

// Updated values after due collection.
double updatedPaid = originalPaid + collected;
double updatedDue = Math.max(0.0, originalDue - collected);

for (int i = 0; i < rows.size(); i++) {

    String[] f = rows.get(i);
    boolean firstMedicine = (i == 0);

    reportModel.addRow(new Object[]{

        firstMedicine ? f[0] : "",
        firstMedicine ? f[1] : "",
        firstMedicine ? f[3] : "",

        f[8],
        f[9],
        f[10],
        f[11],
        f[12],
        f[14],

        firstMedicine ? money(roundedPayable) : "",
        firstMedicine ? money(updatedPaid) : "",
        firstMedicine ? money(updatedDue) : "",

        f[0]
    });
}

}

                
                int[] widths={100,110,110,100,145,105,90,100,105,145,105,105};
                for(int c=0;c<reportTable.getColumnCount();c++) reportTable.getColumnModel().getColumn(c).setPreferredWidth(widths[c]);
            } catch(IOException ex) { JOptionPane.showMessageDialog(this,"Cannot read sales records: "+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
        }
        private void chooseSaleIdAndDelete() {
            try {
                if (!Files.exists(SALES_FILE)) {
                    JOptionPane.showMessageDialog(this, "NO SALES RECORDS AVAILABLE", "Delete Sale", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                LinkedHashSet<String> ids = new LinkedHashSet<>();
                for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
                    if (line.trim().isEmpty()) continue;
                    String[] f = line.split("\\|", -1);
                    if (f.length >= 21 && !f[0].trim().isEmpty()) ids.add(f[0].trim());
                }
                if (ids.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "NO SALES RECORDS AVAILABLE", "Delete Sale", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                JList<String> list = new JList<>(ids.toArray(new String[0]));
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                list.setFont(new Font("Arial", Font.BOLD, 18));
                list.setVisibleRowCount(Math.min(10, ids.size()));
                list.setSelectedIndex(0);
                JScrollPane scroller = new JScrollPane(list);
                scroller.setPreferredSize(new Dimension(300, Math.min(360, 48 + ids.size() * 30)));
                int result = JOptionPane.showConfirmDialog(this, scroller, "Select Sale ID to Delete",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION && list.getSelectedValue() != null) {
                    deleteSaleById(list.getSelectedValue());
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Cannot load Sale IDs: " + ex.getMessage(), "Delete Sale", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void deleteSelectedSale() {
            int row = reportTable.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(this, "Select a Sale ID row first.", "Delete Sale", JOptionPane.WARNING_MESSAGE); return; }
            deleteSaleById(String.valueOf(reportModel.getValueAt(row, 12)));
        }
        private void deleteSaleById(String id) {
            int answer = JOptionPane.showConfirmDialog(this, "WOULD YOU WANT TO DELETE \"" + id + "\" SALE ID?\nThe sold quantities will be returned to stock.", "Confirm Sale Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
            try {
                List<String> saleLines = Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8);
                List<String> keep = new ArrayList<>(); Map<String,Integer> restore = new LinkedHashMap<>(); int removed=0;
                for (String line : saleLines) {
                    if (line.trim().isEmpty()) continue; String[] f=line.split("\\|",-1);
                    if (f.length >= 21 && f[0].equalsIgnoreCase(id)) {
                        int q=Integer.parseInt(f[11]); restore.merge(f[8], q, Integer::sum); removed++;
                    } else keep.add(line);
                }
                if (removed==0) { JOptionPane.showMessageDialog(this,"NO SALES RECORDS AVAILABLE","Total Sales Report",JOptionPane.INFORMATION_MESSAGE); return; }
                List<String> stockLines = Files.exists(STOCK_FILE) ? Files.readAllLines(STOCK_FILE, StandardCharsets.UTF_8) : new ArrayList<>();
                List<String> updated = new ArrayList<>(); StringBuilder restoredNames = new StringBuilder();
                for (String stockLine : stockLines) {
                    if (stockLine.trim().isEmpty()) continue; String[] sf=stockLine.split("\\|",-1);
                    if (sf.length != 7) throw new IOException("Invalid stock record; expected 7 fields.");
                    Integer add=restore.remove(sf[0]);
                    if (add != null) { int newQty=Integer.parseInt(sf[3])+add; sf[3]=String.valueOf(newQty); restoredNames.append(sf[1]).append(": +").append(add).append(" pcs (now ").append(newQty).append(")\n"); }
                    updated.add(String.join("|",sf));
                }
                if (!restore.isEmpty()) throw new IOException("Some sold medicine IDs no longer exist in stock: " + restore.keySet());
                Files.createDirectories(DATA_DIR); Path salesTemp=DATA_DIR.resolve("sales_delete_temp.txt"); Path stockTemp=DATA_DIR.resolve("stock_delete_temp.txt");
                Files.write(salesTemp, keep, StandardCharsets.UTF_8); Files.write(stockTemp, updated, StandardCharsets.UTF_8);
                Files.move(stockTemp, STOCK_FILE, StandardCopyOption.REPLACE_EXISTING); Files.move(salesTemp, SALES_FILE, StandardCopyOption.REPLACE_EXISTING);
                showStockRestoredDialog(id, restoredNames.toString());
                loadReport();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Sale could not be deleted.\n" + ex.getMessage(), "Delete Error", JOptionPane.ERROR_MESSAGE); }
        }

        private void showStockRestoredDialog(String saleId, String restoredLines) {
            JDialog dialog = new JDialog(this, "Stock Restored Successfully", true);
            dialog.setSize(650, 430);
            dialog.setMinimumSize(new Dimension(600, 380));
            dialog.setLocationRelativeTo(this);
            JPanel panel = new JPanel(new BorderLayout(12, 12));
            panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            panel.setBackground(new Color(235, 248, 255));

            JLabel title = new JLabel("SALE ID " + saleId + " DELETED SUCCESSFULLY", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 22));
            title.setForeground(new Color(0, 110, 60));
            title.setBorder(BorderFactory.createEmptyBorder(5, 4, 12, 4));
            panel.add(title, BorderLayout.NORTH);

            JPanel linesPanel = new JPanel();
            linesPanel.setLayout(new BoxLayout(linesPanel, BoxLayout.Y_AXIS));
            linesPanel.setBackground(Color.WHITE);
            linesPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(30, 90, 190), 2),
                    BorderFactory.createEmptyBorder(12, 16, 12, 16)));
            JLabel heading = new JLabel("STOCK QUANTITY RESTORED:");
            heading.setFont(new Font("Arial", Font.BOLD, 18));
            heading.setForeground(new Color(25, 70, 150));
            heading.setAlignmentX(Component.LEFT_ALIGNMENT);
            linesPanel.add(heading);
            linesPanel.add(Box.createVerticalStrut(10));

            for (String line : restoredLines.split("\\R")) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(":\\s*\\+", 2);
                String medicine = parts[0].trim();
                String rest = parts.length > 1 ? "+" + parts[1].trim() : line.trim();
                JLabel medicineLabel = new JLabel("NEW RESTORED QUANTITY OF " + medicine + " IS");
                medicineLabel.setFont(new Font("Arial", Font.BOLD, 17));
                medicineLabel.setForeground(Color.RED);
                medicineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel valueLabel = new JLabel(rest);
                valueLabel.setFont(new Font("Arial", Font.BOLD, 18));
                valueLabel.setForeground(Color.BLACK);
                valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                linesPanel.add(medicineLabel);
                linesPanel.add(Box.createVerticalStrut(3));
                linesPanel.add(valueLabel);
                linesPanel.add(Box.createVerticalStrut(12));
            }
            panel.add(linesPanel, BorderLayout.CENTER);

            JButton ok = coloredButton("OK", new Color(45, 105, 210), Color.WHITE);
            ok.setFont(new Font("Arial", Font.BOLD, 16));
            ok.setPreferredSize(new Dimension(110, 42));
            ok.addActionListener(e -> dialog.dispose());
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bottom.setOpaque(false);
            bottom.add(ok);
            panel.add(bottom, BorderLayout.SOUTH);
            dialog.setContentPane(panel);
            dialog.setVisible(true);
        }
    }

    private static double parseMoneySafe(String value) { try { return Double.parseDouble(value.trim()); } catch (Exception ex) { return 0.0; } }

    private boolean saleIdExists(String saleId) throws IOException {
        if (!Files.exists(SALES_FILE)) return false;
        for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
            if (line.trim().isEmpty()) continue;
            String[] fields = line.split("\\|", -1);
            if (fields.length > 0 && fields[0].trim().equalsIgnoreCase(saleId.trim())) return true;
        }
        return false;
    }

    private String nextSaleId() throws IOException {
        Files.createDirectories(DATA_DIR);

        int counter = 0;
        if (Files.exists(SALE_COUNTER_FILE)) {
            String value = Files.readString(SALE_COUNTER_FILE, StandardCharsets.UTF_8).trim();
            if (!value.isEmpty()) counter = Integer.parseInt(value);
        }

        // Also inspect existing sales so an older project without a counter file
        // continues from the highest Sale ID already stored.
        int maxSaved = 0;
        if (Files.exists(SALES_FILE)) {
            for (String line : Files.readAllLines(SALES_FILE, StandardCharsets.UTF_8)) {
                if (line.trim().isEmpty()) continue;
                String[] fields = line.split("\\|", -1);
                if (fields.length > 0 && fields[0].trim().matches("SAL-\\d+")) {
                    maxSaved = Math.max(maxSaved,
                            Integer.parseInt(fields[0].trim().substring(4)));
                }
            }
        }

        int next = Math.max(counter, maxSaved) + 1;
        // Reserve and persist the ID immediately. Deleted/cancelled sales cannot
        // cause this ID to be issued again.
        Files.writeString(SALE_COUNTER_FILE, String.valueOf(next), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return String.format(Locale.ROOT, "SAL-%03d", next);
    }

    private List<StockItem> readStock() throws IOException {
        Files.createDirectories(DATA_DIR);
        if (!Files.exists(STOCK_FILE)) Files.createFile(STOCK_FILE);
        List<StockItem> result = new ArrayList<>();
        int lineNo = 0;
        for (String line : Files.readAllLines(STOCK_FILE, StandardCharsets.UTF_8)) {
            lineNo++;
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 7) throw new IOException("Invalid stock.txt record at line " + lineNo + ". Each record must have 7 fields separated by |.");
            try {
                int quantity = Integer.parseInt(f[3].trim());
                double mrp = Double.parseDouble(f[4].trim());
                if (quantity < 0 || mrp < 0 || !Double.isFinite(mrp)) throw new NumberFormatException();
                LocalDate.parse(f[5].trim(), DATE_FMT);
                result.add(new StockItem(f[0].trim(), f[1].trim(), f[2].trim(), quantity, mrp, f[5].trim(), f[6].trim()));
            } catch (NumberFormatException | DateTimeParseException ex) {
                throw new IOException("Invalid quantity, MRP or expiry date in stock.txt at line " + lineNo + ".", ex);
            }
        }
        return result;
    }

    private static StockItem findByName(List<StockItem> items, String name) {
        for (StockItem item : items) if (item.name.equalsIgnoreCase(name.trim())) return item;
        return null;
    }
    private static String joinFields(String... fields) {
        StringJoiner joiner = new StringJoiner("|");
        for (String field : fields) joiner.add(field == null ? "" : field.replace('|', '/').replace('\n', ' ').replace('\r', ' '));
        return joiner.toString();
    }
    private static String money(double value) { return String.format(Locale.US, "%.2f", value); }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "Sales Management - Error", JOptionPane.ERROR_MESSAGE); }
    private void showInfo(String message) { JOptionPane.showMessageDialog(this, message, "Sales Management", JOptionPane.INFORMATION_MESSAGE); }

    private static final class StockItemRenderer extends JLabel implements ListCellRenderer<StockItem> {
        StockItemRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(5, 7, 5, 7));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends StockItem> list, StockItem value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(Color.WHITE);
                setForeground(new Color(30, 45, 65));
            }

            if (value == null) {
                setText("");
                setPreferredSize(new Dimension(600, 28));
            } else if (index < 0) {
                // Keep the closed combo box concise; expanded rows show all seven stock fields.
                setText(value.name);
                setFont(new Font("Arial", Font.PLAIN, 13));
                setPreferredSize(new Dimension(400, 28));
            } else {
                setFont(new Font("Arial", Font.PLAIN, 14));
                setText("<html><b>Medicine ID:</b> " + html(value.id)
                        + " &nbsp; | &nbsp; <b>Medicine Name:</b> " + html(value.name)
                        + "<br><b>Form:</b> " + html(value.form)
                        + " &nbsp; | &nbsp; <b>Quantity:</b> " + value.quantity + " pcs"
                        + " &nbsp; | &nbsp; <b>MRP:</b> Tk. " + String.format(Locale.US, "%.2f", value.mrp)
                        + "<br><b>Expiry Date:</b> " + html(value.expiry)
                        + " &nbsp; | &nbsp; <b>Shelf Number:</b> " + html(value.shelf) + "</html>");
                setPreferredSize(new Dimension(700, 58));
            }
            return this;
        }

        private static String html(String value) {
            return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    private static final class StockItem {
        final String id, name, form, expiry, shelf;
        final int quantity;
        final double mrp;
        StockItem(String id, String name, String form, int quantity, double mrp, String expiry, String shelf) {
            this.id=id; this.name=name; this.form=form; this.quantity=quantity; this.mrp=mrp; this.expiry=expiry; this.shelf=shelf;
        }
        StockItem withQuantity(int newQuantity) { return new StockItem(id, name, form, newQuantity, mrp, expiry, shelf); }
    }
    private static final class CartItem {
        StockItem stock;
        int quantity;
        double sellingPrice, discountPercent;
        CartItem(StockItem stock, int quantity, double sellingPrice, double discountPercent) {
            this.stock=stock; this.quantity=quantity; this.sellingPrice=sellingPrice; this.discountPercent=discountPercent;
        }
    }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AddSaleWindow().setVisible(true));
    }
}