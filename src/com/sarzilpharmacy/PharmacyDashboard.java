package com.sarzilpharmacy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PharmacyDashboard extends JFrame {
    private ImageIcon medicineImage;
    
    public PharmacyDashboard() {
        setTitle("Sarzil Pharmacy - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        
        // Load medicine image
        try {
            medicineImage = new ImageIcon("resources/medicine_image.png");
            // Resize image to fit
            Image img = medicineImage.getImage().getScaledInstance(300, 200, Image.SCALE_SMOOTH);
            medicineImage = new ImageIcon(img);
        } catch (Exception e) {
            System.out.println("Medicine image not found, using placeholder");
        }
        
        initializeUI();
    }
    
    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 248, 255)); // Light blue background
        
        // Header
        JLabel header = new JLabel("Sarzil Pharmacy Management System", JLabel.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 24));
        header.setForeground(Color.BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        mainPanel.add(header, BorderLayout.NORTH);
        
        // Center - Medicine Image and Welcome Message
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        
        // Medicine Image
        JLabel imageLabel;
        if (medicineImage != null) {
            imageLabel = new JLabel(medicineImage, JLabel.CENTER);
        } else {
            imageLabel = new JLabel("🏥 Medicine Image Placeholder 🏥", JLabel.CENTER);
            imageLabel.setFont(new Font("Arial", Font.BOLD, 18));
            imageLabel.setForeground(Color.GRAY);
        }
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        
        // Welcome Message
        JTextArea welcomeMessage = new JTextArea();
        welcomeMessage.setText("""
                               Welcome to Sarzil Pharmacy Management System!
                               
                               Features:
                               \u2022 Create Cash Memos
                               \u2022 Manage Medicine Sales
                               \u2022 Automatic Calculation
                               \u2022 Professional Receipts""");
        welcomeMessage.setFont(new Font("Arial", Font.PLAIN, 14));
        welcomeMessage.setEditable(false);
        welcomeMessage.setBackground(new Color(240, 248, 255));
        
        centerPanel.add(imageLabel, BorderLayout.CENTER);
        centerPanel.add(welcomeMessage, BorderLayout.SOUTH);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton cashMemoBtn = new JButton("Create Cash Memo");
        cashMemoBtn.setFont(new Font("Arial", Font.BOLD, 14));
        cashMemoBtn.setBackground(new Color(70, 130, 180));
        cashMemoBtn.setForeground(Color.WHITE);
        cashMemoBtn.setPreferredSize(new Dimension(180, 40));
        cashMemoBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createCashMemo();
            }
        });
        
        JButton exitBtn = new JButton("Exit System");
        exitBtn.setFont(new Font("Arial", Font.BOLD, 14));
        exitBtn.setBackground(new Color(220, 20, 60));
        exitBtn.setForeground(Color.WHITE);
        exitBtn.setPreferredSize(new Dimension(120, 40));
        exitBtn.addActionListener(e -> System.exit(0));
        
        buttonPanel.add(cashMemoBtn);
        buttonPanel.add(exitBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Footer
        JLabel footer = new JLabel("Developed by: SARZIL | Contact: +88 017XX-XXXXXX", JLabel.CENTER);
        footer.setFont(new Font("Arial", Font.ITALIC, 12));
        footer.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        mainPanel.add(footer, BorderLayout.PAGE_END);
        
        add(mainPanel);
    }
    
    private void createCashMemo() {
        CashMemo cashMemo = new CashMemo();
        cashMemo.setVisible(true);
    }
    
    // Main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PharmacyDashboard().setVisible(true);
            }
        });
    }
}
