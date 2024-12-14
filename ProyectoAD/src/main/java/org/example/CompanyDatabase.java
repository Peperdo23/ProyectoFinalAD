package org.example;

import java.sql.*;
import java.io.*;
import java.util.*;

public class CompanyDatabase {

    public static void createTables() {
        String dbUrl = "jdbc:sqlite:company_database.db";

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            if (conn != null) {
                System.out.println("Conexión establecida.");

                //Descomentar para eliminar las tablas
                //DeleteTables(conn);

                String path = "csvFiles/";

                createTableFromCSV(conn, path + "employee_projects.csv", "employee_projects");
                createTableFromCSV(conn, path + "employees_realistic.csv", "employees_realistic");
                createTableFromCSV(conn, path + "customers.csv", "customers");
                createTableFromCSV(conn, path + "departments.csv", "departments");
                createTableFromCSV(conn, path + "order_items.csv", "order_items");
                createTableFromCSV(conn, path + "orders.csv", "orders");
                createTableFromCSV(conn, path + "projects.csv", "projects");
                createProjectSummaryTable(conn);
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
    }

    public static void createTableFromCSV(Connection conn, String csvFile, String tableName) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(csvFile));
            String line;
            String[] headers = br.readLine().split(",");
            String createTableQuery = "CREATE TABLE IF NOT EXISTS " + tableName + " (";
            for (String header : headers) {
                createTableQuery += header + " TEXT,";
            }
            createTableQuery = createTableQuery.substring(0, createTableQuery.length() - 1) + ")";
            Statement stmt = conn.createStatement();
            stmt.execute(createTableQuery);

            String checkDataQuery = "SELECT COUNT(*) FROM " + tableName;
            ResultSet rs = stmt.executeQuery(checkDataQuery);
            rs.next();
            int rowCount = rs.getInt(1);

            if (rowCount == 0) {
                System.out.println("Insertando datos en la tabla: " + tableName);
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    String insertQuery = "INSERT INTO " + tableName + " VALUES (";
                    for (String value : values) {
                        insertQuery += "'" + value.replace("'", "''") + "',";
                    }
                    insertQuery = insertQuery.substring(0, insertQuery.length() - 1) + ")";
                    stmt.executeUpdate(insertQuery);
                }
            } else {
                System.out.println("La tabla " + tableName + " ya existe");
            }

            br.close();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createProjectSummaryTable(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS project_summary (" +
                    "project_id INTEGER, " +
                    "project_salary_costs REAL, " +
                    "budget REAL, " +
                    "cost_fraction REAL)";
            stmt.execute(createTableQuery);

            String clearTableQuery = "DELETE FROM project_summary";
            stmt.execute(clearTableQuery);

            String insertQuery = "INSERT INTO project_summary (project_id, project_salary_costs, budget, cost_fraction) " +
                    "SELECT ep.project_id, " +
                    "ROUND(SUM(er.salary * ep.hours_worked / 1900), 1) AS project_salary_costs, " +
                    "p.budget, " +
                    "ROUND((SUM(er.salary * ep.hours_worked / 1900) * 100.0 / p.budget), 1) AS cost_fraction " +
                    "FROM employee_projects ep " +
                    "JOIN employees_realistic er ON ep.employee_id = er.employee_id " +
                    "JOIN projects p ON ep.project_id = p.project_id " +
                    "GROUP BY ep.project_id, p.budget " +
                    "ORDER BY ep.project_id";

            stmt.executeUpdate(insertQuery);

            System.out.println("Tabla 'project_summary' actualizada correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al crear o insertar en la tabla 'project_summary': " + e.getMessage());
        }
    }

    public static void DeleteTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            if (conn != null) {

                ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");

                List<String> tableNames = new ArrayList<>();
                while (rs.next()) {
                    String tableName = rs.getString("name");
                    if (!tableName.equals("sqlite_master")) {
                        tableNames.add(tableName);
                    }
                }
                rs.close();

                for (String tableName : tableNames) {
                    String dropTableQuery = "DROP TABLE IF EXISTS " + tableName;
                    stmt.executeUpdate(dropTableQuery);
                    System.out.println("Tabla eliminada: " + tableName);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error de conexión o ejecución: " + e.getMessage());
        }
    }
}