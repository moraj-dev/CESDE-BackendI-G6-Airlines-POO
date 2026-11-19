package com.airlinespoo.reservations;

import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.customers.CustomerSampleData;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.repository.CustomerRepository;
import com.airlinespoo.customers.repository.DocumentTypeRepository;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.locations.repository.DatabaseConnection;
import com.airlinespoo.reservations.repository.IReservationRepository;
import com.airlinespoo.reservations.repository.ReservationJdbcRepository;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.ui.ReservationMenu;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lanzador interactivo local para probar el Módulo 5 (Reservas) conectado
 * directamente a SQL Server ({@code AerolinesPoo}).
 *
 * <p>Ejecuta este archivo desde IntelliJ (clic derecho -&gt; Run) para crear,
 * modificar o cancelar reservas desde la consola y luego verificarlas en SSMS.</p>
 */
public final class ReservationSqlRunner {

    private ReservationSqlRunner() {
        // Lanzador de consola
    }

    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput();

        // 1. Inicializar Módulo 1 en memoria y asegurar los registros base en SQL Server (FK)
        CustomerService customerService = new CustomerService(
                new CountryRepository(),
                new DocumentTypeRepository(),
                new CustomerRepository()
        );
        CustomerSampleData.load(customerService);
        ensureSeedDataInSqlServer();

        // 2. Calcular siguientes IDs disponibles en SQL Server para evitar duplicados al reiniciar
        int nextReservationId = queryNextId("SELECT ISNULL(MAX(ReservationId), 0) + 1 FROM dbo.tblReservations");
        int nextDetailId = queryNextId("SELECT ISNULL(MAX(DetailId), 0) + 1 FROM dbo.tblDetailsReservation");

        // 3. Inicializar Módulo 5 conectado a SQL Server (JDBC)
        IReservationRepository jdbcRepository = new ReservationJdbcRepository(customerService);
        ReservationService reservationService = new ReservationService(
                jdbcRepository,
                customerService,
                new IdGenerator(nextReservationId, 1),
                new IdGenerator(nextDetailId, 1)
        );

        System.out.println("=================================================");
        System.out.println("  MÓDULO 5 CONECTADO A SQL SERVER (AerolinesPoo) ");
        System.out.println("  Clientes de prueba: 101010, 202020, 303030     ");
        System.out.println("  Vuelos de prueba:   101, 202, 303              ");
        System.out.println("=================================================");

        // 4. Ejecutar el menú interactivo
        ReservationMenu menu = new ReservationMenu(reservationService, customerService, input);
        menu.run();

        System.out.println("\nPrueba en SQL Server finalizada.");
    }

    private static void ensureSeedDataInSqlServer() {
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("""
                    IF NOT EXISTS (SELECT 1 FROM dbo.tblCustomers WHERE customerId = '101010')
                        INSERT INTO dbo.tblCustomers (customerId, FirtName, Lastname, TelephoneNumber, Email)
                        VALUES ('101010', 'Ana', 'Pérez', '3001112233', 'ana.perez@mail.com');
                    IF NOT EXISTS (SELECT 1 FROM dbo.tblCustomers WHERE customerId = '202020')
                        INSERT INTO dbo.tblCustomers (customerId, FirtName, Lastname, TelephoneNumber, Email)
                        VALUES ('202020', 'Luis', 'Gómez', '3002223344', 'luis.gomez@mail.com');
                    IF NOT EXISTS (SELECT 1 FROM dbo.tblCustomers WHERE customerId = '303030')
                        INSERT INTO dbo.tblCustomers (customerId, FirtName, Lastname, TelephoneNumber, Email)
                        VALUES ('303030', 'María', 'López', '3003334455', 'maria.lopez@mail.com');
                    """);

            st.executeUpdate("""
                    SET IDENTITY_INSERT dbo.tblFlights ON;
                    IF NOT EXISTS (SELECT 1 FROM dbo.tblFlights WHERE FlightId = 101)
                        INSERT INTO dbo.tblFlights (FlightId, FlightName, DateDeparture, HourDeparture, DateArrival, HourArrival)
                        VALUES (101, 'LA-101', '2026-12-01', '08:00:00', '2026-12-01', '09:30:00');
                    IF NOT EXISTS (SELECT 1 FROM dbo.tblFlights WHERE FlightId = 202)
                        INSERT INTO dbo.tblFlights (FlightId, FlightName, DateDeparture, HourDeparture, DateArrival, HourArrival)
                        VALUES (202, 'AV-202', '2026-12-02', '14:00:00', '2026-12-02', '15:30:00');
                    IF NOT EXISTS (SELECT 1 FROM dbo.tblFlights WHERE FlightId = 303)
                        INSERT INTO dbo.tblFlights (FlightId, FlightName, DateDeparture, HourDeparture, DateArrival, HourArrival)
                        VALUES (303, 'VV-303', '2026-12-03', '19:00:00', '2026-12-03', '20:30:00');
                    SET IDENTITY_INSERT dbo.tblFlights OFF;
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron verificar los datos base en SQL Server: " + e.getMessage(), e);
        }
    }

    private static int queryNextId(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo consultar el siguiente ID en SQL Server: " + e.getMessage(), e);
        }
    }
}
