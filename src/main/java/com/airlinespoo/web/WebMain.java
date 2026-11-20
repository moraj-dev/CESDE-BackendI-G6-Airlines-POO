package com.airlinespoo.web;

import com.airlinespoo.airlines.repository.AirlineJdbcRepository;
import com.airlinespoo.airlines.repository.IAirlineRepository;
import com.airlinespoo.airlines.service.AirlineService;
import com.airlinespoo.core.util.IdGenerator;
import com.airlinespoo.customers.repository.CountryRepository;
import com.airlinespoo.customers.repository.CustomerRepository;
import com.airlinespoo.customers.repository.DocumentTypeRepository;
import com.airlinespoo.customers.service.CustomerService;
import com.airlinespoo.flights.repository.FlightRepository;
import com.airlinespoo.flights.service.FlightService;
import com.airlinespoo.locations.repository.AirportRepository;
import com.airlinespoo.locations.repository.CityRepository;
import com.airlinespoo.locations.repository.DatabaseConnection;
import com.airlinespoo.locations.repository.IAirportRepository;
import com.airlinespoo.locations.repository.ICityRepository;
import com.airlinespoo.locations.service.LocationService;
import com.airlinespoo.reservations.repository.IReservationRepository;
import com.airlinespoo.reservations.repository.ReservationJdbcRepository;
import com.airlinespoo.reservations.service.ReservationService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Punto de entrada independiente para la interfaz web y API REST de Airlines-poo.
 *
 * <p>Ensambla exactamente los mismos repositorios JDBC y servicios que {@code app.Main},
 * conectados a SQL Server ({@code AerolinesPoo}), y levanta el servidor web en
 * {@code http://localhost:8080}.</p>
 */
public final class WebMain {

    private static final int DEFAULT_PORT = 8080;

    private WebMain() {
    }

    public static void main(String[] args) throws IOException {
        CustomerService customerService = new CustomerService(
                new CountryRepository(),
                new DocumentTypeRepository(),
                new CustomerRepository()
        );

        ICityRepository cityRepository = new CityRepository();
        IAirportRepository airportRepository = new AirportRepository();
        LocationService locationService = new LocationService(
                cityRepository,
                airportRepository,
                new IdGenerator(1, 1)
        );

        IAirlineRepository airlineRepository = new AirlineJdbcRepository();
        AirlineService airlineService = new AirlineService(
                airlineRepository,
                new IdGenerator(100, 10)
        );
        airlineService.syncIdGenerator();

        FlightRepository flightRepository = new FlightRepository(airportRepository, airlineRepository);
        FlightService flightService = new FlightService(
                flightRepository,
                locationService,
                airlineService
        );

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

        AirlinesWebServer server = new AirlinesWebServer(
                DEFAULT_PORT,
                customerService,
                locationService,
                airlineService,
                flightService,
                reservationService
        );
        server.start();

        System.out.println("=================================================");
        System.out.println("  Airlines-poo Web Server activo                 ");
        System.out.println("  URL: http://localhost:" + DEFAULT_PORT + "                  ");
        System.out.println("  API: http://localhost:" + DEFAULT_PORT + "/api/             ");
        System.out.println("=================================================");
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
