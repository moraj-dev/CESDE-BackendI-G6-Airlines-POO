package com.airlinespoo.app;

import com.airlinespoo.airlines.repository.AirlineJdbcRepository;
import com.airlinespoo.airlines.repository.IAirlineRepository;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.airlines.ui.AirlineMenu;
import com.airlinespoo.core.ui.ConsoleInput;
import com.airlinespoo.core.ui.Menu;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.repository.CustomerRepository;
import com.airlinespoo.customers.repository.DocumentTypeRepository;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.customers.ui.CustomerMenu;
import com.airlinespoo.flights.repository.FlightRepository;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.flights.ui.FlightMenu;
import com.airlinespoo.locations.repository.AirportRepository;
import com.airlinespoo.locations.repository.CityRepository;
import com.airlinespoo.locations.repository.DatabaseConnection;
import com.airlinespoo.locations.repository.IAirportRepository;
import com.airlinespoo.locations.repository.ICityRepository;
import com.airlinespoo.locations.service.LocationService;
import com.airlinespoo.locations.ui.LocationMenu;
import com.airlinespoo.reservations.repository.IReservationRepository;
import com.airlinespoo.reservations.repository.ReservationJdbcRepository;
import com.airlinespoo.reservations.service.ReservationService;
import com.airlinespoo.reservations.ui.ReservationMenu;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Punto de entrada de Airlines-poo.
 *
 * <p>Ensambla los cinco módulos del sistema conectados a SQL Server ({@code AerolinesPoo})
 * y arranca el menú principal unificado.</p>
 */
public final class Main {

    private Main() {
        // Clase de arranque: no se instancia.
    }

    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de la línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        System.out.println("=== Airlines-poo · Sistema de Reservas Aéreas ===");

        ConsoleInput console = new ConsoleInput();

        // Módulo 1: Clientes y catálogos
        CustomerService customerService = new CustomerService(
                new CountryRepository(),
                new DocumentTypeRepository(),
                new CustomerRepository()
        );

        // Módulo 2: Ubicaciones (Ciudades y Aeropuertos)
        ICityRepository cityRepository = new CityRepository();
        IAirportRepository airportRepository = new AirportRepository();
        LocationService locationService = new LocationService(
                cityRepository,
                airportRepository,
                new IdGenerator(1, 1)
        );

        // Módulo 3: Aerolíneas
        IAirlineRepository airlineRepository = new AirlineJdbcRepository();
        AirlineService airlineService = new AirlineService(
                airlineRepository,
                new IdGenerator(100, 10)
        );
        airlineService.syncIdGenerator();

        // Módulo 4: Vuelos
        FlightRepository flightRepository = new FlightRepository(airportRepository, airlineRepository);
        FlightService flightService = new FlightService(
                flightRepository,
                locationService,
                airlineService
        );

        // Módulo 5: Reservas y Pasajeros
        int nextReservationId = queryNextId("SELECT ISNULL(MAX(ReservationId), 0) + 1 FROM dbo.tblReservations");
        int nextDetailId = queryNextId("SELECT ISNULL(MAX(DetailId), 0) + 1 FROM dbo.tblDetailsReservation");
        IReservationRepository reservationRepository = new ReservationJdbcRepository(customerService, flightService);
        ReservationService reservationService = new ReservationService(
                reservationRepository,
                customerService,
                flightService,
                new IdGenerator(nextReservationId, 1),
                new IdGenerator(nextDetailId, 1)
        );

        // Menús de los 5 módulos
        List<Menu> modules = List.of(
                new CustomerMenu(customerService, console),
                new LocationMenu(locationService, console),
                new AirlineMenu(airlineService, console),
                new FlightMenu(flightService, locationService, airlineService, console),
                new ReservationMenu(reservationService, customerService, console)
        );

        new MainMenu(modules, console).run();
        System.out.println("Sistema cerrado.");
    }

    private static int queryNextId(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            return 1;
        }
    }
}
