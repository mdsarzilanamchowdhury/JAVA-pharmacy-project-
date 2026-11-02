package com.sarzilsazibpharmacy;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class CustomerManagement extends JFrame {
    private static final Path DIR = Paths.get("data");
    private static final Path FILE = DIR.resolve("customers.txt");
    private static final Path BIN = DIR.resolve("customers_recycle_bin.txt");
    private static final Path COUNTER = DIR.resolve("customer_id_counter.txt");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"CUSTOMER PHONE", "CUSTOMER ID", "CUSTOMER NAME", "CUSTOMER ADDRESS"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private JScrollPane tableScroll;

    public CustomerManagement() {
        setTitle("Sarzil-Sazib-Pharmacy - Customer Management");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        buildUI();
        refresh();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(new Color(255, 250, 240));
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel("CUSTOMER MANAGEMENT", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(170, 75, 0));
        root.add(title, BorderLayout.NORTH);

        // Main table formatting
        table.setFont(new Font("Arial", Font.PLAIN, 15));
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(255, 220, 120));
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(225, 105, 20));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        // Keep complete column headings visible; horizontal scrolling is enabled if needed.
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        int[] columnWidths = {160, 130, 190, 320};
        for (int i = 0; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
            table.getColumnModel().getColumn(i).setMinWidth(columnWidths[i]);
        }

        tableScroll = new JScrollPane(
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(210, 115, 25), 2));

        // Table and right-side action sidebar
        JPanel mainPanel = new JPanel(new BorderLayout(12, 0));
        mainPanel.setOpaque(false);
        mainPanel.add(tableScroll, BorderLayout.CENTER);

        JPanel sidebar = new JPanel(new BorderLayout(0, 12));
        sidebar.setPreferredSize(new Dimension(195, 0));
        sidebar.setBackground(new Color(255, 242, 220));
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 115, 25), 2),
                BorderFactory.createEmptyBorder(12, 10, 12, 10)
        ));

        JLabel sidebarTitle = new JLabel("CUSTOMER ACTIONS", SwingConstants.CENTER);
        sidebarTitle.setFont(new Font("Arial", Font.BOLD, 15));
        sidebarTitle.setForeground(new Color(170, 75, 0));
        sidebar.add(sidebarTitle, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(7, 1, 0, 10));
        buttons.setOpaque(false);
        String[] labels = {
                "ADD CUSTOMER", "UPDATE CUSTOMER", "DELETE CUSTOMER",
                "RECYCLE BIN", "SEARCH", "REFRESH", "BACK"
        };

        for (String label : labels) {
            JButton b = new GradientButton(label);
            b.setFont(new Font("Arial", Font.BOLD, 14));
            b.setForeground(new Color(70, 45, 0));
            b.setFocusPainted(false);
            b.setPreferredSize(new Dimension(170, 50));
            b.addActionListener(e -> {
                switch (label) {
                    case "ADD CUSTOMER": addCustomer(); break;
                    case "UPDATE CUSTOMER": updateCustomer(); break;
                    case "DELETE CUSTOMER": deleteCustomer(); break;
                    case "RECYCLE BIN": recycleBin(); break;
                    case "SEARCH": searchCustomer(); break;
                    case "REFRESH": refresh(); break;
                    case "BACK": dispose(); break;
                }
            });
            buttons.add(b);
        }
        sidebar.add(buttons, BorderLayout.CENTER);
        mainPanel.add(sidebar, BorderLayout.EAST);
        root.add(mainPanel, BorderLayout.CENTER);
        setContentPane(root);
    }
    private static class GradientButton extends JButton {
        GradientButton(String text) { super(text); setContentAreaFilled(false); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, new Color(225, 85, 0), getWidth(), getHeight(), new Color(255, 235, 130)));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void ensureFiles() throws IOException {
        Files.createDirectories(DIR);
        if (!Files.exists(FILE)) Files.createFile(FILE);
        if (!Files.exists(BIN)) Files.createFile(BIN);
        if (!Files.exists(COUNTER)) Files.writeString(COUNTER, "0", StandardCharsets.UTF_8);
    }

    private List<String[]> read(Path path) throws IOException {
        ensureFiles();
        List<String[]> rows = new ArrayList<>();
        int n = 0;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            n++;
            if (line.trim().isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 4) throw new IOException("Invalid customer record at line " + n + " in " + path);
            rows.add(f);
        }
        return rows;
    }

    private void write(Path path, List<String[]> rows) throws IOException {
        List<String> out = new ArrayList<>();
        for (String[] r : rows) out.add(join(r));
        Files.write(path, out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static String join(String[] f) {
        for (int i = 0; i < f.length; i++) f[i] = safe(f[i]);
        return String.join("|", f);
    }
    private static String safe(String s) {
        return s == null ? "" : s.trim().replace('|', '/').replace('\n', ' ').replace('\r', ' ');
    }
    private static String digits(String phone) {
        if (phone == null) return "";
        String p = phone.trim().replaceAll("\\s+", "");
        if (p.startsWith("+88")) p = p.substring(3);
        return p;
    }
    private static boolean validPhone(String p) { return digits(p).matches("01[3-9]\\d{8}"); }
    private String nextId() throws IOException {
        ensureFiles();
        int last = Integer.parseInt(Files.readString(COUNTER, StandardCharsets.UTF_8).trim());
        return String.format(Locale.ROOT, "CUS-%03d", last + 1);
    }
    private String allocateId() throws IOException {
        String id = nextId();
        int n = Integer.parseInt(Files.readString(COUNTER, StandardCharsets.UTF_8).trim()) + 1;
        Files.writeString(COUNTER, String.valueOf(n), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        return id;
    }
    private void refresh() {
        try {
            model.setRowCount(0);
            for (String[] r : read(FILE)) model.addRow(new Object[]{"+88 " + r[0], r[1], r[2], r[3]});
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private JPanel customerForm(JTextField phone, JTextField id, JTextField name, JTextField address, boolean idEditable) {
        JPanel p = new JPanel(new GridLayout(4, 2, 10, 10));
        p.add(new JLabel("Customer Phone (+88 fixed):")); p.add(phone);
        p.add(new JLabel("Customer ID:")); p.add(id);
        p.add(new JLabel("Customer Name:")); p.add(name);
        p.add(new JLabel("Customer Address:")); p.add(address);
        id.setEditable(idEditable);
        phone.setToolTipText("Enter 11-digit Bangladeshi mobile number, e.g. 01712345678");
        return p;
    }
    private void addCustomer() {
        JTextField phone = new JTextField( " +88 ", 16), id = new JTextField(12);
        JTextField name = new JTextField(18), address = new JTextField(18);
        phone.setText("+88");
        try { id.setText(nextId()); } catch (IOException ex) { error(ex.getMessage()); return; }
        int ans = JOptionPane.showConfirmDialog(this, customerForm(phone,id,name,address,false),
                "ADD CUSTOMER", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ans != JOptionPane.OK_OPTION) return;
        String ph = digits(phone.getText());
        if (!validPhone(ph)) { error("Enter exactly 11 valid digits after +88."); return; }
        if (name.getText().trim().isEmpty() || address.getText().trim().isEmpty()) { error("Customer Name and Address are required."); return; }
        try {
            List<String[]> rows = read(FILE);
            if (find(rows, ph) != null) { error("THIS CUSTOMER PHONE NUMBER ALREADY EXIST"); return; }
            String newId = allocateId();
            rows.add(new String[]{ph, newId, safe(name.getText()), safe(address.getText())});
            write(FILE, rows);
            refresh();
            info("Customer added successfully. Customer ID: " + newId);
        } catch (Exception ex) { error(ex.getMessage()); }
    }
    private void updateCustomer() {
        try {
            List<String[]> rows = read(FILE);

            if (rows.isEmpty()) {
                info("NO CUSTOMER AVAILABLE.");
                return;
            }

            // Show all Customer IDs in a compact dropdown.
            String[] customerIDs = new String[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                customerIDs[i] = rows.get(i)[1];
            }

            JComboBox<String> customerIDBox = new JComboBox<>(customerIDs);
            customerIDBox.setFont(new Font("Arial", Font.BOLD, 14));
            customerIDBox.setPreferredSize(new Dimension(180, 28));

            JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
            selectPanel.add(new JLabel("Select Customer ID:"));
            selectPanel.add(customerIDBox);

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    selectPanel,
                    "UPDATE CUSTOMER - SELECT CUSTOMER",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            String selectedID = (String) customerIDBox.getSelectedItem();
            if (selectedID == null) {
                error("CUSTOMER NOT FOUND");
                return;
            }

            // Find the selected customer by Customer ID.
            String[] selectedCustomer = null;
            for (String[] customer : rows) {
                if (customer[1].equals(selectedID)) {
                    selectedCustomer = customer;
                    break;
                }
            }

            if (selectedCustomer == null) {
                error("CUSTOMER NOT FOUND");
                return;
            }

            // Only these three fields can be edited. Customer ID stays unchanged.
            JTextField phone = new JTextField("+88" + selectedCustomer[0], 18);
            JTextField name = new JTextField(selectedCustomer[2], 18);
            JTextField address = new JTextField(selectedCustomer[3], 18);

            JPanel form = new JPanel(new GridLayout(3, 2, 10, 12));
            form.add(new JLabel("Customer Phone (+88 fixed):"));
            form.add(phone);
            form.add(new JLabel("Customer Name:"));
            form.add(name);
            form.add(new JLabel("Customer Address:"));
            form.add(address);

            int ans = JOptionPane.showConfirmDialog(
                    this,
                    form,
                    "UPDATE CUSTOMER INFORMATION",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (ans != JOptionPane.OK_OPTION) {
                return;
            }

            String newPhone = digits(phone.getText());
            if (!validPhone(newPhone)) {
                error("Enter exactly 11 valid digits after +88.");
                return;
            }

            String[] duplicate = find(rows, newPhone);
            if (duplicate != null && duplicate != selectedCustomer) {
                error("THIS CUSTOMER PHONE NUMBER ALREADY EXIST");
                return;
            }

            if (name.getText().trim().isEmpty()
                    || address.getText().trim().isEmpty()) {
                error("Customer Name and Address are required.");
                return;
            }

            selectedCustomer[0] = newPhone;
            selectedCustomer[2] = safe(name.getText());
            selectedCustomer[3] = safe(address.getText());

            write(FILE, rows);
            refresh();
            info("Customer updated successfully.");

        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void deleteCustomer() {
        int selectedViewRow = table.getSelectedRow();
        if (selectedViewRow < 0) {
            error("ROW NOT SELECTED");
            return;
        }

        int selectedModelRow = table.convertRowIndexToModel(selectedViewRow);
        String displayedPhone = String.valueOf(model.getValueAt(selectedModelRow, 0));
        String ph = digits(displayedPhone);

        try {
            List<String[]> rows = read(FILE);
            String[] selectedCustomer = find(rows, ph);
            if (selectedCustomer == null) {
                error("Selected customer was not found. Please refresh the table.");
                refresh();
                return;
            }

            String message = "Customer Phone: +88 " + selectedCustomer[0]
                    + "\nCustomer ID: " + selectedCustomer[1]
                    + "\nCustomer Name: " + selectedCustomer[2]
                    + "\nCustomer Address: " + selectedCustomer[3]
                    + "\n\nARE YOU SURE YOU WANT TO DELETE THIS CUSTOMER?";
            if (JOptionPane.showConfirmDialog(this, message, "DELETE CUSTOMER",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }

            rows.remove(selectedCustomer);
            write(FILE, rows);
            List<String[]> bin = read(BIN);
            bin.add(selectedCustomer);
            write(BIN, bin);
            refresh();
            info("Customer moved to Recycle Bin.");
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }
    private void recycleBin() {
        try {
            List<String[]> bin=read(BIN);
            DefaultTableModel m=new DefaultTableModel(new String[]{"CUSTOMER PHONE","CUSTOMER ID","CUSTOMER NAME","CUSTOMER ADDRESS"},0){
                @Override public boolean isCellEditable(int r,int c){return false;}
            };
            for(String[] r:bin)m.addRow(new Object[]{"+88 "+r[0],r[1],r[2],r[3]});
            JTable t=new JTable(m); t.setRowHeight(28);
            JPanel panel=new JPanel(new BorderLayout(5,5)); panel.add(new JScrollPane(t),BorderLayout.CENTER);
            JPanel controls=new JPanel(); JButton restore=new JButton("RESTORE"), permanent=new JButton("PERMANENT DELETE");
            controls.add(restore); controls.add(permanent); panel.add(controls,BorderLayout.SOUTH);
            JDialog d=new JDialog(this,"CUSTOMER RECYCLE BIN",true); d.setContentPane(panel); d.setSize(850,420); d.setLocationRelativeTo(this);
            restore.addActionListener(e->{
                int row=t.getSelectedRow(); if(row<0){error("Select a customer.");return;}
                String[] selected=bin.get(row);
                try {
                    List<String[]> active=read(FILE);
                    if(find(active,selected[0])!=null){error("This phone number already exists in active customers.");return;}
                    active.add(selected); bin.remove(row); write(FILE,active); write(BIN,bin); d.dispose(); refresh();
                }catch(Exception ex){error(ex.getMessage());}
            });
            permanent.addActionListener(e->{
                int row=t.getSelectedRow(); if(row<0){error("Select a customer.");return;}
                if(JOptionPane.showConfirmDialog(d,"PERMANENTLY DELETE THIS CUSTOMER?","CONFIRM",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                bin.remove(row);
                try{write(BIN,bin);d.dispose();recycleBin();}catch(Exception ex){error(ex.getMessage());}
            });
            d.setVisible(true);
        }catch(Exception ex){error(ex.getMessage());}
    }
    private void searchCustomer() {
        String ph = askPhone();
        if (ph == null) return;

        try {
            // Reload the table first so the row index always matches the latest file data.
            refresh();

            for (int row = 0; row < model.getRowCount(); row++) {
                String tablePhone = digits(String.valueOf(model.getValueAt(row, 0)));
                if (tablePhone.equals(ph)) {
                    int viewRow = table.convertRowIndexToView(row);
                    table.clearSelection();
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                    table.requestFocusInWindow();
                    return; // Only highlight the matching row; do not open a details window.
                }
            }
            info("Customer not found.");
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }
    private String askPhone() {
        String s=JOptionPane.showInputDialog(this,"Enter 11-digit Customer Phone (without +88):","SEARCH CUSTOMER",JOptionPane.QUESTION_MESSAGE);
        if(s==null)return null;
        if(!validPhone(s)){error("Enter exactly 11 valid digits.");return null;}
        return digits(s);
    }
    private static String[] find(List<String[]> rows,String phone) {
        for(String[] r:rows)if(r[0].equals(digits(phone)))return r;
        return null;
    }
    private void error(String s){JOptionPane.showMessageDialog(this,s,"Customer Management",JOptionPane.ERROR_MESSAGE);}
    private void info(String s){JOptionPane.showMessageDialog(this,s,"Customer Management",JOptionPane.INFORMATION_MESSAGE);}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new CustomerManagement().setVisible(true));}
}