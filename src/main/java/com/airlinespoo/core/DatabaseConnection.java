package com.airlinespoo.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=AerolinesPoo;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "aerolinea";
    private static final String PASSWORD = "java&basededatosg6";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ Error al conectar a la base de datos: " + e.getMessage());
            throw new RuntimeException("No se pudo establecer la conexión con la base de datos.", e);
        }
    }
}
