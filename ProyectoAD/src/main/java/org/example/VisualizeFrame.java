package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;

public class VisualizeFrame extends JFrame {

    private Connection conn;

    public VisualizeFrame() {
        setTitle("Visualizador de la base de datos");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        String dbUrl = "jdbc:sqlite:company_database.db";
        try {
            conn = DriverManager.getConnection(dbUrl);
            System.out.println("Conexión entre el visualizador y la base de datos establecida");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error de conexión: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        String[] options = getTableNames(conn);
        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setSelectedIndex(options.length - 1);
        topPanel.add(comboBox);

        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());

        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        centerPanel.add(Box.createVerticalStrut(30), BorderLayout.NORTH);


        loadTableData(table, options[options.length - 1]);
        comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadTableData(table, comboBox.getSelectedItem().toString());
            }
        } );

        setVisible(true);
    }

    public static String[] getTableNames(Connection conn) {
        try {
            String query = "SELECT name FROM sqlite_master WHERE type = 'table';";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            ArrayList<String> tableNames = new ArrayList<>();
            while (rs.next()) {
                tableNames.add(rs.getString("name"));
            }

            return tableNames.toArray(new String[0]);
        } catch (SQLException e) {
            System.err.println("Error al obtener los nombres de las tablas: " + e.getMessage());
            return new String[0];
        }
    }

    private void loadTableData(JTable table, String tableName) {
        if (tableName.equals("project_summary")) tableName = tableName + " ORDER BY project_id";
        try {
            String query = "SELECT * FROM " + tableName;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            DefaultTableModel model = new DefaultTableModel();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                model.addColumn(metaData.getColumnName(i));
            }

            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                model.addRow(row);
            }
            table.setModel(model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar los datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CompanyDatabase.createTables();
            new VisualizeFrame();
        });
    }
}
