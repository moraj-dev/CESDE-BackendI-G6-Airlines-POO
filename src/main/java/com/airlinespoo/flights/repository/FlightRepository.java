package com.airlinespoo.flights.repository;

import com.airlinespoo.airlines.model.Airline;
import com.airlinespoo.airlines.repository.AirlineJdbcRepository;
import com.airlinespoo.airlines.repository.IAirlineRepository;
import com.airlinespoo.core.exception.AirlinesException;
import com.airlinespoo.core.exception.DuplicateException;
import com.airlinespoo.core.util.Validator;
import com.airlinespoo.flights.model.Flight;
import com.airlinespoo.locations.model.Airport;
import com.airlinespoo.locations.repository.AirportRepository;
import com.airlinespoo.locations.repository.DatabaseConnection;
import com.airlinespoo.locations.repository.IAirportRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JDBC de vuelos en SQL Server (tabla {@code dbo.tblFlights}).
 */
public class FlightRepository {

    private static final int SQL_UNIQUE_VIOLATION_1 = 2627;
    private static final int SQL_UNIQUE_VIOLATION_2 = 2601;

    private static final String BASE_SELECT = """
            SELECT
                f.FlightId,
                f.FlightName,
                f.Departure,
                f.Arrival,
                f.DateDeparture,
                f.HourDeparture,
                f.DateArrival,
                f.HourArrival,
                f.AirlineId,
                a.name AS AirlineName
            FROM dbo.tblFlights f
            LEFT JOIN dbo.tblAirlines a ON f.AirlineId = a.airlineId
            """;

    private static final String INSERT_FLIGHT_SQL = """
            INSERT INTO dbo.tblFlights (
                FlightName,
                Departure,
                Arrival,
                DateDeparture,
                HourDeparture,
                DateArrival,
                HourArrival,
                AirlineId
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final IAirportRepository airportRepository;
    private final IAirlineRepository airlineRepository;

    public FlightRepository() {
        this(new AirportRepository(), new AirlineJdbcRepository());
    }

    public FlightRepository(IAirportRepository airportRepository, IAirlineRepository airlineRepository) {
        this.airportRepository = Validator.requireNotNull(airportRepository, "repositorio de aeropuertos");
        this.airlineRepository = Validator.requireNotNull(airlineRepository, "repositorio de aerolíneas");
    }

    public Flight save(Flight flight) {
        Validator.requireNotNull(flight, "vuelo");

        UUID departureUuid = flight.getDeparture() != null ? flight.getDeparture().getId() : null;
        UUID arrivalUuid = flight.getArrival() != null ? flight.getArrival().getId() : null;
        Integer airlineId = flight.getAirlineId();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_FLIGHT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, flight.getFlightName());
            setUuid(ps, 2, departureUuid);
            setUuid(ps, 3, arrivalUuid);
            ps.setDate(4, flight.getDateDeparture() != null ? Date.valueOf(flight.getDateDeparture()) : null);
            ps.setTime(5, flight.getHourDeparture() != null ? Time.valueOf(flight.getHourDeparture()) : null);
            ps.setDate(6, flight.getDateArrival() != null ? Date.valueOf(flight.getDateArrival()) : null);
            ps.setTime(7, flight.getHourArrival() != null ? Time.valueOf(flight.getHourArrival()) : null);

            if (airlineId != null) {
                ps.setInt(8, airlineId);
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    flight.setFlightId(keys.getInt(1));
                }
            }
            return flight;
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_UNIQUE_VIOLATION_1 || e.getErrorCode() == SQL_UNIQUE_VIOLATION_2) {
                throw new DuplicateException("Ya existe un vuelo con el código '" + flight.getFlightName() + "'.");
            }
            throw new AirlinesException("Error al guardar el vuelo en dbo.tblFlights: " + e.getMessage(), e);
        }
    }

    public List<Flight> findAll() {
        return query(BASE_SELECT + " ORDER BY f.FlightId DESC");
    }

    public Optional<Flight> findById(int id) {
        return query(BASE_SELECT + " WHERE f.FlightId = ?", id).stream().findFirst();
    }

    public Optional<Flight> findByFlightName(String flightName) {
        String clean = Validator.normalize(flightName);
        if (clean == null) {
            return Optional.empty();
        }
        return query(BASE_SELECT + " WHERE LOWER(f.FlightName) = LOWER(?)", clean).stream().findFirst();
    }

    public List<Flight> findByAirlineAndMonth(int airlineId, int year, int month) {
        String sql = BASE_SELECT
                + " WHERE f.AirlineId = ? AND YEAR(f.DateDeparture) = ? AND MONTH(f.DateDeparture) = ?"
                + " ORDER BY f.DateDeparture, f.HourDeparture";
        return query(sql, airlineId, year, month);
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM dbo.tblFlights WHERE FlightId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new AirlinesException("Error al eliminar el vuelo con ID " + id + ": " + e.getMessage(), e);
        }
    }

    private List<Flight> query(String sql, Object... params) {
        List<Flight> flights = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    flights.add(mapRowToFlight(rs));
                }
            }
            return flights;
        } catch (SQLException e) {
            throw new AirlinesException("Error al consultar los vuelos en dbo.tblFlights: " + e.getMessage(), e);
        }
    }

    private Flight mapRowToFlight(ResultSet rs) throws SQLException {
        Flight flight = new Flight();
        flight.setFlightId(rs.getInt("FlightId"));
        flight.setFlightName(rs.getString("FlightName"));

        flight.setDeparture(resolveAirport(rs.getString("Departure")));
        flight.setArrival(resolveAirport(rs.getString("Arrival")));

        Date dateDeparture = rs.getDate("DateDeparture");
        Time hourDeparture = rs.getTime("HourDeparture");
        Date dateArrival = rs.getDate("DateArrival");
        Time hourArrival = rs.getTime("HourArrival");

        flight.setDateDeparture(dateDeparture != null ? dateDeparture.toLocalDate() : null);
        flight.setHourDeparture(hourDeparture != null ? hourDeparture.toLocalTime() : null);
        flight.setDateArrival(dateArrival != null ? dateArrival.toLocalDate() : null);
        flight.setHourArrival(hourArrival != null ? hourArrival.toLocalTime() : null);

        int airlineId = rs.getInt("AirlineId");
        if (!rs.wasNull()) {
            String airlineName = rs.getString("AirlineName");
            Airline airline = airlineRepository.findById(airlineId)
                    .orElseGet(() -> airlineName != null ? new Airline(airlineId, airlineName) : null);
            if (airline != null) {
                flight.setAirline(airline);
            } else {
                flight.setAirlineId(airlineId);
                flight.setAirlineName(airlineName);
            }
        }

        return flight;
    }

    private Airport resolveAirport(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }
        UUID uuid = UUID.fromString(rawUuid.trim());
        return airportRepository.findById(uuid).orElse(null);
    }

    private static void setUuid(PreparedStatement ps, int index, UUID uuid) throws SQLException {
        if (uuid != null) {
            ps.setString(index, uuid.toString());
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }
}
