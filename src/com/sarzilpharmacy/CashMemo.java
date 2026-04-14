package com.sarzilpharmacy;

import com.sarzilpharmacy.utils.NumberToWordConverter;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class CashMemo extends JFrame {
    private final List<Medicine> medicines; // Made final
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel totalLabel, payableLabel, paidLabel, dueLabel, takaInWordsLabel, customerInfoLabel, receivedByLabel;
    private JTextField paidField, customerField, customerNumberField, receivedByField;
    private ImageIcon logo;

    public CashMemo() {
        medicines = new ArrayList<>();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Sarzil Pharmacy - Cash Memo");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 900);
        setLocationRelativeTo(null);

        // Load logo
        try {
            logo = new ImageIcon("resources/logo.png");
            Image img = logo.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            logo = new ImageIcon(img);
        } catch (Exception e) {
            System.out.println("Logo not found, using text instead");
        }

        createLayout();
    }

    private void createLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Header Section - 3 Column Layout
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Table Section - Fixed 200px height
        JPanel tablePanel = createTablePanel();
        mainPanel.add(tablePanel, BorderLayout.CENTER);

        // Footer Section
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridLayout(1, 3, 20, 10)); // 3 columns
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        headerPanel.setBackground(new Color(240, 248, 255));
        headerPanel.setPreferredSize(new Dimension(900, 180));

        // LEFT PANEL: Bill Information (3 rows)
        JPanel leftPanel = createLeftPanel();
        headerPanel.add(leftPanel);

        // CENTER PANEL: Pharmacy Information
        JPanel centerPanel = createCenterPanel();
        headerPanel.add(centerPanel);

        // RIGHT PANEL: Customer Information (3 rows)
        JPanel rightPanel = createRightPanel();
        headerPanel.add(rightPanel);

        return headerPanel;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBackground(new Color(240, 248, 255));

        // Left Title
        JLabel leftTitle = new JLabel("BILL INFORMATION", JLabel.CENTER);
        leftTitle.setFont(new Font("Arial", Font.BOLD, 16));
        leftTitle.setForeground(new Color(70, 130, 180));
        leftTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        leftPanel.add(leftTitle, BorderLayout.NORTH);

        // Bill Information - 3 rows
        JPanel billInfoPanel = new JPanel(new GridLayout(3, 2, 8, 12));
        billInfoPanel.setBackground(new Color(240, 248, 255));

        String currentDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        String currentTime = new SimpleDateFormat("hh:mm a").format(new Date());

        // Row 1: Bill No
        billInfoPanel.add(createBoldLabel("Bill No:"));
        JLabel billNoLabel = new JLabel("SL-" + (System.currentTimeMillis() % 10000));
        billNoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        billInfoPanel.add(billNoLabel);

        // Row 2: Date
        billInfoPanel.add(createBoldLabel("Date:"));
        JLabel dateLabel = new JLabel(currentDate);
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        billInfoPanel.add(dateLabel);

        // Row 3: Time
        billInfoPanel.add(createBoldLabel("Time:"));
        JLabel timeLabel = new JLabel(currentTime);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        billInfoPanel.add(timeLabel);

        leftPanel.add(billInfoPanel, BorderLayout.CENTER);

        return leftPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(new Color(240, 248, 255));

        // Logo Section (Top)
        JPanel logoPanel = new JPanel();
        logoPanel.setBackground(new Color(240, 248, 255));
        if (logo != null) {
            JLabel logoLabel = new JLabel(logo);
            logoPanel.add(logoLabel);
        } else {
            JLabel textLogo = new JLabel("🏥");
            textLogo.setFont(new Font("Arial", Font.BOLD, 24));
            textLogo.setForeground(Color.BLUE);
            logoPanel.add(textLogo);
        }
        centerPanel.add(logoPanel, BorderLayout.NORTH);

        // Pharmacy Information (Center)
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 8, 8));
        infoPanel.setBackground(new Color(240, 248, 255));

        JLabel pharmacyName = new JLabel("Sarzil Pharmacy", JLabel.CENTER);
        pharmacyName.setFont(new Font("Arial", Font.BOLD, 24));
        pharmacyName.setForeground(Color.BLUE);

        JLabel address = new JLabel("Uposhohor, Bogura", JLabel.CENTER);
        address.setFont(new Font("Arial", Font.PLAIN, 16));
        address.setForeground(Color.DARK_GRAY);

        JLabel mobile = new JLabel("Mobile: +88 01772004646", JLabel.CENTER);
        mobile.setFont(new Font("Arial", Font.PLAIN, 14));
        mobile.setForeground(Color.DARK_GRAY);

        infoPanel.add(pharmacyName);
        infoPanel.add(address);
        infoPanel.add(mobile);
        centerPanel.add(infoPanel, BorderLayout.CENTER);

        // Cash Memo Title (Bottom)
        JLabel cashMemo = new JLabel("CASH MEMO", JLabel.CENTER);
        cashMemo.setFont(new Font("Arial", Font.BOLD, 22));
        cashMemo.setForeground(Color.RED);
        cashMemo.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        centerPanel.add(cashMemo, BorderLayout.SOUTH);

        return centerPanel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBackground(new Color(240, 248, 255));

        // Right Title
        JLabel rightTitle = new JLabel("CUSTOMER INFORMATION", JLabel.CENTER);
        rightTitle.setFont(new Font("Arial", Font.BOLD, 16));
        rightTitle.setForeground(new Color(70, 130, 180));
        rightTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        rightPanel.add(rightTitle, BorderLayout.NORTH);

        // Customer Information - 3 rows with input fields
        JPanel customerInfoPanel = new JPanel(new GridLayout(3, 2, 8, 12));
        customerInfoPanel.setBackground(new Color(240, 248, 255));

        // Create DocumentListener with @Override annotations
        DocumentListener customerDocListener = new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) { updateCustomerInfo(); }

            @Override
            public void removeUpdate(DocumentEvent e) { updateCustomerInfo(); }

            @Override
            public void insertUpdate(DocumentEvent e) { updateCustomerInfo(); }
        };

        DocumentListener receivedByDocListener = new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) { updateReceivedBy(); }

            @Override
            public void removeUpdate(DocumentEvent e) { updateReceivedBy(); }

            @Override
            public void insertUpdate(DocumentEvent e) { updateReceivedBy(); }
        };

        // Row 1: Customer Name
        customerInfoPanel.add(createBoldLabel("Customer Name:"));
        customerField = new JTextField("Cash Customer");
        customerField.setFont(new Font("Arial", Font.PLAIN, 12));
        customerField.getDocument().addDocumentListener(customerDocListener);
        customerInfoPanel.add(customerField);

        // Row 2: Customer Phone
        customerInfoPanel.add(createBoldLabel("Customer Phone:"));
        customerNumberField = new JTextField("01XXXXXXXXX");
        customerNumberField.setFont(new Font("Arial", Font.PLAIN, 12));
        customerNumberField.getDocument().addDocumentListener(customerDocListener);
        customerInfoPanel.add(customerNumberField);

        // Row 3: Received By
        customerInfoPanel.add(createBoldLabel("Received By:"));
        receivedByField = new JTextField("Sarzil");
        receivedByField.setFont(new Font("Arial", Font.PLAIN, 12));
        receivedByField.getDocument().addDocumentListener(receivedByDocListener);
        customerInfoPanel.add(receivedByField);

        rightPanel.add(customerInfoPanel, BorderLayout.CENTER);

        return rightPanel;
    }

    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Table columns
        String[] columns = {
                "SL No", "Medicines", "Price (Tk)", "Quantity",
                "Total Price", "Discount %", "Discount (Tk)", "Net Price"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.setFont(new Font("Arial", Font.PLAIN, 11));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(70, 130, 180));
        table.getTableHeader().setForeground(Color.WHITE);

        // FIXED: Table height 200px
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(900, 200));
        scrollPane.setMinimumSize(new Dimension(900, 200));
        scrollPane.setMaximumSize(new Dimension(900, 200));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Add medicine button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBackground(Color.WHITE);

        JButton addButton = new JButton("➕ Add Medicine");
        addButton.setFont(new Font("Arial", Font.BOLD, 12));
        addButton.setBackground(new Color(50, 150, 50));
        addButton.setForeground(Color.WHITE);
        addButton.setFocusPainted(false);
        addButton.setPreferredSize(new Dimension(120, 25));
        addButton.addActionListener(e -> addMedicine());

        JButton removeButton = new JButton("➖ Remove Selected");
        removeButton.setFont(new Font("Arial", Font.BOLD, 12));
        removeButton.setBackground(new Color(200, 50, 50));
        removeButton.setForeground(Color.WHITE);
        removeButton.setFocusPainted(false);
        removeButton.setPreferredSize(new Dimension(140, 25));
        removeButton.addActionListener(e -> removeSelectedMedicine());

        JButton clearButton = new JButton("🗑️ Clear All");
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.setBackground(new Color(150, 150, 150));
        clearButton.setForeground(Color.WHITE);
        clearButton.setFocusPainted(false);
        clearButton.setPreferredSize(new Dimension(100, 25));
        clearButton.addActionListener(e -> clearAllMedicines());

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);

        tablePanel.add(buttonPanel, BorderLayout.SOUTH);

        return tablePanel;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel();
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        footerPanel.setBackground(new Color(250, 250, 250));

        // Create DocumentListener for paid field
        DocumentListener paidDocListener = new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) { updateDue(); }

            @Override
            public void removeUpdate(DocumentEvent e) { updateDue(); }

            @Override
            public void insertUpdate(DocumentEvent e) { updateDue(); }
        };

        // Amount section
        JPanel amountPanel = new JPanel(new GridLayout(4, 2, 15, 10));
        amountPanel.setBackground(new Color(250, 250, 250));

        totalLabel = createAmountLabel("Total: 0.00");
        payableLabel = createAmountLabel("Payable: 0.00");
        paidLabel = new JLabel("Paid Amount:");
        paidLabel.setFont(new Font("Arial", Font.BOLD, 14));

        paidField = new JTextField("0");
        paidField.setFont(new Font("Arial", Font.BOLD, 14));
        paidField.setHorizontalAlignment(JTextField.RIGHT);
        paidField.getDocument().addDocumentListener(paidDocListener);

        dueLabel = createAmountLabel("Due: 0.00");

        amountPanel.add(totalLabel);
        amountPanel.add(new JLabel(""));
        amountPanel.add(payableLabel);
        amountPanel.add(new JLabel(""));
        amountPanel.add(paidLabel);
        amountPanel.add(paidField);
        amountPanel.add(dueLabel);
        amountPanel.add(new JLabel(""));

        // Taka in words
        takaInWordsLabel = new JLabel("Taka in words: Zero Taka Only");
        takaInWordsLabel.setFont(new Font("Arial", Font.ITALIC, 13));
        takaInWordsLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
        takaInWordsLabel.setForeground(new Color(70, 130, 180));

        // Customer Info Display
        customerInfoLabel = new JLabel("Customer: Cash Customer | Phone: 01XXXXXXXXX");
        customerInfoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        customerInfoLabel.setForeground(new Color(70, 130, 180));

        // Received by
        receivedByLabel = new JLabel("Received by: Sarzil");
        receivedByLabel.setFont(new Font("Arial", Font.BOLD, 16));
        receivedByLabel.setForeground(Color.DARK_GRAY);

        // Terms and conditions
        JLabel terms = new JLabel("⚠️ Sold medicines are not returnable");
        terms.setFont(new Font("Arial", Font.ITALIC, 12));
        terms.setForeground(Color.RED);

        // Realistic Barcode Section
        JPanel barcodeContainer = createRealisticBarcode();

        // Thank you message - Using text block
        JLabel thankYou = new JLabel("""
                ❤️ Thank you, we appreciate your visit ❤️
                """, JLabel.CENTER);
        thankYou.setFont(new Font("Arial", Font.BOLD, 16));
        thankYou.setForeground(new Color(70, 130, 180));

        // Created by
        JLabel createdBy = new JLabel("Created by: SARZIL PHARMACY SOFTWARE", JLabel.CENTER);
        createdBy.setFont(new Font("Arial", Font.BOLD, 12));
        createdBy.setForeground(Color.GRAY);

        footerPanel.add(amountPanel);
        footerPanel.add(takaInWordsLabel);
        footerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        footerPanel.add(customerInfoLabel);
        footerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        footerPanel.add(receivedByLabel);
        footerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        footerPanel.add(terms);
        footerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        footerPanel.add(barcodeContainer);
        footerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        footerPanel.add(thankYou);
        footerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        footerPanel.add(createdBy);

        return footerPanel;
    }

    private void updateCustomerInfo() {
        String customerName = customerField.getText().trim();
        String customerPhone = customerNumberField.getText().trim();

        if (customerName.isEmpty()) {
            customerName = "Cash Customer";
        }
        if (customerPhone.isEmpty()) {
            customerPhone = "01XXXXXXXXX";
        }

        customerInfoLabel.setText("Customer: " + customerName + " | Phone: " + customerPhone);
    }

    private void updateReceivedBy() {
        String receivedByName = receivedByField.getText().trim();

        if (receivedByName.isEmpty()) {
            receivedByName = "Sarzil";
        }

        receivedByLabel.setText("Received by: " + receivedByName);
    }

    private JPanel createRealisticBarcode() {
        JPanel mainBarcodePanel = new JPanel();
        mainBarcodePanel.setLayout(new BoxLayout(mainBarcodePanel, BoxLayout.Y_AXIS));
        mainBarcodePanel.setBackground(Color.WHITE);
        mainBarcodePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        String barcodeNumber = "SL" + (System.currentTimeMillis() % 100000);
        JLabel barcodeText = new JLabel(barcodeNumber, JLabel.CENTER);
        barcodeText.setFont(new Font("Courier New", Font.BOLD, 12));
        barcodeText.setForeground(Color.BLACK);
        barcodeText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel barcodePanel = new JPanel();
        barcodePanel.setBackground(Color.WHITE);
        barcodePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 0));
        barcodePanel.setPreferredSize(new Dimension(300, 80));

        int[] barcodePattern = generateBarcodePattern(barcodeNumber);

        for (int i = 0; i < barcodePattern.length; i++) {
            JLabel line = new JLabel();
            line.setBackground(Color.BLACK);
            line.setOpaque(true);

            int width = barcodePattern[i];
            int height = 60 + (int)(Math.random() * 10);

            line.setPreferredSize(new Dimension(width, height));
            barcodePanel.add(line);
        }

        JLabel startZone = new JLabel();
        startZone.setBackground(Color.WHITE);
        startZone.setOpaque(true);
        startZone.setPreferredSize(new Dimension(20, 70));

        JLabel endZone = new JLabel();
        endZone.setBackground(Color.WHITE);
        endZone.setOpaque(true);
        endZone.setPreferredSize(new Dimension(20, 70));

        JPanel fullBarcode = new JPanel(new BorderLayout());
        fullBarcode.setBackground(Color.WHITE);
        fullBarcode.add(startZone, BorderLayout.WEST);
        fullBarcode.add(barcodePanel, BorderLayout.CENTER);
        fullBarcode.add(endZone, BorderLayout.EAST);

        JLabel barcodeType = new JLabel("CODE 128", JLabel.CENTER);
        barcodeType.setFont(new Font("Arial", Font.PLAIN, 10));
        barcodeType.setForeground(Color.GRAY);
        barcodeType.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainBarcodePanel.add(barcodeText);
        mainBarcodePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainBarcodePanel.add(fullBarcode);
        mainBarcodePanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainBarcodePanel.add(barcodeType);

        return mainBarcodePanel;
    }

    private int[] generateBarcodePattern(String number) {
        String binary = "";
        for (char c : number.toCharArray()) {
            if (Character.isDigit(c)) {
                int digit = Character.getNumericValue(c);
                binary += (digit % 2 == 0) ? "111100" : "110011";
            } else {
                binary += "101010";
            }
        }

        int[] pattern = new int[binary.length()];
        for (int i = 0; i < binary.length(); i++) {
            pattern[i] = (binary.charAt(i) == '1') ? 2 : 1;
        }

        return pattern;
    }

    private JLabel createAmountLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(new Color(70, 130, 180));
        return label;
    }

    private void addMedicine() {
        String name = JOptionPane.showInputDialog(this,
                "Enter Medicine Name:",
                "Add Medicine",
                JOptionPane.QUESTION_MESSAGE);

        if (name == null || name.trim().isEmpty()) {
            return;
        }

        String priceStr = JOptionPane.showInputDialog(this,
                "Enter Price per unit (Tk):",
                "Add Medicine - Price",
                JOptionPane.QUESTION_MESSAGE);

        if (priceStr == null || priceStr.trim().isEmpty()) {
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(this,
                "Enter Quantity:",
                "Add Medicine - Quantity",
                JOptionPane.QUESTION_MESSAGE);

        if (qtyStr == null || qtyStr.trim().isEmpty()) {
            return;
        }

        String discPercentStr = JOptionPane.showInputDialog(this,
                "Enter Discount Percentage (%) - Optional:",
                "Add Medicine - Discount %",
                JOptionPane.QUESTION_MESSAGE);

        try {
            double price = Double.parseDouble(priceStr);
            int quantity = Integer.parseInt(qtyStr);
            double discountPercent = (discPercentStr == null || discPercentStr.trim().isEmpty()) ? 0 : Double.parseDouble(discPercentStr);

            if (price < 0 || quantity < 1 || discountPercent < 0 || discountPercent > 100) {
                JOptionPane.showMessageDialog(this,
                        "Please enter valid numbers!\nPrice & Quantity > 0\nDiscount: 0-100%",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            double totalPrice = price * quantity;
            double discountAmount = (totalPrice * discountPercent) / 100.0;
            double netPrice = totalPrice - discountAmount;

            Medicine medicine = new Medicine(name, price, quantity, discountAmount);
            medicines.add(medicine);

            int slNo = medicines.size();

            tableModel.addRow(new Object[]{
                    slNo,
                    name,
                    String.format("%.2f", price),
                    quantity,
                    String.format("%.2f", totalPrice),
                    String.format("%.1f%%", discountPercent),
                    String.format("%.2f", discountAmount),
                    String.format("%.2f", netPrice)
            });

            updateTotals();

            // Using text block for success message
            String successMessage = """
                    Medicine added successfully!
                    Name: %s
                    Total: %.2f Tk
                    Discount: %.1f%% (%.2f Tk)
                    Net: %.2f Tk
                    """.formatted(name, totalPrice, discountPercent, discountAmount, netPrice);

            JOptionPane.showMessageDialog(this,
                    successMessage,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid number format! Please enter valid numbers.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeSelectedMedicine() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < medicines.size()) {
            String medicineName = (String) table.getValueAt(selectedRow, 1);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to remove:\n" + medicineName + "?",
                    "Confirm Removal",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                medicines.remove(selectedRow);
                tableModel.removeRow(selectedRow);
                updateTotals();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(i + 1, i, 0);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Please select a medicine to remove.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearAllMedicines() {
        if (medicines.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No medicines to clear.",
                    "Info",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all medicines?",
                "Confirm Clear All",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            medicines.clear();
            tableModel.setRowCount(0);
            updateTotals();
            JOptionPane.showMessageDialog(this,
                    "All medicines cleared successfully.",
                    "Cleared",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateTotals() {
        double total = 0;
        double totalDiscount = 0;

        for (Medicine medicine : medicines) {
            total += medicine.getTotalPrice();
            totalDiscount += medicine.getDiscount();
        }

        double payable = total - totalDiscount;

        // 🔁 Round payable to nearest full taka
        // Example: 56.78 -> 57.00, 56.38 -> 56.00
        double roundedPayable = Math.round(payable);

        totalLabel.setText(String.format("Total: %.2f", total));
        payableLabel.setText(String.format("Payable: %.2f", roundedPayable));
        updateDue();
    }

    private void updateDue() {
        try {
            double payable = Double.parseDouble(payableLabel.getText().split(": ")[1]);
            double paid = paidField.getText().isEmpty() ? 0 : Double.parseDouble(paidField.getText());
            double due = payable - paid;

            dueLabel.setText(String.format("Due: %.2f", Math.max(0, due)));

            int paidInt = (int) Math.round(paid);
            String words = NumberToWordConverter.convert(paidInt);
            takaInWordsLabel.setText("Taka in words: " + words);

        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            dueLabel.setText("Due: 0.00");
        }
    }

    // Main method for direct testing - Using lambda expression
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new CashMemo().setVisible(true);
        });
    }
}
