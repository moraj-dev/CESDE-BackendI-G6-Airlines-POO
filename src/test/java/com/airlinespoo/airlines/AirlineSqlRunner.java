package com.airlinespoo.airlines;

import com.airlinespoo.airlines.repository.AirlineJdbcRepository;
import com.airlinespoo.airlines.repository.IAirlineRepository;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.airlines.ui.AirlineMenu;
import com.airlinespoo.airlines.ui.AirlineSampleData;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.locations.repository.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lanzador interactivo local para probar el Módulo 3 (Aerolíneas) conectado
 * directamente a SQL Server ({@code AerolinesPoo}).
 *
 * <p>Ejecuta este archivo desde IntelliJ (clic derecho -&gt; Run) para registrar,
 * consultar, renombrar, listar, buscar o eliminar aerolíneas directamente en
 * {@code dbo.tblAirlines} y verificar los cambios en SSMS.</p>
 */
public final class AirlineSqlRunner {

    private AirlineSqlRunner() {
        // Lanzador de consola
    }

    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput();

        int nextAirlineId = queryNextAirlineId();
        IAirlineRepository jdbcRepository = new AirlineJdbcRepository();
        AirlineService airlineService = new AirlineService(
                jdbcRepository,
                new IdGenerator(nextAirlineId, 10)
        );

        AirlineSampleData.load(airlineService);

        System.out.println("=================================================");
        System.out.println("  MÓDULO 3 CONECTADO A SQL SERVER (AerolinesPoo) ");
        System.out.println("  Tabla: dbo.tblAirlines                         ");
        System.out.println("=================================================");

        AirlineMenu menu = new AirlineMenu(airlineService, input);
        menu.run();

        System.out.println("\nPrueba en SQL Server finalizada.");
    }

    private static int queryNextAirlineId() {
        String sql = "SELECT ISNULL(MAX(AirlineId), 90) + 10 FROM dbo.tblAirlines";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo conectar o consultar tblAirlines en SQL Server: " + e.getMessage(), e);
        }
    }
}
