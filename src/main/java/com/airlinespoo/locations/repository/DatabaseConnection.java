package com.airlinespoo.locations.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Entrega conexiones a la base de datos {@code AerolinesPoo} en SQL Server.
 *
 * <p>Los datos de conexión están en un solo lugar: todos los repositorios piden aquí su
 * conexión. Todo el grupo usa la misma base de datos, el mismo motor y el mismo login,
 * así que este archivo es igual para todos y no genera conflictos en los merge.</p>
 */
public final class DatabaseConnection {

    private static final String URL = "jdbc:sqlserver://localhost:1433;"
            + "databaseName=AerolinesPoo;encrypt=true;trustServerCertificate=true";
    private static final String USER = "aerolinea";
    private static final String PASSWORD = "java&basededatosg6";

    private DatabaseConnection() {
        // Clase utilitaria: no se instancia.
    }

    /**
     * Abre una conexión nueva. Quien la pide debe cerrarla (con try-with-resources).
     *
     * @return una conexión abierta a AerolinesPoo
     * @throws SQLException si el servidor no responde o el usuario/contraseña no son válidos
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
