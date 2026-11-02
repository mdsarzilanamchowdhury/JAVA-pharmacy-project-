package com.sarzilsazibpharmacy;

import javax.swing.*;
import java.awt.*;

/**
 * Main launcher for Sarzil-Sazib-Pharmacy.
 * Opens Stock Management, Sales Management, or Total Sales Report.
 */
public class Main extends JFrame {

    private static final Color BACKGROUND = new Color(255, 248, 242);
    private static final Color MAROON = new Color(105, 18, 42);

    public Main() {
        setTitle("Sarzil-Sazib-Pharmacy - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(620, 470);
        setMinimumSize(new Dimension(520, 400));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBorder(BorderFactory.createEmptyBorder(28, 38, 28, 38));
        root.setBackground(BACKGROUND);

        JLabel heading = new JLabel("SARZIL SAZIB PHARMACY", SwingConstants.CENTER);
        heading.setFont(new Font("Arial", Font.BOLD, 27));
        heading.setForeground(MAROON);
        root.add(heading, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(3, 1, 12, 12));
        buttons.setOpaque(false);

        JButton stockButton = createButton("STOCK MANAGEMENT");
        JButton salesButton = createButton("SALES MANAGEMENT");
        JButton reportButton = createButton("TOTAL SALES REPORT");

        stockButton.addActionListener(e -> openStockManagement());
        salesButton.addActionListener(e -> openSalesManagement());
        reportButton.addActionListener(e -> openTotalSalesReport());

        buttons.add(stockButton);
        buttons.add(salesButton);
        buttons.add(reportButton);
        root.add(buttons, BorderLayout.CENTER);

        JLabel footer = new JLabel("Select a module to continue", SwingConstants.CENTER);
        footer.setFont(new Font("Arial", Font.ITALIC, 13));
        footer.setForeground(new Color(90, 75, 75));
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 17));
        button.setForeground(Color.WHITE);
        button.setBackground(MAROON);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 10, 28), 1),
                BorderFactory.createEmptyBorder(13, 16, 13, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void openStockManagement() {
        try {
            new StockManagement().setVisible(true);
        } catch (Exception ex) {
            showLaunchError("Stock Management", ex);
        }
    }

    private void openSalesManagement() {
        try {
            // SalesManagement.setVisible(true) opens its Add Sale window.
            new SalesManagement().setVisible(true);
        } catch (Exception ex) {
            showLaunchError("Sales Management", ex);
        }
    }

    private void openTotalSalesReport() {
        try {
            new TotalSalesReport().setVisible(true);
        } catch (Exception ex) {
            showLaunchError("Total Sales Report", ex);
        }
    }

    private void showLaunchError(String module, Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                "Could not open " + module + ".\n" + ex.getMessage(),
                "Launch Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}